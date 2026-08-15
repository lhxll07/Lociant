//! Model catalog: bundled by default, overridable via `LOCIANT_CATALOG`
//! (a JSON file with the same shape). Install-time file discovery mirrors the
//! Android market: when an entry has no explicit `files`, the ModelScope repo
//! API is queried and supported model files are selected.

use serde::Deserialize;
use serde_json::{json, Value};

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogFile {
    pub path: String,
    #[serde(default)]
    pub size: u64,
    #[serde(default)]
    pub sha256: String,
    #[serde(default)]
    pub url: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogEntry {
    pub id: String,
    pub name: String,
    pub repo: String,
    pub description: String,
    pub vendor: String,
    pub runtime: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub size_gb: f64,
    pub file_size: u64,
    pub tags: Vec<String>,
    #[serde(default)]
    pub files: Option<Vec<CatalogFile>>,
}

pub fn load() -> Vec<CatalogEntry> {
    let raw = std::env::var("LOCIANT_CATALOG")
        .ok()
        .and_then(|path| std::fs::read_to_string(&path).ok())
        .unwrap_or_else(|| include_str!("catalog.json").to_owned());
    serde_json::from_str(&raw).unwrap_or_default()
}

pub fn find<'a>(entries: &'a [CatalogEntry], model_id: &str) -> Option<&'a CatalogEntry> {
    entries.iter().find(|entry| {
        entry.id.eq_ignore_ascii_case(model_id)
            || entry.name.eq_ignore_ascii_case(model_id)
            || entry.repo.eq_ignore_ascii_case(model_id)
    })
}

pub fn matches_query(entry: &CatalogEntry, query: &str) -> bool {
    let q = query.trim().to_lowercase();
    if q.is_empty() {
        return true;
    }
    [
        &entry.id,
        &entry.name,
        &entry.repo,
        &entry.vendor,
        &entry.description,
    ]
    .into_iter()
    .chain(entry.tags.iter())
    .any(|field| field.to_lowercase().contains(&q))
}

pub fn market_json(entry: &CatalogEntry, installed: bool) -> Value {
    json!({
        "id": entry.id,
        "repo": entry.repo,
        "name": entry.name,
        "description": entry.description,
        "vendor": entry.vendor,
        "sizeGb": entry.size_gb,
        "fileSize": entry.file_size,
        "tags": entry.tags,
        "installed": installed,
    })
}

pub fn installed_json(entry: &CatalogEntry) -> Value {
    json!({
        "id": entry.id,
        "name": entry.name,
        "runtime": entry.runtime,
        "type": entry.kind,
        "ready": true,
        "installed": true,
        "missingFiles": [],
    })
}

pub fn model_id_valid(id: &str) -> bool {
    !id.is_empty()
        && id.len() <= 96
        && id
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || matches!(c, '.' | '_' | '-'))
}

/// Lists supported downloadable model files from a ModelScope repo.
pub async fn repo_files(client: &reqwest::Client, repo: &str) -> Result<Vec<CatalogFile>, String> {
    let url = format!(
        "https://modelscope.cn/api/v1/models/{repo}/repo/files?Revision=master&Recursive=true"
    );
    let response = client
        .get(&url)
        .header(reqwest::header::USER_AGENT, "lociant-server")
        .send()
        .await
        .map_err(|error| format!("ModelScope repo files request failed: {error}"))?;
    if !response.status().is_success() {
        return Err(format!(
            "ModelScope repo files returned {}",
            response.status()
        ));
    }
    let body: Value = response
        .json()
        .await
        .map_err(|error| format!("ModelScope repo files parse failed: {error}"))?;
    let files = body
        .pointer("/Data/Files")
        .and_then(Value::as_array)
        .cloned()
        .unwrap_or_default();
    let mut selected = Vec::new();
    for file in files {
        if file.get("Type").and_then(Value::as_str) != Some("blob") {
            continue;
        }
        let path = file
            .get("Path")
            .and_then(Value::as_str)
            .unwrap_or("")
            .replace('\\', "/")
            .trim_start_matches('/')
            .to_owned();
        if !safe_relative_path(&path) || !is_model_file(&path) {
            continue;
        }
        selected.push(CatalogFile {
            path,
            size: file.get("Size").and_then(Value::as_u64).unwrap_or(0),
            sha256: file
                .get("Sha256")
                .and_then(Value::as_str)
                .unwrap_or("")
                .to_owned(),
            url: None,
        });
    }
    Ok(selected)
}

pub fn resolve_url(repo: &str, path: &str) -> String {
    let encoded = path
        .split('/')
        .map(urlencode_path_segment)
        .collect::<Vec<_>>()
        .join("/");
    format!("https://modelscope.cn/models/{repo}/resolve/master/{encoded}")
}

fn is_model_file(path: &str) -> bool {
    let lower = path.to_lowercase();
    const NAMES: [&str; 5] = [
        "config.json",
        "configuration.json",
        "llm_config.json",
        "tokenizer.txt",
        "tokenizer.json",
    ];
    let name = path.rsplit('/').next().unwrap_or(path);
    NAMES.contains(&name) || lower.ends_with(".gguf")
}

pub(crate) fn safe_relative_path(path: &str) -> bool {
    !path.is_empty()
        && !path.starts_with('/')
        && !path.contains(':')
        && path
            .split('/')
            .all(|segment| !segment.is_empty() && segment != "..")
}

fn urlencode_path_segment(segment: &str) -> String {
    segment
        .bytes()
        .map(|b| match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                (b as char).to_string()
            }
            _ => format!("%{b:02X}"),
        })
        .collect()
}

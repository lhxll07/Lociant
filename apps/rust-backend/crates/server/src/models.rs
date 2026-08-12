//! Model management: local model list, catalog, install jobs and delete.
//! Install mirrors the Android market (ModelScope repos, MNN file set) with
//! progress surfaced through the same job resource the Flutter UI polls.

use std::collections::HashSet;

use axum::extract::{Path, Query, State};
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use futures_util::StreamExt;
use serde_json::{json, Value};
use sha2::{Digest, Sha256};
use tokio::io::AsyncWriteExt;

use crate::catalog::{self, CatalogEntry};
use crate::error::{Problem, RequireAuth};
use crate::state::AppState;

#[derive(Debug, Clone)]
pub struct InstallJob {
    pub model_id: String,
    pub state: String,
    pub active: bool,
    pub progress: Option<f64>,
    pub message: String,
}

pub async fn list_models(
    State(state): State<AppState>,
    _: RequireAuth,
) -> Result<Json<Value>, Problem> {
    // collect_models may reach out to peers over HTTP (blocking client),
    // so run it off the async workers.
    let state_for_block = state.clone();
    let models = tokio::task::spawn_blocking(move || collect_models(&state_for_block))
        .await
        .map_err(|error| Problem::internal(error.to_string()))?;
    Ok(Json(json!({ "models": models })))
}

/// The models this node can serve: installed MNN packages, local MNN models
/// from the device layer, the built-in RKLLM model, the configured cloud
/// model, and models forwarded from discovered peers (`peer:<id>:<model>`).
pub fn collect_models(state: &AppState) -> Vec<Value> {
    let mut models = collect_local_models(state);
    if let Some(peers) = &state.peers {
        for model in peers.peer_models() {
            let id = model
                .get("id")
                .and_then(Value::as_str)
                .unwrap_or("")
                .to_owned();
            if !models
                .iter()
                .any(|m| m.get("id").and_then(Value::as_str) == Some(id.as_str()))
            {
                models.push(model);
            }
        }
    }
    models
}

/// Models this node can serve directly (no peer forwarding), used by the
/// peer plane so sibling calls can never recurse back into discovery.
pub fn collect_local_models(state: &AppState) -> Vec<Value> {
    let mut models = Vec::new();
    for installed in state.store.list_models().unwrap_or_default() {
        let json = match catalog::find(&state.catalog, &installed.id) {
            Some(entry) => catalog::installed_json(entry),
            None => json!({
                "id": installed.id,
                "name": installed.name,
                "runtime": installed.runtime,
                "type": installed.kind,
                "ready": true,
                "installed": true,
                "missingFiles": [],
                "cloud": false,
            }),
        };
        models.push(json);
    }
    if let Some(device) = &state.device {
        for model in device.models() {
            let id = model
                .get("id")
                .and_then(Value::as_str)
                .unwrap_or("")
                .to_owned();
            if id.is_empty()
                || models
                    .iter()
                    .any(|m| m.get("id").and_then(Value::as_str) == Some(id.as_str()))
            {
                continue;
            }
            let name = model
                .get("name")
                .and_then(Value::as_str)
                .unwrap_or(&id)
                .to_owned();
            models.push(json!({
                "id": id,
                "name": name,
                "runtime": "mnn",
                "type": "chat",
                "ready": true,
                "installed": true,
                "missingFiles": [],
                "cloud": false,
            }));
        }
    }
    if let Some(cloud) = cloud_model(state) {
        models.push(cloud);
    }
    if state.rkllm.is_some() {
        let id = state
            .settings_snapshot()
            .get("rkllmModelName")
            .and_then(Value::as_str)
            .unwrap_or("rkllm")
            .to_owned();
        if !models
            .iter()
            .any(|m| m.get("id").and_then(Value::as_str) == Some(id.as_str()))
        {
            models.push(json!({
                "id": id,
                "name": id,
                "runtime": "rkllm",
                "type": "chat",
                "ready": true,
                "installed": true,
                "missingFiles": [],
                "cloud": false,
            }));
        }
    }
    models
}

pub async fn catalog_models(
    State(state): State<AppState>,
    _: RequireAuth,
    Query(params): Query<std::collections::HashMap<String, String>>,
) -> Json<Value> {
    let query = params.get("q").cloned().unwrap_or_default();
    let installed: HashSet<String> = state
        .store
        .list_models()
        .map(|models| models.into_iter().map(|m| m.id).collect())
        .unwrap_or_default();
    let models = state
        .catalog
        .iter()
        .filter(|entry| catalog::matches_query(entry, &query))
        .map(|entry| catalog::market_json(entry, installed.contains(&entry.id)))
        .collect::<Vec<_>>();
    Json(json!({ "models": models }))
}

pub async fn start_install(
    State(state): State<AppState>,
    _: RequireAuth,
    Json(body): Json<Value>,
) -> Result<Response, Problem> {
    let model_id = body
        .get("modelId")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    let entry = catalog::find(&state.catalog, &model_id)
        .cloned()
        .ok_or_else(|| {
            Problem::not_found(
                format!("Unknown model: {model_id}"),
                "/api/v1/model-installations",
            )
        })?;
    if !catalog::model_id_valid(&entry.id) {
        return Err(Problem::bad_request(
            format!("Invalid model id: {}", entry.id),
            "/api/v1/model-installations",
        ));
    }
    {
        let mut installs = state.installs.lock().expect("installs lock");
        if installs
            .get(&entry.id)
            .map(|job| job.active)
            .unwrap_or(false)
        {
            return Ok((
                StatusCode::ACCEPTED,
                Json(json!({
                    "jobId": entry.id,
                    "modelId": entry.id,
                    "message": "already installing",
                })),
            )
                .into_response());
        }
        installs.insert(
            entry.id.clone(),
            InstallJob {
                model_id: entry.id.clone(),
                state: "installing".into(),
                active: true,
                progress: Some(0.0),
                message: "Starting".into(),
            },
        );
    }
    let task_state = state.clone();
    let task_entry = entry.clone();
    tokio::spawn(async move { run_install(task_state, task_entry).await });
    Ok((
        StatusCode::ACCEPTED,
        Json(json!({
            "jobId": entry.id,
            "modelId": entry.id,
            "message": "installing",
        })),
    )
        .into_response())
}

pub async fn install_progress(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(job_id): Path<String>,
) -> Result<Json<Value>, Problem> {
    let installs = state.installs.lock().expect("installs lock");
    let job = installs.get(&job_id).ok_or_else(|| {
        Problem::not_found(
            format!("Installation not found: {job_id}"),
            &format!("/api/v1/model-installations/{job_id}"),
        )
    })?;
    Ok(Json(json!({
        "jobId": job.model_id,
        "modelId": job.model_id,
        "state": job.state,
        "active": job.active,
        "progress": job.progress,
        "message": job.message,
    })))
}

pub async fn delete_model(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(model_id): Path<String>,
) -> Result<StatusCode, Problem> {
    if !catalog::model_id_valid(&model_id) {
        return Err(Problem::not_found(
            format!("Model not found: {model_id}"),
            &format!("/api/v1/models/{model_id}"),
        ));
    }
    let exists = state
        .store
        .get_model(&model_id)
        .map_err(|e| Problem::internal(e.to_string()))?
        .is_some();
    if !exists {
        return Err(Problem::not_found(
            format!("Model not found: {model_id}"),
            &format!("/api/v1/models/{model_id}"),
        ));
    }
    state
        .store
        .delete_model(&model_id)
        .map_err(|e| Problem::internal(e.to_string()))?;
    let dir = state.models_dir.join(&model_id);
    if dir.exists() {
        std::fs::remove_dir_all(&dir).map_err(|e| Problem::internal(e.to_string()))?;
    }
    if let Ok(mut installs) = state.installs.lock() {
        installs.remove(&model_id);
    }
    Ok(StatusCode::NO_CONTENT)
}

pub async fn openai_models(
    State(state): State<AppState>,
    _: RequireAuth,
) -> Result<Json<Value>, Problem> {
    let mut data = Vec::new();
    for installed in state
        .store
        .list_models()
        .map_err(|e| Problem::internal(e.to_string()))?
    {
        data.push(json!({
            "id": installed.id,
            "object": "model",
            "created": 0,
            "owned_by": installed.runtime,
        }));
    }
    if let Some(device) = &state.device {
        for model in device.models() {
            let id = model
                .get("id")
                .and_then(Value::as_str)
                .unwrap_or("")
                .to_owned();
            if !id.is_empty()
                && data
                    .iter()
                    .all(|m| m.get("id").and_then(Value::as_str) != Some(id.as_str()))
            {
                data.push(json!({
                    "id": id,
                    "object": "model",
                    "created": 0,
                    "owned_by": "mnn",
                }));
            }
        }
    }
    if let Some(cloud) = cloud_model(&state) {
        data.push(json!({
            "id": cloud["id"],
            "object": "model",
            "created": 0,
            "owned_by": "lociant",
        }));
    }
    Ok(Json(json!({ "object": "list", "data": data })))
}

pub(crate) fn cloud_model(state: &AppState) -> Option<Value> {
    let settings = state.settings_snapshot();
    let enabled = settings
        .get("cloudEnabled")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    let id = settings
        .get("cloudModel")
        .and_then(Value::as_str)
        .unwrap_or("");
    if !enabled || id.is_empty() {
        return None;
    }
    Some(json!({
        "id": id,
        "name": id,
        "runtime": "cloud",
        "type": "chat",
        "ready": true,
        "installed": true,
        "missingFiles": [],
        "cloud": true,
    }))
}

async fn run_install(state: AppState, entry: CatalogEntry) {
    update_job(&state, &entry.id, Some(0.0), "Starting", true, "installing");
    let files = match &entry.files {
        Some(files) if !files.is_empty() => files.clone(),
        _ => match catalog::repo_files(&state.http, &entry.repo).await {
            Ok(files) if !files.is_empty() => files,
            Ok(_) => {
                fail_install(
                    &state,
                    &entry.id,
                    "model repo has no downloadable MNN files",
                );
                return;
            }
            Err(error) => {
                fail_install(&state, &entry.id, &error);
                return;
            }
        },
    };
    let total: u64 = files.iter().map(|file| file.size).sum();
    let target = state.models_dir.join(&entry.id);
    if let Err(error) = std::fs::create_dir_all(&target) {
        fail_install(
            &state,
            &entry.id,
            &format!("create model dir failed: {error}"),
        );
        return;
    }
    let mut downloaded: u64 = 0;
    for file in &files {
        if !catalog::safe_relative_path(&file.path) {
            fail_install(
                &state,
                &entry.id,
                &format!("invalid model file path: {}", file.path),
            );
            let _ = std::fs::remove_dir_all(&target);
            return;
        }
        let url = file
            .url
            .clone()
            .unwrap_or_else(|| catalog::resolve_url(&entry.repo, &file.path));
        let out = target.join(&file.path);
        if let Some(parent) = out.parent() {
            if let Err(error) = std::fs::create_dir_all(parent) {
                fail_install(&state, &entry.id, &format!("create dir failed: {error}"));
                let _ = std::fs::remove_dir_all(&target);
                return;
            }
        }
        let message = format!(
            "Downloading {}",
            file.path.rsplit('/').next().unwrap_or(&file.path)
        );
        match download_to_file(&state.http, &url, &out, &file.sha256, |delta| {
            downloaded += delta;
            let progress = if total > 0 {
                Some((downloaded as f64 / total as f64).clamp(0.0, 0.99))
            } else {
                None
            };
            update_job(&state, &entry.id, progress, &message, true, "installing");
        })
        .await
        {
            Ok(()) => {}
            Err(error) => {
                fail_install(&state, &entry.id, &error);
                let _ = std::fs::remove_dir_all(&target);
                return;
            }
        }
    }
    match state
        .store
        .insert_model(&entry.id, &entry.name, &entry.runtime, &entry.kind)
    {
        Ok(()) => update_job(
            &state,
            &entry.id,
            Some(1.0),
            "Model installed",
            false,
            "done",
        ),
        Err(error) => {
            fail_install(
                &state,
                &entry.id,
                &format!("register model failed: {error}"),
            );
            let _ = std::fs::remove_dir_all(&target);
        }
    }
}

fn fail_install(state: &AppState, model_id: &str, message: &str) {
    tracing::warn!("model install failed {model_id}: {message}");
    update_job(state, model_id, None, message, false, "error");
}

fn update_job(
    state: &AppState,
    model_id: &str,
    progress: Option<f64>,
    message: &str,
    active: bool,
    job_state: &str,
) {
    if let Ok(mut installs) = state.installs.lock() {
        if let Some(job) = installs.get_mut(model_id) {
            job.progress = progress;
            job.message = message.to_owned();
            job.active = active;
            job.state = job_state.to_owned();
        }
    }
}

async fn download_to_file(
    client: &reqwest::Client,
    url: &str,
    out: &std::path::Path,
    expected_sha256: &str,
    mut on_bytes: impl FnMut(u64),
) -> Result<(), String> {
    let response = client
        .get(url)
        .header(reqwest::header::USER_AGENT, "lociant-server")
        .send()
        .await
        .map_err(|error| format!("download failed: {error}"))?;
    if !response.status().is_success() {
        return Err(format!("download returned {}", response.status()));
    }
    let mut stream = response.bytes_stream();
    let mut file = tokio::fs::File::create(out)
        .await
        .map_err(|error| format!("create {} failed: {error}", out.display()))?;
    let mut hasher = Sha256::new();
    while let Some(chunk) = stream.next().await {
        let chunk = chunk.map_err(|error| format!("download stream failed: {error}"))?;
        hasher.update(&chunk);
        file.write_all(&chunk)
            .await
            .map_err(|error| format!("write {} failed: {error}", out.display()))?;
        on_bytes(chunk.len() as u64);
    }
    if !expected_sha256.is_empty() {
        let actual = hex::encode(hasher.finalize());
        if !actual.eq_ignore_ascii_case(expected_sha256) {
            return Err(format!(
                "checksum mismatch for {}",
                out.file_name()
                    .map(|n| n.to_string_lossy().into_owned())
                    .unwrap_or_default()
            ));
        }
    }
    Ok(())
}

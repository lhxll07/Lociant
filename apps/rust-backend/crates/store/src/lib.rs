//! SQLite persistence for sessions, messages and settings.
//!
//! SQLite is compiled in (rusqlite `bundled`), so the backend has no system
//! database dependency and behaves identically on Android, desktop and
//! Armbian. All access goes through one mutex-protected connection; request
//! volume is small and write throughput is not a bottleneck.

use std::path::Path;
use std::sync::{Arc, Mutex};

use anyhow::{Context, Result};
use lociant_core::{InstalledModel, SessionSummary, StoredMessage};
use rusqlite::{params, Connection, OptionalExtension};
use serde_json::{json, Value};

const SCHEMA: &str = "
CREATE TABLE IF NOT EXISTS sessions (
    id            TEXT PRIMARY KEY,
    title         TEXT NOT NULL,
    model_id      TEXT NOT NULL DEFAULT '',
    created_at    INTEGER NOT NULL,
    updated_at    INTEGER NOT NULL,
    message_count INTEGER NOT NULL DEFAULT 0,
    last_role     TEXT NOT NULL DEFAULT '',
    last_text     TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS messages (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id   TEXT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    role         TEXT NOT NULL,
    text         TEXT NOT NULL DEFAULT '',
    content_json TEXT NOT NULL DEFAULT '{}',
    created_at   INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_messages_session ON messages(session_id, id);

CREATE TABLE IF NOT EXISTS kv (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS models (
    id           TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    runtime      TEXT NOT NULL DEFAULT '',
    type         TEXT NOT NULL DEFAULT 'chat',
    installed_at INTEGER NOT NULL
);
";

/// Last-text preview cap kept in sync with the Android runtime defaults.
const LAST_TEXT_LIMIT: usize = 200;

pub struct Store {
    conn: Mutex<Connection>,
    next_session_id: Mutex<u64>,
}

impl Store {
    pub fn open(data_dir: &Path) -> Result<Arc<Store>> {
        std::fs::create_dir_all(data_dir)
            .with_context(|| format!("create data dir {}", data_dir.display()))?;
        let conn = Connection::open(data_dir.join("lociant.db"))
            .with_context(|| format!("open {}", data_dir.join("lociant.db").display()))?;
        conn.pragma_update(None, "journal_mode", "WAL")?;
        conn.pragma_update(None, "foreign_keys", "ON")?;
        conn.execute_batch(SCHEMA)?;
        Ok(Arc::new(Store {
            conn: Mutex::new(conn),
            next_session_id: Mutex::new(0),
        }))
    }

    // ---- sessions ----

    pub fn list_sessions(&self) -> Result<Vec<SessionSummary>> {
        let conn = self.conn.lock().expect("db lock");
        let mut stmt = conn.prepare(
            "SELECT id, title, model_id, updated_at, message_count, last_role, last_text
             FROM sessions ORDER BY updated_at DESC LIMIT 200",
        )?;
        let rows = stmt.query_map([], |row| {
            Ok(SessionSummary {
                id: row.get(0)?,
                title: row.get(1)?,
                model_id: row.get(2)?,
                updated_at: row.get(3)?,
                message_count: row.get(4)?,
                last_role: row.get(5)?,
                last_text: row.get(6)?,
            })
        })?;
        rows.collect::<rusqlite::Result<Vec<_>>>()
            .map_err(Into::into)
    }

    pub fn get_session(&self, id: &str) -> Result<Option<SessionSummary>> {
        let conn = self.conn.lock().expect("db lock");
        let row = conn
            .query_row(
                "SELECT id, title, model_id, updated_at, message_count, last_role, last_text
                 FROM sessions WHERE id = ?1",
                params![id],
                |row| {
                    Ok(SessionSummary {
                        id: row.get(0)?,
                        title: row.get(1)?,
                        model_id: row.get(2)?,
                        updated_at: row.get(3)?,
                        message_count: row.get(4)?,
                        last_role: row.get(5)?,
                        last_text: row.get(6)?,
                    })
                },
            )
            .optional()?;
        Ok(row)
    }

    pub fn create_session(&self, model_id: &str) -> Result<String> {
        let id = {
            let mut counter = self.next_session_id.lock().expect("id lock");
            *counter += 1;
            format!(
                "chat_{}_{:x}",
                std::time::SystemTime::now()
                    .duration_since(std::time::UNIX_EPOCH)
                    .map(|d| d.as_millis())
                    .unwrap_or(0),
                *counter
            )
        };
        let now = now_millis();
        let conn = self.conn.lock().expect("db lock");
        conn.execute(
            "INSERT INTO sessions (id, title, model_id, created_at, updated_at)
             VALUES (?1, ?2, ?3, ?4, ?4)",
            params![id, "New chat", model_id, now],
        )?;
        Ok(id)
    }

    pub fn delete_session(&self, id: &str) -> Result<bool> {
        let conn = self.conn.lock().expect("db lock");
        let changed = conn.execute("DELETE FROM sessions WHERE id = ?1", params![id])?;
        Ok(changed > 0)
    }

    pub fn append_message(
        &self,
        session_id: &str,
        role: &str,
        text: &str,
        content_json: Value,
    ) -> Result<i64> {
        let now = now_millis();
        let conn = self.conn.lock().expect("db lock");
        conn.execute(
            "INSERT INTO messages (session_id, role, text, content_json, created_at)
             VALUES (?1, ?2, ?3, ?4, ?5)",
            params![session_id, role, text, content_json.to_string(), now],
        )?;
        let id = conn.last_insert_rowid();
        let preview: String = text.chars().take(LAST_TEXT_LIMIT).collect();
        conn.execute(
            "UPDATE sessions
             SET message_count = message_count + 1, last_role = ?1, last_text = ?2, updated_at = ?3
             WHERE id = ?4",
            params![role, preview, now, session_id],
        )?;
        Ok(id)
    }

    pub fn list_messages(&self, session_id: &str) -> Result<Vec<StoredMessage>> {
        let conn = self.conn.lock().expect("db lock");
        let mut stmt = conn.prepare(
            "SELECT id, role, text, content_json, created_at
             FROM messages WHERE session_id = ?1 ORDER BY id",
        )?;
        let rows = stmt.query_map(params![session_id], |row| {
            let content_json: String = row.get(3)?;
            Ok(StoredMessage {
                id: row.get(0)?,
                role: row.get(1)?,
                text: row.get(2)?,
                content_json: serde_json::from_str(&content_json).unwrap_or_else(|_| json!({})),
                created_at: row.get(4)?,
            })
        })?;
        rows.collect::<rusqlite::Result<Vec<_>>>()
            .map_err(Into::into)
    }

    // ---- settings ----

    pub fn set_json(&self, key: &str, value: &Value) -> Result<()> {
        let conn = self.conn.lock().expect("db lock");
        conn.execute(
            "INSERT INTO kv (key, value) VALUES (?1, ?2)
             ON CONFLICT(key) DO UPDATE SET value = excluded.value",
            params![key, value.to_string()],
        )?;
        Ok(())
    }

    pub fn get_json(&self, key: &str) -> Result<Option<Value>> {
        let conn = self.conn.lock().expect("db lock");
        let raw = conn
            .query_row("SELECT value FROM kv WHERE key = ?1", params![key], |row| {
                row.get::<_, String>(0)
            })
            .optional()?;
        raw.map(|s| serde_json::from_str(&s).context("parse stored JSON"))
            .transpose()
    }

    // ---- installed models ----

    pub fn list_models(&self) -> Result<Vec<InstalledModel>> {
        let conn = self.conn.lock().expect("db lock");
        let mut stmt = conn.prepare(
            "SELECT id, name, runtime, type, installed_at FROM models ORDER BY installed_at",
        )?;
        let rows = stmt.query_map([], |row| {
            Ok(InstalledModel {
                id: row.get(0)?,
                name: row.get(1)?,
                runtime: row.get(2)?,
                kind: row.get(3)?,
                installed_at: row.get(4)?,
            })
        })?;
        rows.collect::<rusqlite::Result<Vec<_>>>()
            .map_err(Into::into)
    }

    pub fn get_model(&self, id: &str) -> Result<Option<InstalledModel>> {
        let conn = self.conn.lock().expect("db lock");
        let row = conn
            .query_row(
                "SELECT id, name, runtime, type, installed_at FROM models WHERE id = ?1",
                params![id],
                |row| {
                    Ok(InstalledModel {
                        id: row.get(0)?,
                        name: row.get(1)?,
                        runtime: row.get(2)?,
                        kind: row.get(3)?,
                        installed_at: row.get(4)?,
                    })
                },
            )
            .optional()?;
        Ok(row)
    }

    pub fn insert_model(&self, id: &str, name: &str, runtime: &str, kind: &str) -> Result<()> {
        let conn = self.conn.lock().expect("db lock");
        conn.execute(
            "INSERT INTO models (id, name, runtime, type, installed_at)
             VALUES (?1, ?2, ?3, ?4, ?5)
             ON CONFLICT(id) DO UPDATE SET name = excluded.name, runtime = excluded.runtime",
            params![id, name, runtime, kind, now_millis()],
        )?;
        Ok(())
    }

    pub fn delete_model(&self, id: &str) -> Result<bool> {
        let conn = self.conn.lock().expect("db lock");
        let changed = conn.execute("DELETE FROM models WHERE id = ?1", params![id])?;
        Ok(changed > 0)
    }
}

fn now_millis() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn tmp_store(label: &str) -> Arc<Store> {
        let dir = std::env::temp_dir().join(format!(
            "lociant-store-test-{label}-{}-{}",
            std::process::id(),
            now_millis()
        ));
        Store::open(&dir).unwrap()
    }

    #[test]
    fn session_crud_roundtrip() {
        let store = tmp_store("session");
        let id = store.create_session("qwen3").unwrap();
        assert!(store.get_session(&id).unwrap().is_some());

        store
            .append_message(&id, "user", "hello", json!({}))
            .unwrap();
        store
            .append_message(&id, "assistant", "hi there", json!({"reasoning": "think"}))
            .unwrap();

        let summary = store.get_session(&id).unwrap().unwrap();
        assert_eq!(summary.message_count, 2);
        assert_eq!(summary.last_role, "assistant");
        assert_eq!(summary.last_text, "hi there");

        let messages = store.list_messages(&id).unwrap();
        assert_eq!(messages.len(), 2);
        assert_eq!(messages[1].role, "assistant");
        assert_eq!(messages[1].content_json["reasoning"], "think");

        assert!(store.delete_session(&id).unwrap());
        assert!(store.get_session(&id).unwrap().is_none());
        assert!(store.list_messages(&id).unwrap().is_empty());
    }

    #[test]
    fn settings_kv_roundtrip() {
        let store = tmp_store("settings");
        assert!(store.get_json("settings").unwrap().is_none());
        store
            .set_json("settings", &json!({"modelId": "local", "apiToken": "t"}))
            .unwrap();
        let loaded = store.get_json("settings").unwrap().unwrap();
        assert_eq!(loaded["modelId"], "local");
        store
            .set_json("settings", &json!({"apiToken": "u"}))
            .unwrap();
        assert_eq!(
            store.get_json("settings").unwrap().unwrap()["apiToken"],
            "u"
        );
    }

    #[test]
    fn models_crud_roundtrip() {
        let store = tmp_store("models");
        assert!(store.list_models().unwrap().is_empty());
        store
            .insert_model("qwen3.5-0.8b-gguf", "Qwen3.5-0.8B", "llama", "chat")
            .unwrap();
        let model = store.get_model("qwen3.5-0.8b-gguf").unwrap().unwrap();
        assert_eq!(model.name, "Qwen3.5-0.8B");
        assert_eq!(model.kind, "chat");
        assert_eq!(store.list_models().unwrap().len(), 1);
        assert!(store.delete_model("qwen3.5-0.8b-gguf").unwrap());
        assert!(store.get_model("qwen3.5-0.8b-gguf").unwrap().is_none());
    }
}

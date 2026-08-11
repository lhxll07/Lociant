//! Control-plane error handling: RFC 7807 problem+json responses and the
//! bearer-token guard shared by every control/data route.

use axum::extract::FromRequestParts;
use axum::http::header::{AUTHORIZATION, CONTENT_TYPE};
use axum::http::{HeaderValue, StatusCode};
use axum::response::{IntoResponse, Response};
use axum::Json;
use serde::Serialize;

use crate::state::AppState;

#[derive(Debug, Clone, Serialize)]
pub struct Problem {
    #[serde(rename = "type")]
    pub kind: String,
    pub title: String,
    pub status: u16,
    pub detail: String,
    pub code: String,
    pub instance: String,
}

impl Problem {
    pub fn new(
        status: StatusCode,
        code: &str,
        title: &str,
        detail: impl Into<String>,
        instance: &str,
    ) -> Self {
        Problem {
            kind: format!("https://lociant.io/problems/{code}"),
            title: title.to_owned(),
            status: status.as_u16(),
            detail: detail.into(),
            code: code.to_owned(),
            instance: instance.to_owned(),
        }
    }

    pub fn bad_request(detail: impl Into<String>, instance: &str) -> Self {
        Self::new(
            StatusCode::BAD_REQUEST,
            "bad-request",
            "Bad request",
            detail,
            instance,
        )
    }

    pub fn unauthorized(instance: &str) -> Self {
        Self::new(
            StatusCode::UNAUTHORIZED,
            "unauthorized",
            "Unauthorized",
            "invalid or missing API token",
            instance,
        )
    }

    pub fn not_found(detail: impl Into<String>, instance: &str) -> Self {
        Self::new(
            StatusCode::NOT_FOUND,
            "not-found",
            "Resource not found",
            detail,
            instance,
        )
    }

    pub fn forbidden(detail: impl Into<String>, instance: &str) -> Self {
        Self::new(
            StatusCode::FORBIDDEN,
            "forbidden",
            "Forbidden",
            detail,
            instance,
        )
    }

    pub fn internal(detail: impl Into<String>) -> Self {
        Self::new(
            StatusCode::INTERNAL_SERVER_ERROR,
            "internal",
            "Internal server error",
            detail,
            "",
        )
    }
}

impl IntoResponse for Problem {
    fn into_response(self) -> Response {
        let status = StatusCode::from_u16(self.status).unwrap_or(StatusCode::INTERNAL_SERVER_ERROR);
        let content_type = HeaderValue::from_static("application/problem+json; charset=utf-8");
        (status, [(CONTENT_TYPE, content_type)], Json(self)).into_response()
    }
}

impl IntoResponse for Box<Problem> {
    fn into_response(self) -> Response {
        (*self).into_response()
    }
}

/// Extractor that enforces the configured bearer token on control/data routes.
/// When no token is configured the routes are open, matching the Android
/// runtime's behavior.
pub struct RequireAuth;

impl FromRequestParts<AppState> for RequireAuth {
    type Rejection = Box<Problem>;

    async fn from_request_parts(
        parts: &mut axum::http::request::Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let token = state.auth_token();
        if token.is_empty() {
            return Ok(Self);
        }
        let supplied = parts
            .headers
            .get(AUTHORIZATION)
            .and_then(|v| v.to_str().ok())
            .and_then(|v| v.strip_prefix("Bearer "))
            .map(str::to_owned)
            .or_else(|| {
                parts
                    .headers
                    .get("x-lociant-token")
                    .and_then(|v| v.to_str().ok())
                    .map(str::to_owned)
            });
        if supplied.as_deref() == Some(token.as_str()) {
            Ok(Self)
        } else {
            Err(Box::new(Problem::unauthorized("")))
        }
    }
}

fn supplied_token(parts: &axum::http::request::Parts) -> Option<String> {
    parts
        .headers
        .get(AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.strip_prefix("Bearer "))
        .map(str::to_owned)
        .or_else(|| {
            parts
                .headers
                .get("x-lociant-token")
                .and_then(|v| v.to_str().ok())
                .map(str::to_owned)
        })
}

/// Peer-plane guard: accepts only the configured peer token, so nodes can
/// call each other's tools/models without gaining control-plane access.
pub struct RequirePeerAuth;

impl FromRequestParts<AppState> for RequirePeerAuth {
    type Rejection = Box<Problem>;

    async fn from_request_parts(
        parts: &mut axum::http::request::Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let peer_token = state
            .settings_snapshot()
            .get("peerToken")
            .and_then(serde_json::Value::as_str)
            .unwrap_or("")
            .to_owned();
        if peer_token.is_empty() {
            return Err(Box::new(Problem::forbidden(
                "peer networking is not enabled on this node",
                "/api/v1/peer",
            )));
        }
        if supplied_token(parts).as_deref() == Some(peer_token.as_str()) {
            Ok(Self)
        } else {
            Err(Box::new(Problem::unauthorized("/api/v1/peer")))
        }
    }
}

/// Chat-plane guard: accepts the API token or the peer token, so sibling
/// nodes can forward chat completions without control-plane access.
pub struct RequireChatAuth;

impl FromRequestParts<AppState> for RequireChatAuth {
    type Rejection = Box<Problem>;

    async fn from_request_parts(
        parts: &mut axum::http::request::Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let token = state.auth_token();
        let peer_token = state
            .settings_snapshot()
            .get("peerToken")
            .and_then(serde_json::Value::as_str)
            .unwrap_or("")
            .to_owned();
        if token.is_empty() && peer_token.is_empty() {
            return Ok(Self);
        }
        let supplied = supplied_token(parts);
        if supplied.as_deref() == Some(token.as_str())
            || (!peer_token.is_empty() && supplied.as_deref() == Some(peer_token.as_str()))
        {
            Ok(Self)
        } else {
            Err(Box::new(Problem::unauthorized("/v1/chat/completions")))
        }
    }
}

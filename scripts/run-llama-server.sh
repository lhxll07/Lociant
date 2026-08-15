#!/usr/bin/env bash
# Launch a local llama.cpp OpenAI-compatible server for Lociant.
#
# Usage:
#   bash scripts/run-llama-server.sh /path/to/model.gguf [port] [alias] [threads]
#
# Lociant can register it as a local llama runtime with the settings emitted
# at the end of this script.
set -euo pipefail

MODEL="${1:?usage: run-llama-server.sh <model.gguf> [port] [alias] [threads]}"
PORT="${2:-11502}"
ALIAS="${3:-$(basename "$MODEL" .gguf)}"
THREADS="${4:-8}"

if ! command -v llama-server >/dev/null 2>&1; then
  echo "llama-server not found. Install llama.cpp first." >&2
  exit 1
fi

if curl -sf "http://127.0.0.1:${PORT}/health" >/dev/null 2>&1; then
  echo "llama-server already listening on port ${PORT}" >&2
else
  echo "Starting llama-server: model=$MODEL port=$PORT alias=$ALIAS threads=$THREADS"
  llama-server \
    -m "$MODEL" \
    --port "$PORT" \
    --alias "$ALIAS" \
    --host 127.0.0.1 \
    --threads "$THREADS" \
    --ctx-size 4096 \
    --n-predict 512 >/tmp/lociant-llama-server.log 2>&1 &
  echo "llama-server pid=$!" >&2
fi

cat <<JSON
{
  "llamaEnabled": true,
  "llamaBaseUrl": "http://127.0.0.1:${PORT}",
  "llamaModelName": "$ALIAS"
}
JSON

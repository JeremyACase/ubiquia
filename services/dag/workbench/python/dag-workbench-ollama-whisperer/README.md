# ollama-whisperer

A minimal FastAPI service packaged with **uv** and linted/formatted with **ruff**. It exposes `/wrap` which accepts a JSON body `{ "userPrompt": "..." }` and returns `{ "payload": { "userPrompt": "..." } }`.

## Quickstart

```bash
cd services/ai/ollama-whisperer
uv sync --all-extras --dev
uv run uvicorn ollama_whisperer.main:app --host 0.0.0.0 --port 8080
```

## Test

```bash
curl -sS http://localhost:8080/health
curl -sS -X POST http://localhost:8080/wrap \
  -H 'Content-Type: application/json' \
  -d '{"userPrompt":"Classify a pet image"}'
```

## Gradle

```bash
./gradlew :services:ai:ollama-whisperer:build
```

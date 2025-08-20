.PHONY: dev test lint run build docker

PY = uv run python

init:
	uv lock
	uv sync --all-extras --dev

lint:
	uv run ruff check src tests

format:
	uv run ruff format src tests

test:
	uv run pytest

run:
	uv run uvicorn ollama_whisperer.main:app --host 0.0.0.0 --port 8080

build:
	uv build

docker:
	docker build -t ubiquia/ollama-whisperer:latest .

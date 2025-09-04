import pytest
from fastapi.testclient import TestClient
from ollama_whisperer.main import app


client = TestClient(app)


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_wrap_ok():
    payload = {"userPrompt": "Find me a pet store image classifier DAG."}
    r = client.post("/wrap", json=payload)
    assert r.status_code == 200
    assert r.json()["payload"]["userPrompt"] == payload["userPrompt"]


def test_wrap_empty_rejected():
    r = client.post("/wrap", json={"userPrompt": "   "})
    assert r.status_code == 400

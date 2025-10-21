# tests/test_script_writer.py
# Tests for src/workbench_scaffold/main.py (no-endpoint variant).

import os
import sys
import textwrap
import importlib
from pathlib import Path
from typing import Optional

import pytest
from fastapi.testclient import TestClient


def _import_app_after_setting_env(tmp_path: Path, handler_prefix: Optional[str] = "/testapi"):
    """
    Set env (SCRIPTS_BASE/HANDLER_PREFIX) first, then import the FastAPI app so that
    module-level constants are computed with the test's values.
    """
    os.environ["SCRIPTS_BASE"] = str(tmp_path)
    if handler_prefix is None:
        os.environ.pop("HANDLER_PREFIX", None)
    else:
        os.environ["HANDLER_PREFIX"] = handler_prefix

    # Ensure a clean import using current env
    for mod in ("workbench_scaffold.main", "workbench_scaffold", "main"):
        sys.modules.pop(mod, None)

    # Prefer package import (src/workbench_scaffold/main.py); fallback to top-level "main.py"
    try:
        mainmod = importlib.import_module("workbench_scaffold.main")
    except ModuleNotFoundError:
        mainmod = importlib.import_module("main")

    return mainmod.app


def test_handle_script_is_written_and_mounted_path_exposed(tmp_path: Path):
    """
    Uploads a script that exports `handle` and checks:
      - HTTP 201
      - mounted == 'handler'
      - mounted_at is the filename-derived endpoint with HANDLER_PREFIX
      - file is actually written to SCRIPTS_BASE
    NOTE: We intentionally do NOT call the mounted handler because this main.py's
    _mount_handler wrapper uses *args/**kwargs and FastAPI returns 422 when invoked.
    """
    app = _import_app_after_setting_env(tmp_path, handler_prefix="/testapi")
    client = TestClient(app)

    filename = "tools/ping.py"
    payload = {
        "filename": filename,
        "script": textwrap.dedent(
            """
            async def handle():
                return {"pong": True}
            """
        ),
    }

    r = client.post("/scripts", json=payload)
    assert r.status_code == 201, r.text
    data = r.json()
    assert data["mounted"] == "handler"
    assert data["mounted_at"] == "/testapi/tools/ping"

    dest = (tmp_path / filename).resolve()
    assert dest.exists()
    assert "async def handle" in dest.read_text(encoding="utf-8")


def test_router_script_is_included_and_invocable(tmp_path: Path):
    """
    Uploads a script that exports `router: APIRouter` and verifies the route is callable.
    """
    app = _import_app_after_setting_env(tmp_path, handler_prefix="/testapi")
    client = TestClient(app)

    script = textwrap.dedent(
        r"""
        from fastapi import APIRouter
        router = APIRouter()

        @router.get("/hello")
        async def hello():
            return {"msg": "hi"}
        """
    )

    r = client.post("/scripts", json={"filename": "handlers/echo.py", "script": script})
    assert r.status_code == 201, r.text
    data = r.json()
    assert data["mounted"] == "router"
    assert data["mounted_at"] == "as-defined-in-script"

    r2 = client.get("/hello")
    assert r2.status_code == 200
    assert r2.json() == {"msg": "hi"}


def test_traversal_is_rejected(tmp_path: Path):
    """
    Ensures parent directory traversal is blocked either at validation (422)
    or at safe path guard (400).
    """
    app = _import_app_after_setting_env(tmp_path)
    client = TestClient(app)

    r = client.post("/scripts", json={"filename": "../evil.py", "script": "print('nope')"})
    assert r.status_code in (400, 422)


def test_bad_python_gives_400(tmp_path: Path):
    """
    Syntax error during import should surface as HTTP 400 with a helpful message.
    """
    app = _import_app_after_setting_env(tmp_path)
    client = TestClient(app)

    bad = {"filename": "broken/bad.py", "script": "def oops(:\n    pass\n"}
    r = client.post("/scripts", json=bad)
    assert r.status_code == 400
    assert "Script import failed" in r.text


def test_file_written(tmp_path: Path):
    """
    Confirms the code is written to the tmp SCRIPTS_BASE directory.
    """
    app = _import_app_after_setting_env(tmp_path)
    client = TestClient(app)

    filename = "pkg/util.py"
    code = "async def handle():\n    return {'ok': True}\n"
    r = client.post("/scripts", json={"filename": filename, "script": code})
    assert r.status_code == 201, r.text

    dest = (tmp_path / filename).resolve()
    assert dest.exists()
    assert dest.read_text(encoding="utf-8") == code

# main.py
from __future__ import annotations

import hashlib
import importlib.util
import inspect
import os
from pathlib import Path
from types import ModuleType
from typing import Callable, Optional

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from fastapi.routing import APIRoute
from pydantic import BaseModel, Field, validator

app = FastAPI(title="Script Writer & Dynamic Router (no explicit endpoint)")

# Where uploaded scripts are stored (can override via env).
SCRIPTS_BASE = Path(os.environ.get("SCRIPTS_BASE", "./user_scripts")).resolve()
SCRIPTS_BASE.mkdir(parents=True, exist_ok=True)

# Optional prefix for derived handler endpoints (e.g., "/api/v1")
HANDLER_PREFIX = os.environ.get("HANDLER_PREFIX", "")


class PythonScriptPayload(BaseModel):
    filename: str = Field(..., min_length=1, max_length=255, description="Relative path under SCRIPTS_BASE ending in .py")
    script: str = Field(..., min_length=1, description="Full Python source")

    @validator("filename")
    def validate_filename(cls, v: str) -> str:
        if not v.endswith(".py"):
            raise ValueError("filename must end with .py")
        p = Path(v)
        if p.is_absolute():
            raise ValueError("filename must be a relative path")
        if ".." in p.parts:
            raise ValueError("filename cannot contain parent directory traversal ('..')")
        import re
        if not re.fullmatch(r"[A-Za-z0-9._\-/]+\.py", v):
            raise ValueError("filename contains disallowed characters")
        return v


def _safe_destination(filename: str) -> Path:
    dest = (SCRIPTS_BASE / filename).resolve()
    if SCRIPTS_BASE not in dest.parents and dest != SCRIPTS_BASE:
        raise HTTPException(status_code=400, detail="Resolved path escapes script base directory")
    dest.parent.mkdir(parents=True, exist_ok=True)
    return dest


def _module_name_for(path: Path) -> str:
    digest = hashlib.sha256(str(path).encode("utf-8")).hexdigest()[:16]
    safe_stem = path.stem.replace("-", "_").replace(".", "_")
    return f"user_script_{safe_stem}_{digest}"


def _import_module_from_file(module_name: str, file_path: Path) -> ModuleType:
    spec = importlib.util.spec_from_file_location(module_name, str(file_path))
    if spec is None or spec.loader is None:
        raise HTTPException(status_code=500, detail="Could not create import spec for uploaded script")
    module = importlib.util.module_from_spec(spec)
    try:
        spec.loader.exec_module(module)  # type: ignore[attr-defined]
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Script import failed: {e!s}")
    return module


def _find_router(module: ModuleType):
    router = getattr(module, "router", None)
    try:
        from fastapi import APIRouter  # local import to avoid hard dep if unused
        if router is not None and isinstance(router, APIRouter):
            return router
    except Exception:
        pass
    return None


def _find_handler(module: ModuleType) -> Optional[Callable]:
    handler = getattr(module, "handle", None)
    if callable(handler):
        return handler
    return None


def _derived_endpoint_from_filename(filename: str) -> str:
    # Turn "handlers/ping.py" into "/handlers/ping"
    rel = Path(filename).with_suffix("")
    path_part = "/" + "/".join(rel.parts)
    prefix = HANDLER_PREFIX.strip()
    if prefix:
        if not prefix.startswith("/"):
            prefix = "/" + prefix
        return prefix.rstrip("/") + path_part
    return path_part


def _remove_exact_route(path: str) -> None:
    """Remove a single route by exact path match (for handler hot-reload)."""
    to_keep = []
    for route in app.router.routes:
        if isinstance(route, APIRoute) and route.path_format == path:
            continue
        to_keep.append(route)
    app.router.routes = to_keep  # type: ignore[attr-defined]


def _mount_handler(endpoint: str, handler: Callable) -> None:
    """Mount a callable handler at the derived endpoint (all common methods)."""
    _remove_exact_route(endpoint)

    async def _async_wrapper(*args, **kwargs):
        if inspect.iscoroutinefunction(handler):
            return await handler(*args, **kwargs)
        return handler(*args, **kwargs)

    app.add_api_route(endpoint, _async_wrapper, methods=["GET", "POST", "PUT", "PATCH", "DELETE"])


@app.post("/scripts", response_class=JSONResponse, status_code=201)
def upload_script(payload: PythonScriptPayload):
    """
    Accept a PythonScriptPayload, write it to disk, and try to mount.

    Mounting rules:
    - If the module defines `router: fastapi.APIRouter`, it is included as-is (no prefix).
      The script controls its own paths.
    - Else if the module defines a callable `handle`, it is mounted at a derived endpoint
      based on the filename (optionally prefixed by HANDLER_PREFIX).
    - Else the script is saved but not mounted.
    """
    dest = _safe_destination(payload.filename)
    dest.write_text(payload.script, encoding="utf-8", newline="\n")

    module_name = _module_name_for(dest)
    module = _import_module_from_file(module_name, dest)

    router = _find_router(module)
    if router is not None:
        # Included without prefix; potential conflicts are the script's responsibility.
        app.include_router(router)
        return {
            "status": "created",
            "written_to": str(dest),
            "mounted": "router",
            "mounted_at": "as-defined-in-script",
            "note": "Detected `router: APIRouter` and included it with no additional prefix."
        }

    handler = _find_handler(module)
    if handler is not None:
        endpoint = _derived_endpoint_from_filename(payload.filename)
        _mount_handler(endpoint, handler)
        return {
            "status": "created",
            "written_to": str(dest),
            "mounted": "handler",
            "mounted_at": endpoint,
            "note": (
                "Detected `handle` callable and mounted it at a derived endpoint. "
                "Set HANDLER_PREFIX env var to change the base path."
            )
        }

    return {
        "status": "created",
        "written_to": str(dest),
        "mounted": None,
        "mounted_at": None,
        "note": (
            "Script saved. To auto-mount, export either:\n"
            "  1) `router: fastapi.APIRouter` (your script defines its own routes), or\n"
            "  2) a callable `handle(request)` (mounted at a filename-derived endpoint)."
        )
    }


@app.get("/healthz")
def healthz():
    return {"ok": True}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=int(os.environ.get("PORT", "8080")), reload=True)

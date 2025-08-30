import os
import uuid
import logging
from logging.config import dictConfig
from time import perf_counter

from fastapi import FastAPI, Request

# ---------- Logging setup ----------
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO").upper()

dictConfig({
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "default": {"format": "%(asctime)s %(levelname)s %(name)s req_id=%(request_id)s %(message)s"},
        "access": {"format": "%(asctime)s %(levelname)s uvicorn.access %(message)s"},
    },
    "handlers": {
        "default": {"class": "logging.StreamHandler", "formatter": "default"},
        "access":  {"class": "logging.StreamHandler", "formatter": "access"},
    },
    "loggers": {
        "uvicorn":        {"handlers": ["default"], "level": LOG_LEVEL},
        "uvicorn.error":  {"handlers": ["default"], "level": LOG_LEVEL, "propagate": False},
        "uvicorn.access": {"handlers": ["access"],  "level": LOG_LEVEL, "propagate": False},
        "ollama_whisperer": {"handlers": ["default"], "level": LOG_LEVEL, "propagate": False},
    },
    "root": {"handlers": ["default"], "level": LOG_LEVEL},
})

class RequestIdFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        if not hasattr(record, "request_id"):
            record.request_id = "-"
        return True

_request_id_filter = RequestIdFilter()
for _logger_name in ("", "ollama_whisperer", "uvicorn", "uvicorn.error", "uvicorn.access"):
    _logger = logging.getLogger(_logger_name)
    for _h in _logger.handlers:
        _h.addFilter(_request_id_filter)

logger = logging.getLogger("ollama_whisperer")

# ---------- App ----------
app = FastAPI(title="ollama-json-schema-generator", version="0.4.0")

@app.middleware("http")
async def log_requests(request: Request, call_next):
    request_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
    start = perf_counter()

    logger.info("→ %s %s", request.method, request.url.path, extra={"request_id": request_id})

    response = None
    try:
        response = await call_next(request)
        return response
    except Exception:
        logger.exception("Unhandled exception during request", extra={"request_id": request_id})
        raise
    finally:
        duration_ms = int((perf_counter() - start) * 1000)
        status = getattr(response, "status_code", 500)
        client_host = getattr(getattr(request, "client", None), "host", "-")

        logger.info(
            "← %s %s status=%s duration_ms=%s client=%s",
            request.method,
            request.url.path,
            status,
            duration_ms,
            client_host,
            extra={"request_id": request_id},
        )
        if response is not None:
            response.headers["X-Request-ID"] = request_id

@app.get("/health")
async def health() -> dict:
    logger.debug("Health check hit")
    return {"status": "ok"}

# ---------- Routers ----------
from .generator import router as generator_router
from .linter import router as linter_router

app.include_router(generator_router, tags=["schema-generate"])
app.include_router(linter_router, tags=["schema-lint"])

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8080, reload=True, log_config=None)

import os
import uuid
import logging
from logging.config import dictConfig
from time import perf_counter

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel, Field


# ---------- Logging setup ----------
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO").upper()

dictConfig({
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "default": {
            "format": "%(asctime)s %(levelname)s %(name)s req_id=%(request_id)s %(message)s"
        },
        "access": {
            # No special fields; let Uvicorn’s message render as text.
            "format": "%(asctime)s %(levelname)s uvicorn.access %(message)s"
        },
    },
    "handlers": {
        "default": {"class": "logging.StreamHandler", "formatter": "default"},
        "access":  {"class": "logging.StreamHandler", "formatter": "access"},
    },
    "loggers": {
        # Keep uvicorn’s own loggers but use our handlers/levels
        "uvicorn":        {"handlers": ["default"], "level": LOG_LEVEL},
        "uvicorn.error":  {"handlers": ["default"], "level": LOG_LEVEL, "propagate": False},
        "uvicorn.access": {"handlers": ["access"],  "level": LOG_LEVEL, "propagate": False},

        # App logger
        "ollama_whisperer": {"handlers": ["default"], "level": LOG_LEVEL, "propagate": False},
    },
    # Ensure 3rd-party logs go somewhere consistent
    "root": {"handlers": ["default"], "level": LOG_LEVEL},
})


# Small helper to attach request_id even if caller forgets to pass it in `extra`
class RequestIdFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        if not hasattr(record, "request_id"):
            record.request_id = "-"
        return True


# Attach RequestIdFilter to all relevant handlers (root + our named loggers)
_request_id_filter = RequestIdFilter()
for _logger_name in ("", "ollama_whisperer", "uvicorn", "uvicorn.error", "uvicorn.access"):
    _logger = logging.getLogger(_logger_name)
    for _h in _logger.handlers:
        _h.addFilter(_request_id_filter)


logger = logging.getLogger("ollama_whisperer")


# ---------- Models ----------
class PromptRequest(BaseModel):
    userPrompt: str = Field(
        ..., max_length=2000, description="Natural-language request from the user."
    )


class LlamaInput(BaseModel):
    model: str
    stream: bool = Field(default=False, description="If true, request streaming/chunked output from the LLM.")
    prompt: str


# ---------- App ----------
app = FastAPI(title="ollama-whisperer", version="0.1.0")


# Request/response logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    request_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
    start = perf_counter()

    logger.info(
        "→ %s %s",
        request.method,
        request.url.path,
        extra={"request_id": request_id},
    )

    response = None
    try:
        response = await call_next(request)
        return response
    except Exception:
        logger.exception(
            "Unhandled exception during request",
            extra={"request_id": request_id},
        )
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

        # Echo request id back for tracing across services
        if response is not None:
            response.headers["X-Request-ID"] = request_id


@app.get("/health")
async def health() -> dict:
    logger.debug("Health check hit")
    return {"status": "ok"}


@app.post("/wrap", response_model=LlamaInput)
async def wrap_prompt(req: PromptRequest, request: Request) -> LlamaInput:
    request_id = request.headers.get("X-Request-ID", "-")

    if not req.userPrompt or not req.userPrompt.strip():
        logger.warning(
            "Validation failed: empty userPrompt",
            extra={"request_id": request_id},
        )
        raise HTTPException(status_code=400, detail="userPrompt must be non-empty")

    # Avoid logging raw prompts if they may contain sensitive info
    prompt = req.userPrompt.strip()
    logger.info(
        "Wrapping prompt (length=%s)",
        len(prompt),
        extra={"request_id": request_id},
    )

    return LlamaInput(
        model="llama3.2",
        stream=False,
        prompt=prompt,
    )


if __name__ == "__main__":
    import uvicorn

    # If you keep our dictConfig, pass log_config=None to avoid Uvicorn overwriting it.
    uvicorn.run(
        "ollama_whisperer.main:app",
        host="0.0.0.0",
        port=8080,
        reload=True,
        log_config=None,
        # access_log=False,  # uncomment to avoid double logging with our middleware
    )

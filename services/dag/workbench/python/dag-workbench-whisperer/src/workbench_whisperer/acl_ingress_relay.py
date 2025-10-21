from __future__ import annotations

import logging
import os
import time
import uuid
from typing import Any, Dict, List, Optional

import httpx
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field, validator

logger = logging.getLogger("workbench_whisperer")

# -----------------------------
# Env / config
# -----------------------------
JAVA_BASE_URL: str = os.getenv("JAVA_BASE_URL", "http://ubiquia-core-flow-service:8080/ubiquia/flow-service/agent-communication-language")
JAVA_QUERY_PATH: str = os.getenv("JAVA_QUERY_PATH", "/query")

HTTP_TIMEOUT_SECS: float = float(os.getenv("HTTP_TIMEOUT_SECS", "10"))
RETRY_ATTEMPTS: int = int(os.getenv("RETRY_ATTEMPTS", "3"))
RETRY_BACKOFF_SECS: float = float(os.getenv("RETRY_BACKOFF_SECS", "0.5"))

router = APIRouter()

# -----------------------------
# Models
# -----------------------------
class IngressResponse(BaseModel):
    id: str = Field(..., min_length=1, description="Primary key used to query core service")
    modelType: str = "IngressResponse"
    payloadModelType: str = "IngressResponse"

    @validator("id")
    def _id_nonempty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("id must be non-empty")
        return v


class SemanticVersion(BaseModel):
    major: int
    minor: int
    patch: int


class AgentCommunicationLanguage(BaseModel):
    domain: str
    graphs: Optional[List[Dict[str, Any]]] = None
    version: SemanticVersion
    jsonSchema: Any

    @validator("domain")
    def _domain_nonempty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("domain must be non-empty")
        return v


# -----------------------------
# Helpers
# -----------------------------
def _req_id(request: Request) -> str:
    return request.headers.get("X-Request-ID", str(uuid.uuid4()))

def _build_query_url(id_: str) -> str:
    base = JAVA_BASE_URL.rstrip("/")
    path = JAVA_QUERY_PATH.strip("/")
    return f"{base}/{path}/{id_}"

def _get_with_retries(url: str, request_id: str) -> httpx.Response:
    last_exc: Optional[Exception] = None
    for attempt in range(1, RETRY_ATTEMPTS + 1):
        try:
            with httpx.Client(timeout=HTTP_TIMEOUT_SECS) as client:
                logger.info(
                    "Querying upstream: %s (attempt %d/%d)",
                    url, attempt, RETRY_ATTEMPTS,
                    extra={"request_id": request_id},
                )
                return client.get(url)
        except Exception as exc:
            last_exc = exc
            logger.warning("Upstream request failed: %r", exc, extra={"request_id": request_id})
            if attempt < RETRY_ATTEMPTS:
                time.sleep(RETRY_BACKOFF_SECS * attempt)
    raise HTTPException(status_code=502, detail=f"Upstream unavailable: {last_exc}")


# -----------------------------
# Endpoint
# -----------------------------
@router.post("/ingress/relay", response_model=AgentCommunicationLanguage)
def relay_ingress(body: IngressResponse, request: Request):
    """
    Accepts IngressResponse, pulls `id`, calls Java GET /query/{id}, returns the ACL.
    - Upstream 204 -> 404
    - Other upstream 4xx/5xx -> 502 with upstream payload included
    """
    req_id = _req_id(request)
    url = _build_query_url(body.id)

    logger.info("Relay request id=%s -> %s", body.id, url, extra={"request_id": req_id})
    resp = _get_with_retries(url, req_id)

    if resp.status_code == 204:
        logger.info("No content for id=%s", body.id, extra={"request_id": req_id})
        raise HTTPException(status_code=404, detail=f"No model found for id '{body.id}'")

    if resp.status_code == 404:
        logger.info("Upstream 404 for id=%s", body.id, extra={"request_id": req_id})
        raise HTTPException(status_code=404, detail=f"Upstream returned 404 for id '{body.id}'")

    if resp.status_code >= 400:
        logger.error(
            "Upstream error %s: %s", resp.status_code, resp.text, extra={"request_id": req_id}
        )
        raise HTTPException(status_code=502, detail=f"Upstream error {resp.status_code}: {resp.text}")

    try:
        data = resp.json()
    except ValueError:
        logger.error("Non-JSON response for id=%s", body.id, extra={"request_id": req_id})
        raise HTTPException(status_code=502, detail="Upstream returned non-JSON body")

    try:
        acl = AgentCommunicationLanguage.parse_obj(data)
    except Exception as e:
        logger.exception("Response validation failed for id=%s", body.id, extra={"request_id": req_id})
        raise HTTPException(status_code=502, detail=f"Response validation failed: {e}")

    return acl

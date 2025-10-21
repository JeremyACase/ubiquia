from __future__ import annotations

from typing import Union, Any, Optional, Literal
import os
import re

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

router = APIRouter()

# --------- Defaults (overridable via env) ---------
DEFAULT_DOMAIN = os.getenv("ACL_DEFAULT_DOMAIN", "generated-domain")
DEFAULT_VERSION = (
    int(os.getenv("ACL_DEFAULT_VERSION_MAJOR", "1")),
    int(os.getenv("ACL_DEFAULT_VERSION_MINOR", "0")),
    int(os.getenv("ACL_DEFAULT_VERSION_PATCH", "0")),
)

# ---------- Models ----------
class SemanticVersion(BaseModel):
    major: int
    minor: int
    patch: int

class GeneratedAgentCommunicationLanguage(BaseModel):
    """
    Minimal ACL envelope needed for extraction.
    """
    domain: Optional[str] = Field(
        default=None,
        max_length=120,
        pattern=r"^[a-zA-Z][a-zA-Z0-9\-_.]{1,119}$"
    )
    version: Optional[SemanticVersion] = None
    modelType: Optional[Literal["AgentCommunicationLanguage"]] = "AgentCommunicationLanguage"
    # Accept object/boolean/string per your schema
    jsonSchema: Optional[Union[dict, bool, str]] = None

class BeliefStateGeneration(BaseModel):
    """
    Output POJO-equivalent:
      domainName: String (not null)
      version:    SemanticVersion (not null)
      modelType:  always "BeliefStateGeneration"
    """
    domainName: str = Field(..., max_length=120, pattern=r"^[a-zA-Z][a-zA-Z0-9\-_.]{1,119}$")
    version: SemanticVersion
    modelType: Literal["BeliefStateGeneration"] = "BeliefStateGeneration"

# ---------- Helpers ----------
_DOMAIN_RE = re.compile(r"^[a-zA-Z][a-zA-Z0-9\-_.]{1,119}$")

def _slugify_for_domain(title: str) -> str:
    t = title.strip().lower()
    t = re.sub(r"\s+", "-", t)
    t = re.sub(r"[^a-z0-9\-_.]", "", t)
    if len(t) > 120:
        t = t[:120]
    if not t or not t[0].isalpha():
        t = ("a" + t)[:120]
    return t

def _fallback_domain() -> str:
    if _DOMAIN_RE.match(DEFAULT_DOMAIN or ""):
        return DEFAULT_DOMAIN
    return "generated-domain"

def _resolve_domain_from_sources(acl: GeneratedAgentCommunicationLanguage) -> str:
    if acl.domain and _DOMAIN_RE.match(acl.domain):
        return acl.domain

    schema = acl.jsonSchema if isinstance(acl.jsonSchema, dict) else None
    if schema:
        raw_title = schema.get("title")
        if isinstance(raw_title, str) and raw_title.strip():
            cand = _slugify_for_domain(raw_title)
            if _DOMAIN_RE.match(cand):
                return cand

        schema_id = schema.get("$id")
        if isinstance(schema_id, str) and schema_id.strip():
            seg = schema_id.split("://", 1)[0] if "://" in schema_id else schema_id.split("/", 1)[0]
            cand = _slugify_for_domain(seg)
            if _DOMAIN_RE.match(cand):
                return cand

    return _fallback_domain()

def _resolve_version(acl: GeneratedAgentCommunicationLanguage) -> SemanticVersion:
    if acl.version is not None:
        return acl.version
    major, minor, patch = DEFAULT_VERSION
    return SemanticVersion(major=major, minor=minor, patch=patch)

# ---------- Endpoint ----------
@router.post(
    "/acl/to-belief-state-generation",
    response_model=BeliefStateGeneration,
    summary="Extract a BeliefStateGeneration from an Agent Communication Language (ACL) envelope.",
)
def to_belief_state_generation(acl: GeneratedAgentCommunicationLanguage) -> Any:
    try:
        domain_name = _resolve_domain_from_sources(acl)
        version = _resolve_version(acl)
        return BeliefStateGeneration(
            domainName=domain_name,
            version=version,
            modelType="BeliefStateGeneration",
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to derive BeliefStateGeneration: {e}")

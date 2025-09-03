# src/ollama_whisperer/acl_wrapper.py
from typing import Union, Any, Optional, List, Literal
import json
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

class LlamaOutput(BaseModel):
    model: str
    created_at: str
    response: str = Field(..., description="Text output from the model, often containing a JSON Schema")
    done: bool
    done_reason: Optional[str] = None
    context: List[int] = Field(default_factory=list)

class GeneratedAgentCommunicationLanguage(BaseModel):
    # Pydantic v2: use `pattern=` and `Literal[...]` instead of `regex=` / `const=`
    domain: str = Field(..., max_length=120, pattern=r"^[a-zA-Z][a-zA-Z0-9\-_.]{1,119}$")
    version: SemanticVersion
    modelType: Literal["AgentCommunicationLanguage"] = "AgentCommunicationLanguage"
    jsonSchema: Union[dict, str]

# ---------- Helpers ----------
_DOMAIN_RE = re.compile(r"^[a-zA-Z][a-zA-Z0-9\-_.]{1,119}$")

def _strip_fences(s: str) -> str:
    """
    Remove ```json ... ``` or ``` ... ``` fences if an LLM included them.
    """
    s = s.strip()
    if s.startswith("```") and s.endswith("```"):
        s = s[3:-3].strip()
        # optional leading language label like "json"
        if s.lower().startswith("json"):
            s = s[4:].lstrip()
    return s

def _parse_schema_from_response(resp: str) -> Union[dict, str]:
    """
    Try to parse the response string into a JSON object.
    If it fails, return the original string (lenient mode).
    """
    s = _strip_fences(resp)
    if s.startswith("{") or s.startswith("["):
        try:
            return json.loads(s)
        except Exception:
            return resp
    return resp

def _slugify_for_domain(title: str) -> str:
    """
    Convert a title to a domain-friendly slug that matches the allowed charset [-_.A-Za-z0-9].
    - Lowercase
    - Replace whitespace with '-'
    - Drop characters not in [-_.A-Za-z0-9]
    - Trim to max length 120 (we'll ensure the first char is a letter separately)
    """
    t = title.strip().lower()
    # Replace whitespace runs with single '-'
    t = re.sub(r"\s+", "-", t)
    # Remove disallowed characters
    t = re.sub(r"[^a-z0-9\-_.]", "", t)
    # Ensure length cap
    if len(t) > 120:
        t = t[:120]
    # Ensure first char is a letter; if not, prefix with 'a'
    if not t or not t[0].isalpha():
        t = ("a" + t)[:120]
    return t

def _resolve_domain_from_schema_title(schema: Union[dict, str]) -> str:
    """
    Extract the JSON Schema 'title' and convert it into a valid domain per the pattern.
    Fallback to DEFAULT_DOMAIN if we cannot produce a valid domain.
    """
    # If schema isn't a dict, we can't extract title
    if not isinstance(schema, dict):
        return _fallback_domain()

    raw_title = schema.get("title")
    if isinstance(raw_title, str) and raw_title.strip():
        candidate = _slugify_for_domain(raw_title)
        if _DOMAIN_RE.match(candidate):
            return candidate

    # If title is missing/invalid, try $id host-ish prefix as a bonus
    schema_id = schema.get("$id")
    if isinstance(schema_id, str) and schema_id.strip():
        # very liberal parse: take the segment before '://' or the first '/'
        cand = schema_id.split("://", 1)[0] if "://" in schema_id else schema_id.split("/", 1)[0]
        cand = _slugify_for_domain(cand)
        if _DOMAIN_RE.match(cand):
            return cand

    # Fallback
    return _fallback_domain()

def _fallback_domain() -> str:
    """
    Last resort: use the configured DEFAULT_DOMAIN if valid, else a safe hardcoded fallback.
    """
    if _DOMAIN_RE.match(DEFAULT_DOMAIN or ""):
        return DEFAULT_DOMAIN
    return "generated-domain"

# ---------- Endpoint ----------
@router.post(
    "/acl/from-llama-output",
    response_model=GeneratedAgentCommunicationLanguage,
    summary="Wrap a LlamaOutput response into an Agent Communication Language (ACL) envelope.",
)
def acl_from_llama_output(payload: LlamaOutput) -> Any:
    try:
        # Validate configured default up-front to avoid surprising 500s later
        if not _DOMAIN_RE.match(DEFAULT_DOMAIN or ""):
            # Don't fail the whole request—just ensure we have a safe fallback
            pass

        schema = _parse_schema_from_response(payload.response)
        domain = _resolve_domain_from_schema_title(schema)

        major, minor, patch = DEFAULT_VERSION
        return GeneratedAgentCommunicationLanguage(
            domain=domain,
            version=SemanticVersion(major=major, minor=minor, patch=patch),
            jsonSchema=schema,
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to wrap schema from LlamaOutput: {e}")

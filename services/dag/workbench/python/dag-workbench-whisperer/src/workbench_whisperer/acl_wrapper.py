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
    # NEW: carry forward the original prompt from LlamaInput
    prompt: Optional[str] = Field(
        default=None,
        description="The original prompt used to generate this response."
    )

class GeneratedAgentCommunicationLanguage(BaseModel):
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

def _inject_description(schema: Union[dict, str], description: Optional[str]) -> Union[dict, str]:
    """
    If the parsed schema is a dict, set/override the top-level 'description'
    with the provided description (trimmed). Creates the field if missing.
    If the schema isn't a dict, return it unchanged.
    """
    if not description:
        return schema
    if isinstance(schema, dict):
        schema["description"] = description.strip()
    return schema

def _slugify_for_domain(title: str) -> str:
    t = title.strip().lower()
    t = re.sub(r"\s+", "-", t)
    t = re.sub(r"[^a-z0-9\-_.]", "", t)
    if len(t) > 120:
        t = t[:120]
    if not t or not t[0].isalpha():
        t = ("a" + t)[:120]
    return t

def _resolve_domain_from_schema_title(schema: Union[dict, str]) -> str:
    if not isinstance(schema, dict):
        return _fallback_domain()

    raw_title = schema.get("title")
    if isinstance(raw_title, str) and raw_title.strip():
        candidate = _slugify_for_domain(raw_title)
        if _DOMAIN_RE.match(candidate):
            return candidate

    schema_id = schema.get("$id")
    if isinstance(schema_id, str) and schema_id.strip():
        cand = schema_id.split("://", 1)[0] if "://" in schema_id else schema_id.split("/", 1)[0]
        cand = _slugify_for_domain(cand)
        if _DOMAIN_RE.match(cand):
            return cand

    return _fallback_domain()

def _fallback_domain() -> str:
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
        # Parse the model response
        schema = _parse_schema_from_response(payload.response)

        # Inject the prompt into the schema's description (if we have both)
        schema = _inject_description(schema, payload.prompt)

        # Determine domain (uses title/$id or falls back)
        domain = _resolve_domain_from_schema_title(schema)

        major, minor, patch = DEFAULT_VERSION
        return GeneratedAgentCommunicationLanguage(
            domain=domain,
            version=SemanticVersion(major=major, minor=minor, patch=patch),
            jsonSchema=schema,
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to wrap schema from LlamaOutput: {e}")

from __future__ import annotations

import json
import os
import re
import textwrap
from typing import Any, Dict, List, Tuple

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

# ---------------------------
# Models (reuse if available)
# ---------------------------
try:
    from .acl_wrapper import GeneratedAgentCommunicationLanguage, SemanticVersion  # type: ignore
except Exception:
    class SemanticVersion(BaseModel):
        major: int
        minor: int
        patch: int

    class GeneratedAgentCommunicationLanguage(BaseModel):
        domain: str
        version: SemanticVersion
        modelType: str = "AgentCommunicationLanguage"
        jsonSchema: Any

class LlamaInput(BaseModel):
    model: str = Field(..., description="The model identifier to invoke.")
    prompt: str = Field(..., description="The natural-language prompt to send to the LLM.", max_length=300000)
    stream: bool = Field(default=False, description="Whether to stream output tokens.")

router = APIRouter(tags=["dag"])

# ---------------------------
# Model resolution
# ---------------------------
def _resolve_model() -> str:
    """
    Decide which model to use with easy swapping via env:
      - WORKBENCH_LLM_FAMILY: preset family key ('gemma2' default, 'llama3', etc.)
      - WORKBENCH_LLM_MODEL : explicit model tag/name override (takes precedence)
    """
    override = os.getenv("WORKBENCH_LLM_MODEL")
    if override and override.strip():
        return override.strip()

    family = (os.getenv("WORKBENCH_LLM_FAMILY", "gemma2") or "gemma2").strip().lower()

    # Minimal, opinionated defaults; extend as you add runners/images.
    family_defaults = {
        "gemma2": "gemma2:latest",   # e.g., gemma2:9b-instruct-q4_K_M
        "llama3": "llama3.2",        # e.g., llama3.2:latest
        # Add more here as needed:
        # "mistral": "mistral-nemo:latest",
        # "phi4": "phi4:latest",
    }
    return family_defaults.get(family, family_defaults["gemma2"])

DEFAULT_MODEL = _resolve_model()

# ---------------------------
# Helpers
# ---------------------------
def _squish(text: str) -> str:
    return re.sub(r"\s+", " ", (text or "").strip())

def _dedent(text: str) -> str:
    return textwrap.dedent(text).strip()

def _collect_definition_names(acl: Dict[str, Any]) -> List[str]:
    defs = (((acl or {}).get("jsonSchema") or {}).get("definitions") or {})
    return [k for k in defs.keys() if isinstance(k, str)]

def _validate_acl_minimal(acl: Dict[str, Any]) -> Tuple[str, Dict[str, int], List[str]]:
    if not isinstance(acl, dict):
        raise ValueError("`acl` must be an object.")
    domain = acl.get("domain")
    version = acl.get("version")
    if not isinstance(domain, str) or not domain:
        raise ValueError("`acl.domain` is required and must be a non-empty string.")
    if not isinstance(version, dict) or not all(k in version for k in ("major", "minor", "patch")):
        raise ValueError("`acl.version` must contain major/minor/patch.")
    model_names = _collect_definition_names(acl)
    if not model_names:
        raise ValueError("`acl.jsonSchema.definitions` must contain at least one model.")
    return domain, version, model_names

def _build_llm_prompt(acl: Dict[str, Any], domain: str, version: Dict[str, int], model_names: List[str]) -> str:
    """
    Compose the natural-language prompt instructing the LLM to return a single JSON DAG,
    and include the FULL GeneratedAgentCommunicationLanguage JSON verbatim.
    """
    # Pretty-print the exact ACL for the model to ground on
    acl_json = json.dumps(acl, ensure_ascii=False, sort_keys=True, indent=2)

    # We keep the JSON block multi-line and avoid collapsing whitespace
    prompt = f"""
You are a Ubiquia DAG synthesizer.

INPUTS:
1) STRICT INSTRUCTIONS:
   - Produce exactly ONE JSON object (no Markdown, no YAML, no comments) representing a Ubiquia DAG.
   - The DAG models a workflow that operates purely over the ACL's `jsonSchema.definitions`.
   - Every Adapter's `inputSubSchemas` and `outputSubSchema` MUST reference ONLY model names from this set: {model_names}
   - Components MUST be Python script components (scripts will be generated downstream).
   - Do NOT invent model names or reference anything outside the ACL.
   - Required fields:
     - "modelType": "Graph"
     - "domainDataContract": {{
         "name": "{_squish(domain)}",
         "version": {{"major": {version['major']}, "minor": {version['minor']}, "patch": {version['patch']}}}
       }}

2) EXACT ACL JSON (verbatim; base ALL choices on this object):
{acl_json}

YOUR TASK:
- Return ONLY the JSON DAG object that satisfies the above rules and is fully consistent with the provided ACL.
"""
    return _dedent(prompt)

# ---------------------------
# Endpoint
# ---------------------------
@router.post("/wrap-dag", response_model=LlamaInput, summary="Wrap ACL into a LlamaInput prompt for DAG generation")
async def wrap_dag(acl: GeneratedAgentCommunicationLanguage) -> LlamaInput:
    """
    Accepts a GeneratedAgentCommunicationLanguage and returns a LlamaInput
    that, when sent to the LLM, will produce a JSON DAG constrained to the ACL's definitions.
    """
    try:
        acl_dict = acl.dict() if hasattr(acl, "dict") else acl
        domain, version, model_names = _validate_acl_minimal(acl_dict)

        prompt = _build_llm_prompt(acl=acl_dict, domain=domain, version=version, model_names=model_names)
        return LlamaInput(model=DEFAULT_MODEL, prompt=prompt, stream=False)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to wrap ACL into LlamaInput: {e}")

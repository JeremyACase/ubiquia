import os
import re
import textwrap
import logging
from typing import Optional, Dict, Any

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

router = APIRouter()
logger = logging.getLogger("ollama_whisperer")

# ---------- Helpers ----------
def squish(text: str) -> str:
    """
    Dedent, trim, and collapse all runs of whitespace (including newlines) to single spaces.
    """
    t = textwrap.dedent(text).strip()
    t = re.sub(r'\s+', ' ', t)
    return t

# ---------- Prompt scaffolding (Schema-only) ----------
SCHEMA_ONLY_SYSTEM = (
    "You are a JSON Schema synthesizer. Output ONLY a single valid JSON object that is a Draft-07 JSON Schema. "
    "No code fences. No prose. No explanations."
)

SCHEMA_CONTRACT = """Contract (must satisfy):
- Produce exactly ONE JSON object that is a Draft-07 JSON Schema.
- Top-level describes the primary object implied by the user's request and MUST include:
  { "$schema": "http://json-schema.org/draft-07/schema#", "type": "object", ... }.
- Do NOT embed/inline object schemas inside properties or array items.
  Instead, declare EVERY object type under "definitions" and reference it with "$ref".
  (Use "definitions" explicitly — not "$defs".)
- Use "additionalProperties": false at the top-level and for object definitions unless open-ended extension is clearly required.
- Use "required" appropriately; add obvious constraints (format, pattern, min/max, minLength/maxLength, enum) when evident.
- Keys must be stable and machine-friendly.
- Output ONLY the JSON Schema. No markdown. No commentary. No surrounding text.
"""

SCHEMA_PROMPT_PREAMBLE = (
    "Given the user's description, synthesize a precise Draft-07 JSON Schema for the data to be exchanged."
)

# ---------- Models ----------
class PromptRequest(BaseModel):
    userPrompt: str = Field(..., max_length=2000, description="Natural-language request from the user.")

class LlamaInput(BaseModel):
    model: str
    stream: bool = Field(default=False, description="If true, request streaming/chunked output from the LLM.")
    prompt: str
    # Added fields used by Ollama's /api/generate
    format: Optional[str] = Field(default="json", description="Response format hint (e.g., 'json').")
    system: Optional[str] = Field(default=None, description="System message for the model.")
    options: Optional[Dict[str, Any]] = Field(default=None, description="Decoder and other options.")

# ---------- Endpoints ----------
@router.post("/wrap", response_model=LlamaInput)
async def wrap_prompt(req: PromptRequest, request: Request) -> LlamaInput:
    request_id = request.headers.get("X-Request-ID", "-")

    if not req.userPrompt or not req.userPrompt.strip():
        logger.warning("Validation failed: empty userPrompt", extra={"request_id": request_id})
        raise HTTPException(status_code=400, detail="userPrompt must be non-empty")

    user_prompt = req.userPrompt.strip()
    logger.info("Wrapping prompt (length=%s)", len(user_prompt), extra={"request_id": request_id})

    # Build a compact, schema-only instruction that includes the user's prompt.
    raw_prompt = (
        SCHEMA_PROMPT_PREAMBLE + " "
        + SCHEMA_CONTRACT + " "
        + "User prompt: " + user_prompt
    )
    final_prompt = squish(raw_prompt)

    model_name = os.getenv("OLLAMA_MODEL", "llama3.2")

    return LlamaInput(
        model=model_name,
        stream=False,
        prompt=final_prompt,
        format="json",                         # Request strict JSON output
        system=squish(SCHEMA_ONLY_SYSTEM),     # Squished system message
        options={
            "temperature": float(os.getenv("OLLAMA_TEMPERATURE", "0.1")),
            "top_p": float(os.getenv("OLLAMA_TOP_P", "0.9")),
            "top_k": int(os.getenv("OLLAMA_TOP_K", "40")),
            "repeat_penalty": float(os.getenv("OLLAMA_REPEAT_PENALTY", "1.05")),
            "num_predict": int(os.getenv("OLLAMA_NUM_PREDICT", "4096")),
            "num_ctx": int(os.getenv("OLLAMA_NUM_CTX", "8192")),
        },
    )

import os
import re
import textwrap
import logging
from typing import Optional, Dict, Any, Tuple

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

router = APIRouter()
logger = logging.getLogger("workbench_whisperer")

# ---------- Helpers ----------
def squish(text: str) -> str:
    """
    Dedent, trim, and collapse all runs of whitespace (including newlines) to single spaces.
    """
    t = textwrap.dedent(text).strip()
    t = re.sub(r'\s+', ' ', t)
    return t

def resolve_llm() -> Tuple[str, Dict[str, Any]]:
    """
    Decide which model to use and supply sensible default options,
    with easy swapping via env:
      - MODEL_FAMILY: 'gemma3' (default) or 'llama3'
      - OLLAMA_MODEL: explicit model tag/name override
    """
    family = os.getenv("MODEL_FAMILY", "llama3").strip().lower()

    # Sensible defaults per family; OLLAMA_MODEL can override the name.
    if family == "llama3":
        default_name = "llama3.2"
        default_opts = {
            "temperature": 0.1,
            "top_p": 0.9,
            "top_k": 40,
            "repeat_penalty": 1.05,
            "num_predict": 4096,
            "num_ctx": 8192,
        }
    else:
        # Default to gemma3
        family = "gemma3"
        # Common Ollama tags: 'gemma3:latest', 'gemma3:9b-instruct-q4_K_M', etc.
        default_name = "gemma3"
        default_opts = {
            "temperature": 0.1,
            "top_p": 0.9,
            "top_k": 40,
            "repeat_penalty": 1.05,
            "num_predict": 4096,
            "num_ctx": 8192,
        }

    model_name = os.getenv("OLLAMA_MODEL", default_name)

    # Allow fine-grained overrides while keeping family defaults.
    opts = {
        **default_opts,
        "temperature": float(os.getenv("OLLAMA_TEMPERATURE", str(default_opts["temperature"]))),
        "top_p": float(os.getenv("OLLAMA_TOP_P", str(default_opts["top_p"]))),
        "top_k": int(os.getenv("OLLAMA_TOP_K", str(default_opts["top_k"]))),
        "repeat_penalty": float(os.getenv("OLLAMA_REPEAT_PENALTY", str(default_opts["repeat_penalty"]))),
        "num_predict": int(os.getenv("OLLAMA_NUM_PREDICT", str(default_opts["num_predict"]))),
        "num_ctx": int(os.getenv("OLLAMA_NUM_CTX", str(default_opts["num_ctx"]))),
    }
    return model_name, opts

# ---------- Prompt scaffolding (Schema-only) ----------
SCHEMA_ONLY_SYSTEM = squish("""
You are a JSON Schema synthesizer. Output ONLY a single valid JSON object that is a Draft-07 JSON Schema.
No code fences. No prose. No explanations.
""")

SCHEMA_CONTRACT = """
Contract (must satisfy):
- Produce exactly ONE JSON object that is a Draft-07 JSON Schema.
- The top-level MUST include:
  { "$schema": "http://json-schema.org/draft-07/schema#", "type": "object", ... }.
- Add a top-level field "definition" (string) that concisely condenses the user's prompt into <= 200 characters,
  in plain English, no line breaks, no quotes metadata, and without repeating these instructions.
  This is NOT "definitions"; it is a single "definition" string summary for downstream use.
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

    raw_prompt = (
        SCHEMA_PROMPT_PREAMBLE + " "
        + SCHEMA_CONTRACT + " "
        + "User prompt: " + user_prompt
    )
    final_prompt = squish(raw_prompt)

    # ---- Model selection (defaults to gemma3) ----
    model_name, options = resolve_llm()

    return LlamaInput(
        model=model_name,
        stream=False,
        prompt=final_prompt,
        format="json",                 # Request strict JSON output
        system=SCHEMA_ONLY_SYSTEM,     # Squished system message
        options=options,
    )

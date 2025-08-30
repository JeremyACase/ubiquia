import os
import json
import logging
from typing import Any, Union, Optional, List

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

router = APIRouter()
logger = logging.getLogger("ollama_whisperer")


# ---------- Models ----------
class LlamaOutput(BaseModel):
    """
    Matches the Ollama /api/generate response shape we receive upstream.
    We assume the un-linted JSON Schema is in the `response` string.
    """
    model: str
    created_at: str
    response: str = Field(..., min_length=1)
    done: bool
    done_reason: Optional[str] = None
    context: List[int] = Field(default_factory=list)


# ---------- Helpers ----------
def squish(text: str) -> str:
    from re import sub
    from textwrap import dedent

    t = dedent(text).strip()
    t = sub(r"\s+", " ", t)
    return t


def _schema_to_text(schema: Union[str, dict]) -> str:
    """
    Accepts either:
      - a Python dict representing a JSON Schema
      - a JSON string (raw) representing a JSON Schema
    Returns a compact JSON string.
    """
    if isinstance(schema, str):
        return schema.strip()
    try:
        return json.dumps(schema, ensure_ascii=False)
    except Exception as e:
        raise ValueError(f"schema not JSON-serializable: {e}")


def _extract_first_json_object(s: str) -> str:
    """
    Returns the substring containing the first complete top-level JSON object.
    Handles ``` fences and braces inside strings. Raises ValueError if none found.
    """
    import re

    s = s.strip()
    # Strip code fences if the model ignored instructions
    if s.startswith("```"):
        s = re.sub(r"^```(?:json)?\s*|\s*```$", "", s).strip()

    out = []
    depth = 0
    in_str = False
    esc = False
    started = False

    for ch in s:
        if in_str:
            out.append(ch)
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue

        if ch == '"':
            in_str = True
            out.append(ch)
            continue

        if ch == "{":
            depth += 1
            started = True
            out.append(ch)
            continue

        if ch == "}":
            if started:
                depth -= 1
                out.append(ch)
                if depth == 0:
                    return "".join(out)
            continue

        if started:
            out.append(ch)

    raise ValueError("No complete JSON object found in model output")


# ---------- Prompt scaffolding ----------
SCHEMA_LINT_SYSTEM = squish("""
You are a Draft-07 JSON Schema linter/editor.

OUTPUT RULES (COMPLIANCE-CRITICAL):
- Return EXACTLY ONE JSON object that is a valid Draft-07 JSON Schema.
- No code fences, no markdown, no preface, no analysis, no trailing text.
- The first character of your reply MUST be '{' and the last MUST be '}'.
- Do not wrap the object in an array or include additional keys outside the schema.

If you cannot fully comply, return the best valid JSON object you can—still obeying the rules above.
""")

SCHEMA_LINT_CONTRACT = """Contract (must satisfy):
- Input is an existing Draft-07 JSON Schema (possibly imperfect).
- Produce exactly ONE JSON object that is a valid Draft-07 JSON Schema.
- Preserve the original semantics and field names where sensible.
- Fix structural errors and normalize style:
  * Top-level MUST be: { "$schema": "http://json-schema.org/draft-07/schema#", "type": "object", ... }.
  * Do NOT inline object schemas inside properties or array items.
    Instead, declare EVERY object type under "definitions" and reference it via "$ref".
    (Use "definitions" explicitly — not "$defs".)
  * Prefer "additionalProperties": false on object types unless open-ended extension is clearly required.
  * Use "required" appropriately and keep obvious constraints (format, pattern, min/max, minLength/maxLength, enum).
  * Keep descriptions when present; do not add marketing prose.
- If the input is invalid or incomplete, repair it conservatively while staying faithful to intent.
- Output ONLY the JSON Schema. No markdown. No commentary. No surrounding text.
"""

SCHEMA_LINT_PREAMBLE = (
    "Given the provided Draft-07 JSON Schema, lint and (optionally) apply edits while preserving intent."
)


def _build_lint_prompt(schema_text: str) -> str:
    base = (
        SCHEMA_LINT_PREAMBLE + " "
        + SCHEMA_LINT_CONTRACT + " "
        + "Input schema is delimited by <schema>...</schema>. "
        + "No explicit edits requested; perform linting/normalization only."
    )
    raw = f"{base} <schema>{schema_text}</schema>"
    return squish(raw)


# ---------- Endpoint ----------
@router.post("/lint")
async def lint(llama_output: LlamaOutput, request: Request) -> dict:
    """
    Accepts a LlamaOutput (Ollama response), extracts the candidate schema from `response`,
    and RETURNS the /api/generate payload that will cause Ollama to lint/repair the schema.

    This endpoint DOES NOT call Ollama; your adapters can forward the returned `request` to `target`.
    """
    request_id = request.headers.get("X-Request-ID", "-")

    raw_text = llama_output.response or ""
    if not raw_text.strip():
        raise HTTPException(status_code=400, detail="LlamaOutput.response is empty")

    # Best-effort: extract the first JSON object from the response; if not found, pass raw text
    try:
        schema_text = _extract_first_json_object(raw_text)
    except ValueError:
        logger.warning(
            "No JSON object found in LlamaOutput.response; passing raw text to linter",
            extra={"request_id": request_id},
        )
        schema_text = _schema_to_text(raw_text)

    final_prompt = _build_lint_prompt(schema_text)
    system_text = squish(SCHEMA_LINT_SYSTEM)

    model_name = os.getenv("OLLAMA_MODEL", "llama3.2")
    base_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434").rstrip("/")
    generate_url = f"{base_url}/api/generate"

    stream_enabled = os.getenv("OLLAMA_STREAM", "false").lower() in ("1", "true", "yes", "on")

    payload = {
        "model": model_name,
        "prompt": final_prompt,
        "format": "json",
        "system": system_text,
        "stream": stream_enabled,
        "options": {
            "temperature": float(os.getenv("OLLAMA_TEMPERATURE", "0.0")),  # deterministic for linting
            "top_p": float(os.getenv("OLLAMA_TOP_P", "0.9")),
            "top_k": int(os.getenv("OLLAMA_TOP_K", "40")),
            "repeat_penalty": float(os.getenv("OLLAMA_REPEAT_PENALTY", "1.05")),
            "num_predict": int(os.getenv("OLLAMA_NUM_PREDICT", "4096")),
            "num_ctx": int(os.getenv("OLLAMA_NUM_CTX", "8192")),
        },
    }

    logger.info("Prepared Ollama generate payload (no network call).", extra={"request_id": request_id})

    # Return exactly what your bus needs to call Ollama
    return {
        "target": generate_url,
        "request": payload
    }

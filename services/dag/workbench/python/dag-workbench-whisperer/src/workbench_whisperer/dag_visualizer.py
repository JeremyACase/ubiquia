from __future__ import annotations

import re
import textwrap
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

router = APIRouter(tags=["dag"])


# ---------------------------
# Models
# ---------------------------

class LlamaOutput(BaseModel):
    """
    Minimal Llama /api/generate-style response that we care about.
    - Input: response is expected to contain the YAML DAG (or mostly just that).
    """
    model: str = Field(..., description="Model that generated the response or logical pipeline stage.")
    response: str = Field(..., description="Raw text payload (DAG YAML, possibly with markdown fences).")


class LlamaInput(BaseModel):
    """
    Standard Llama /api/generate-style request.
    - Output: prompt will instruct the model to produce Mermaid markdown for the DAG.
    """
    model: str = Field(..., description="The model identifier to invoke.")
    prompt: str = Field(..., description="Prompt text to send to the model.", max_length=300000)
    stream: bool = Field(default=False, description="Whether to stream output tokens.")


# ---------------------------
# Helpers
# ---------------------------

def _dedent(text: Optional[str]) -> str:
    """Remove indentation and trim."""
    return textwrap.dedent(text or "").strip()


def _strip_markdown_fences(text: str) -> str:
    """
    Best-effort removal of Markdown code fences if the model
    ignored a 'no backticks' rule and wrapped the YAML.
    """
    t = text.strip()

    if t.startswith("```"):
        # Strip the first fence line
        t = re.sub(r"^```[a-zA-Z0-9_-]*\s*", "", t, count=1, flags=re.MULTILINE)
        # Strip a trailing fence if present
        t = re.sub(r"\s*```$", "", t, count=1, flags=re.MULTILINE)

    return t.strip()


def _extract_dag_yaml(llama: LlamaOutput) -> str:
    """
    Extract the YAML DAG text from a LlamaOutput.
    Assumes the DAG is in llama.response, possibly wrapped in code fences.
    """
    raw = _dedent(llama.response)
    return _strip_markdown_fences(raw)


def _build_mermaid_prompt(dag_yaml: str) -> str:
    """
    Build a prompt that instructs the LLM to convert the given Ubiquia DAG YAML
    into a Mermaid DAG visualization.
    """
    # We do NOT rely on the model parsing the YAML here; we just embed it and tell it what to do.
    # You can tighten or relax these rules as needed.
    prompt = f"""
You are a Ubiquia DAG visualization assistant.

TASK:
Given the YAML definition of a Ubiquia DAG, generate a Mermaid graph definition
that visualizes the DAG's adapters and their edges.

INPUT DAG (YAML):
{dag_yaml}

OUTPUT RULES:
- Output ONLY a Mermaid diagram (no prose, no backticks, no markdown code fences).
- Use 'graph LR' as the root directive (left-to-right layout).
- Treat each adapter in 'componentlessAdapters' as a node.
- For each entry in 'edges', generate links of the form:
    leftAdapterName --> rightAdapterName
- Use adapter names as both the node ID (sanitized to be Mermaid-safe) and the human-readable label, e.g.:
    someId["Adapter Name"]
- Ensure the output is valid Mermaid syntax that can be rendered as a diagram.
- Do NOT include any explanation or commentary, only the Mermaid graph.

Return ONLY the Mermaid graph.
"""
    return _dedent(prompt)


# ---------------------------
# Endpoint
# ---------------------------

@router.post(
    "/visualize-dag",
    response_model=LlamaInput,
    summary="Wrap a DAG YAML (inside a LlamaOutput) into a LlamaInput that asks for a Mermaid DAG visualization"
)
async def visualize_dag(llama_output: LlamaOutput) -> LlamaInput:
    """
    Accepts a LlamaOutput whose 'response' field contains a Ubiquia DAG YAML document,
    and returns a LlamaInput instructing the LLM to convert that DAG into a Mermaid
    DAG visualization.
    """
    try:
        dag_yaml = _extract_dag_yaml(llama_output)
        if not dag_yaml.strip():
            raise ValueError("No YAML content found in LlamaOutput.response.")

        prompt = _build_mermaid_prompt(dag_yaml)

        # Use the same model identifier by default; you can swap this if you prefer a different one.
        return LlamaInput(
            model=llama_output.model,
            prompt=prompt,
            stream=False,
        )
    except Exception as e:
        raise HTTPException(
            status_code=400,
            detail=f"Failed to build Mermaid visualization LlamaInput from DAG: {e}",
        )

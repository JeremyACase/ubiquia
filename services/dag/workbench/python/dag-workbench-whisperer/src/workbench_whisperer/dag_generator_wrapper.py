from __future__ import annotations

import os
import re
import textwrap
from typing import Any, Dict

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

router = APIRouter(tags=["dag"])

# ---------------------------
# Models
# ---------------------------

class UserPrompt(BaseModel):
    """Natural-language workflow description from the Workbench UI."""
    userPrompt: str = Field(
        ...,
        max_length=2000,
        description="The natural-language prompt to send to the LLM."
    )


class LlamaInput(BaseModel):
    """Request body to send to Ollama /api/generate."""
    model: str = Field(..., description="The model identifier to invoke.")
    prompt: str = Field(..., description="Prompt text to send to the model.", max_length=300000)
    stream: bool = Field(default=False, description="Whether to stream output tokens.")


# ---------------------------
# Helpers
# ---------------------------

def _squish(text: str) -> str:
    """Collapse whitespace to single spaces."""
    return re.sub(r"\s+", " ", (text or "").strip())


def _dedent(text: str) -> str:
    """Remove indentation and trim."""
    return textwrap.dedent(text).strip()


def _resolve_model() -> str:
    """
    Decide which model to use with easy swapping via env:
      - WORKBENCH_LLM_FAMILY: 'llama3' (default) or 'gemma3'
      - WORKBENCH_LLM_MODEL : explicit model name override
    """
    override = os.getenv("WORKBENCH_LLM_MODEL")
    if override and override.strip():
        return override.strip()

    family = (os.getenv("WORKBENCH_LLM_FAMILY", "llama3") or "llama3").strip().lower()
    defaults = {"llama3": "llama3.2", "gemma3": "gemma3"}
    return defaults.get(family, defaults["llama3"])


DEFAULT_MODEL = _resolve_model()


# ---------------------------
# Prompt builder
# ---------------------------

def _build_dag_prompt(user_prompt: str) -> str:
    """
    Build a YAML-based DAG-generation instruction prompt from a natural-language user description.
    """
    example_yaml = _dedent("""
    name: example-graph
    modelType: Graph
    author: example
    description: Example DAG demonstrating expected YAML structure.
    version:
      major: 1
      minor: 0
      patch: 0
    tags: []
    capabilities: []
    domainDataContract:
      name: example-domain
      version:
        major: 1
        minor: 0
        patch: 0
    components:
      - name: Example-Component
        componentType: POD
        modelType: Component
        description: Example processing node
        port: 8080
        image:
          registry: example
          repository: example
          tag: latest
        overrideSettings: {}
    componentlessNodes:
      - modelType: Adapter
        nodeType: PUSH
        name: Example-Input-Adapter
        description: Example node
        endpoint: http://example:8080/input
        inputSubSchemas:
          - modelName: InputModel
        outputSubSchema:
          modelName: OutputModel
        nodeSettings:
          persistInputPayload: true
          persistOutputPayload: true
          validateInputPayload: true
          validateOutputPayload: true
          stimulateInputPayload: false
    edges:
      - leftAdapterName: Example-Input-Adapter
        rightAdapterNames: []
    """)

    contract = _dedent(f"""
    CONTRACT:
    - Output MUST be a single valid YAML document describing a Ubiquia DAG.
    - Do NOT output JSON.
    - Do NOT wrap in Markdown code fences.
    - Do NOT include any explanatory text.
    - The top-level keys must include:
        name, modelType, author, description, version, tags, capabilities,
        domainDataContract, components, componentlessNodes, edges.
    - modelType MUST be "Graph".
    - domainDataContract MUST contain a name and version (major/minor/patch).
    - Every node in componentlessNodes must be referenced in at least one edge.
    - Every edge.leftAdapterName and rightAdapterNames[] must reference defined nodes.
    - Do not include unrecognized keys like nodes, graph, pipeline, or scripts.
    - The YAML must parse cleanly.
    """)

    prompt = f"""
You are a Ubiquia DAG architect.

TASK:
Given the user's workflow description, design a valid, complete DAG in Ubiquia YAML format
that would orchestrate components and nodes to accomplish the workflow.

USER DESCRIPTION:
{_squish(user_prompt)}

OUTPUT RULES:
- Emit ONLY the YAML DAG (no markdown, no commentary, no backticks).
- The DAG must be executable in principle and connect components logically.
- Follow the contract below.

{contract}

REFERENCE YAML EXAMPLE (shape only; DO NOT COPY values):
{example_yaml}

Return ONLY the YAML DAG.
"""
    return _dedent(prompt)


# ---------------------------
# Endpoint
# ---------------------------

@router.post(
    "/generate-dag-input",
    response_model=LlamaInput,
    summary="Wrap a natural-language user prompt into a YAML DAG-generation LlamaInput"
)
async def wrap_dag(user: UserPrompt) -> LlamaInput:
    """
    Accepts a UserPrompt (free-form workflow description) and returns a LlamaInput
    that instructs the model to generate a YAML-formatted Ubiquia DAG.
    """
    try:
        if not user.userPrompt.strip():
            raise ValueError("userPrompt cannot be empty.")
        prompt = _build_dag_prompt(user.userPrompt)
        return LlamaInput(model=DEFAULT_MODEL, prompt=prompt, stream=False)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to build DAG LlamaInput: {e}")

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


class PromptRequest(BaseModel):
    userPrompt: str = Field(
        ..., max_length=2000, description="Natural-language request from the user."
    )


class LlamaInput(BaseModel):
    model: str
    stream: str
    prompt: str


app = FastAPI(title="ollama-whisperer", version="0.1.0")


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.post("/wrap", response_model=LlamaInput)
async def wrap_prompt(req: PromptRequest) -> LlamaInput:
    if not req.userPrompt or not req.userPrompt.strip():
        raise HTTPException(status_code=400, detail="userPrompt must be non-empty")

    # Construct the fixed structure
    return LlamaInput(
        model="llama3.2",
        stream="false",
        prompt=req.userPrompt.strip(),
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("ollama_whisperer.main:app", host="0.0.0.0", port=8080, reload=True)

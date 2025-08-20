FROM python:3.11-slim AS base

RUN pip install --no-cache-dir uv && \
    useradd -m app

WORKDIR /app
COPY pyproject.toml .
COPY uv.lock* ./

RUN uv sync --no-dev

COPY src ./src
RUN uv pip install -e .

FROM python:3.11-slim AS runtime
ENV PATH="/root/.local/bin:${PATH}"
WORKDIR /app

COPY --from=base /root/.cache/uv /root/.cache/uv
COPY --from=base /root/.local /root/.local
COPY --from=base /app /app

EXPOSE 8080
USER root
CMD ["uv", "run", "uvicorn", "ollama_whisperer.main:app", "--host", "0.0.0.0", "--port", "8080"]

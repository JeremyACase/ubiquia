FROM python:3.11-slim AS base
RUN pip install --no-cache-dir uv
WORKDIR /app
COPY pyproject.toml ./
COPY src ./src
RUN uv sync --no-dev

FROM python:3.11-slim AS runtime
WORKDIR /app
COPY --from=base /app /app
ENV PATH="/app/.venv/bin:${PATH}"
ENTRYPOINT ["util-simulation-service"]

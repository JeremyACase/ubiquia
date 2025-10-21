# Build stage
FROM python:3.11-slim AS base
RUN pip install --no-cache-dir uv
WORKDIR /app

# If pyproject references README.md, copy it
COPY pyproject.toml README.md ./
COPY src ./src

# Create venv and install deps + the package
RUN uv sync --no-dev   # creates /app/.venv and installs uvicorn, fastapi, your pkg

# Runtime stage
FROM python:3.11-slim AS runtime
WORKDIR /app

# Bring app + its virtualenv
COPY --from=base /app /app

# Use the venv's binaries (uvicorn is here)
ENV PATH="/app/.venv/bin:${PATH}"

EXPOSE 8080
CMD ["uvicorn", "workbench_scaffold.main:app", "--host", "0.0.0.0", "--port", "8080"]

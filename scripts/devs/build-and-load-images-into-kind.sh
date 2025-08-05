#!/bin/bash

set -euo pipefail

# ----------- CONFIGURATION -------------
OPENJDK_VERSION="${OPENJDK_VERSION:-21}"             # Default version
KIND_CLUSTER_NAME="${KIND_CLUSTER_NAME:-ubiquia-agent-0}"   # Default KIND cluster name

echo "Using OPENJDK_VERSION=${OPENJDK_VERSION}"
echo "Using KIND_CLUSTER_NAME=${KIND_CLUSTER_NAME}"

# ----------- KIND CLUSTER --------------
if kind get clusters | grep -q "^${KIND_CLUSTER_NAME}$"; then
    echo "✅ KIND cluster '${KIND_CLUSTER_NAME}' already exists."
else
    echo "🔧 Creating KIND cluster '${KIND_CLUSTER_NAME}'..."
    kind create cluster --name "${KIND_CLUSTER_NAME}"
fi

# ----------- BUILD LOOP ----------------
find . -type f -name 'Dockerfile' | while read -r dockerfile; do
  dir=$(dirname "$dockerfile")
  short_name=$(basename "$dir" | tr '[:upper:]' '[:lower:]')
  image_name="ubiquia/${short_name}"

  echo "🔨 Building Docker image: $image_name from $dockerfile"
  docker build \
    --build-arg OPENJDK_VERSION="$OPENJDK_VERSION" \
    -t "$image_name:latest" \
    "$dir"

  echo "📦 Loading image into KIND: $image_name:latest"
  kind load docker-image "$image_name:latest" --name "$KIND_CLUSTER_NAME"
done

echo "✅ All Docker images built and loaded into KIND."

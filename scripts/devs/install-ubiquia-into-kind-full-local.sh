#!/bin/bash

# Author: Jeremy Case
# Email: JeremyCase@odysseyconsult.com
# Purpose: This script will take locally-built artifacts and containerize them, load the 
#          containers into KIND, and then install Ubiquia. This is often faster than
#          doing this via a Github-based devops pipeline when trying to iterate on local builds.


set -euo pipefail

# ----------- CONFIGURATION -------------
OPENJDK_VERSION="${OPENJDK_VERSION:-21}"             # Default version
KIND_CLUSTER_NAME="${KIND_CLUSTER_NAME:-ubiquia-agent-0}"   # Default KIND cluster name

echo "Using OPENJDK_VERSION=${OPENJDK_VERSION}"
echo "Using KIND_CLUSTER_NAME=${KIND_CLUSTER_NAME}"

# Check that 'kind' is available in the path
if ! command -v kind &> /dev/null; then
    echo "ERROR: 'kind' command not found. Please install KIND and ensure it's available in your PATH."
    exit 1
fi

# Check Helm and kubectl are also available
for cmd in helm kubectl; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "ERROR: '$cmd' command not found. Please install $cmd and ensure it's available in your PATH."
        exit 1
    fi
done

# ----------- KIND CLUSTER --------------
if kind get clusters | grep -q "^${KIND_CLUSTER_NAME}$"; then
    echo "✅ KIND cluster '${KIND_CLUSTER_NAME}' already exists."
else
    echo "🔧 Creating KIND cluster '${KIND_CLUSTER_NAME}'..."
    kind create cluster --name "${KIND_CLUSTER_NAME}"
fi

# Check if the namespace 'ubiquia' already exists
if kubectl get namespace ubiquia &> /dev/null; then
    echo "Namespace 'ubiquia' already exists."
else
    echo "Creating namespace 'ubiquia'..."
    kubectl create namespace ubiquia
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

echo Installing Ubiquia into KIND...

helm dependency update helm/

# Create a mount for KIND to mount into to store our generated belief state jars
docker exec ubiquia-agent-0-control-plane mkdir -p /mnt/data/belief-state-jars

# This is to ensure the flow-service can manipulate the Kubernetes cluster; it will likely be a different service account in prod
kubectl apply -f config/dev/ubiquia_dev_service_account.yaml -n ubiquia

# This is to ensure the belief state generator has a shared mount path between it and the belief states it will be deploying in KIND
kubectl apply -f config/dev/ubiquia_dev_kind_pv.yaml -n ubiquia

if helm status ubiquia -n ubiquia > /dev/null 2>&1; then
    echo "Helm release 'ubiquia' exists — upgrading..."
    helm upgrade ubiquia helm/ --values helm/configurations/dev/lightweight-dev.yaml -n ubiquia
else
    echo "Installing Helm release 'ubiquia'..."
    helm install ubiquia helm/ --values helm/configurations/dev/lightweight-dev.yaml -n ubiquia
fi

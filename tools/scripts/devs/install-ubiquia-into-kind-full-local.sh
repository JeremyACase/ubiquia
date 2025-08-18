#!/bin/bash
# Author: Jeremy Case
# Email: JeremyCase@odysseyconsult.com
# Purpose: Build local artifacts into containers, load into KIND, then install Ubiquia.

set -euo pipefail

# ----------- CONFIGURATION -------------
OPENJDK_VERSION="${OPENJDK_VERSION:-21}"                    # For Java service images
KIND_CLUSTER_NAME="${KIND_CLUSTER_NAME:-ubiquia-agent-0}"   # KIND cluster name
UI_BASE_PATH="${UI_BASE_PATH:-/}"                           # Angular base-href (/, /workbench, etc.)
WORKBENCH_PATH="services/dag/workbench/ts/dag-workbench-ui" # UI subproject root

echo "Using OPENJDK_VERSION=${OPENJDK_VERSION}"
echo "Using KIND_CLUSTER_NAME=${KIND_CLUSTER_NAME}"
echo "Using UI_BASE_PATH=${UI_BASE_PATH}"

# ----------- PREREQS -------------------
for cmd in kind helm kubectl docker; do
  command -v "$cmd" >/dev/null || { echo "ERROR: '$cmd' not found in PATH."; exit 1; }
done

# ----------- KIND CLUSTER --------------
if kind get clusters | grep -q "^${KIND_CLUSTER_NAME}$"; then
  echo "✅ KIND cluster '${KIND_CLUSTER_NAME}' already exists."
else
  echo "🔧 Creating KIND cluster '${KIND_CLUSTER_NAME}'..."
  kind create cluster --name "${KIND_CLUSTER_NAME}"
fi

# Namespace
kubectl get namespace ubiquia >/dev/null 2>&1 || kubectl create namespace ubiquia

# ----------- BUILD & LOAD --------------
# (No BuildKit)

# Find Dockerfiles (skip node_modules to keep it tidy)
mapfile -t DOCKERFILES < <(find . -type d -name node_modules -prune -o -type f -name 'Dockerfile' -print | sort)

for dockerfile in "${DOCKERFILES[@]}"; do
  dir=$(dirname "$dockerfile")
  short_name=$(basename "$dir" | tr '[:upper:]' '[:lower:]')
  image_name="ubiquia/${short_name}"

  echo "----"
  echo "🔨 Building Docker image: ${image_name} from ${dockerfile}"

  if [[ "$dir" == *"/${WORKBENCH_PATH}" ]]; then
    # Angular/NGINX image. Avoid MSYS path mangling: only pass APP_BASE_PATH when not "/"
    if [[ "${UI_BASE_PATH}" == "/" ]]; then
      docker build -t "${image_name}:latest" "$dir"
    else
      MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" \
      docker build --build-arg APP_BASE_PATH="${UI_BASE_PATH}" \
                   -t "${image_name}:latest" "$dir"
    fi
  else
    # Java services
    docker build --build-arg OPENJDK_VERSION="${OPENJDK_VERSION}" \
                 -t "${image_name}:latest" "$dir"
  fi

  echo "📦 Loading image into KIND: ${image_name}:latest"
  kind load docker-image "${image_name}:latest" --name "${KIND_CLUSTER_NAME}"
done

echo "✅ All Docker images built and loaded into KIND."

# ----------- INSTALL / UPGRADE ---------
echo "Installing Ubiquia into KIND..."

helm dependency update deploy/helm/

# Shared PV in KinD node for belief-state jars
docker exec "${KIND_CLUSTER_NAME}-control-plane" mkdir -p /mnt/data/belief-state-jars || true

# RBAC/service account for dev
kubectl apply -f deploy/config/dev/ubiquia_dev_service_account.yaml -n ubiquia
# PV/PVC for belief state generator
kubectl apply -f deploy/config/dev/ubiquia_dev_kind_pv.yaml -n ubiquia

if helm status ubiquia -n ubiquia >/dev/null 2>&1; then
  echo "Helm release 'ubiquia' exists — upgrading..."
  helm upgrade ubiquia deploy/helm/ \
    --values deploy/helm/configurations/dev/featherweight-dev.yaml \
    -n ubiquia
else
  echo "Installing Helm release 'ubiquia'..."
  helm install ubiquia deploy/helm/ \
    --values deploy/helm/configurations/dev/featherweight-dev.yaml \
    -n ubiquia
fi

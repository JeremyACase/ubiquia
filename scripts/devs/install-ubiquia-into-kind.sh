#!/bin/bash

# Author: Jeremy Case
# Email: JeremyCase@odysseyconsult.com
# Purpose: This script will allow devs to install a local Ubiquia instance in Kubernetes-IN-Docker KIND. 
#          It assumes that KIND is installed and available globally via the command line, 
#          and that Helm, KubeCTL, and

echo Installing Ubiquia into KIND from scratch...

# Check that 'kind' is available in the path
if ! command -v kind &> /dev/null; then
    echo "ERROR: 'kind' command not found. Please install KIND and ensure it's available in your PATH."
    exit 1
fi

# Optional: Check Helm and kubectl are also available
for cmd in helm kubectl; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "ERROR: '$cmd' command not found. Please install $cmd and ensure it's available in your PATH."
        exit 1
    fi
done

helm dependency update helm/
helm dependency build helm/

if kind get clusters | grep -q "^ubiquia-agent-0$"; then
    echo "KIND cluster 'ubiquia-agent-0' already exists."
else
    kind create cluster --name ubiquia-agent-0
fi

kubectl create namespace ubiquia

# Create a mount for KIND to mount into
docker exec ubiquia-agent-0-control-plane mkdir -p /mnt/data/belief-state-jars

# This is to ensure the flow-service can manipulate the Kubernetes cluster; it will likely be a different service account in prod
kubectl apply -f config/dev/ubiquia_dev_service_account.yaml -n ubiquia

# This is to ensure the belief state generator has a shared mount path between it and the belief states it will be deploying in KIND
kubectl apply -f config/dev/ubiquia_dev_kind_pv.yaml -n ubiquia

if helm status ubiquia -n ubiquia > /dev/null 2>&1; then
    echo "Helm release 'ubiquia' exists — upgrading..."
    helm upgrade ubiquia helm/ --values helm/configurations/dev/featherweight-dev.yaml -n ubiquia
else
    echo "Installing Helm release 'ubiquia'..."
    helm install ubiquia helm/ --values helm/configurations/dev/featherweight-dev.yaml -n ubiquia
fi

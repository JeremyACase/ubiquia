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

kind create cluster -n ubiquia-agent-0

kubectl create namespace ubiquia

# This is to ensure the flow-service can manipulate the Kubernetes cluster; it will likely be a different service account in prod
kubectl apply -f config/dev/ubiquia-dev-service-account.yaml -n ubiquia

helm install ubiquia helm/ --values helm/configurations/dev/featherweight-dev.yaml -n ubiquia

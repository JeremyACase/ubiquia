#!/bin/bash

# Author: Jeremy Case
# Email: JeremyCase@odysseyconsult.com
# Purpose: This script will allow devs to install a local Ubiquia agent in KIND. It assumes that KIND is installed and available globally via the command line, and that Helm, KubeCTL, and

echo Installing Ubiquia into KIND from scratch...

helm dependency update ../../helm/
helm dependency build ../../helm/

kubectl create namespace ubiquia

# This is to ensure the flow-service can manipulate the Kubernetes cluster; it will likely be a different service account in prod
kubectl apply -f ../../config/dev/core/ubiquia-dev-service-account.yaml -n ubiquia

helm install ubiquia ../../helm/ --values ../../helm/valuesOverrides/dev/local-dev-values.yaml -n ubiquia

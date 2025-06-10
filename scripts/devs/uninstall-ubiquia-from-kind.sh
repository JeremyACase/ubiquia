#!/bin/bash

# Author: Jeremy Case
# Email: JeremyCase@odysseyconsult.com
# Purpose: This script will allow devs to install a local Ubiquia instance in Kubernetes-IN-Docker KIND. 
#          It assumes that KIND is installed and available globally via the command line, 
#          and that Helm, KubeCTL, and

echo Uninstalling Ubiquia from KIND cluster...

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

helm uninstall ubiquia -n ubiquia

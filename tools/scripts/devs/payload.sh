#!/usr/bin/env bash

# Endpoint URL
URL="http://localhost:8080/graph/ubiquia-workbench/adapter/workbench-user-prompt-adapter/push"

# JSON payload
read -r -d '' PAYLOAD <<EOF
{
  "userPrompt": "Help me build a workflow that enumerates the most lucrative real estate investment opportunities in Maui"
}
EOF

# POST request
curl -X POST "$URL" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD"

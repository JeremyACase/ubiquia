#!/bin/bash

# --- Configuration ---
FILE_PATH="./README.md"                         # First argument: path to the file to upload
ENDPOINT_URL="http://localhost:8080/ubiquia/belief-state-service/object/upload"  # Replace with your actual endpoint
FIELD_NAME="file"                     # Field name expected by the server (e.g., 'file' or 'upload')

# --- Validation ---
if [[ ! -f "$FILE_PATH" ]]; then
  echo "❌ File not found: $FILE_PATH"
  exit 1
fi

# --- Upload ---
curl -X POST "$ENDPOINT_URL" \
  -F "${FIELD_NAME}=@${FILE_PATH}" \
  -H "Accept: application/json"
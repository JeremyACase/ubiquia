#!/bin/bash

# Replace this with your actual proxy or belief-state endpoint
ENDPOINT="http://localhost:8080/ubiquia/belief-state-service/EOObservationFull/add"

# Number of observations to upload
COUNT=5

# Generate current ISO datetime with milliseconds
current_datetime() {
  date -u +"%Y-%m-%dT%H:%M:%S.%3NZ"
}

for i in $(seq 1 $COUNT); do
  echo "Uploading EOObservation #$i..."

  # Prepare payload
  payload=$(cat <<EOF
{
  "classificationMarking": "U",
  "obTime": "$(current_datetime)",
  "source": "Bluestaq",
  "dataMode": "TEST",
  "uct": false,
  "azimuth": 1.1,
  "elevation": 1.1,
  "range": 1.1,
  "corrQuality": 1.1,
  "satNo": 12345
}
EOF
)

  # Post it
  curl -s -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$payload"

  echo -e "\n---"
done

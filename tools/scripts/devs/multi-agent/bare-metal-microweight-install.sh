#!/bin/bash

# Author: Jeremy Case
# Purpose: Stand up N bare-metal Ubiquia flow services via Docker.
#          Each service gets a unique agent ID and shares the repo's ontologies.
#          Requires the service jar to be pre-built: ./gradlew build
#
# Usage: ./bare-metal-microweight-install.sh [--count N] [--build]
#   --count, -n   Number of flow services to start (default: 3)
#   --build, -b   Force rebuild of the Docker image before starting

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
SERVICE_DIR="$REPO_ROOT/services/core/java/core-flow-service"
JAR_PATH="$SERVICE_DIR/build/libs/ubiquia-core-flow-service.jar"
COMPOSE_DIR="$REPO_ROOT/deploy/compose"
ONTOLOGIES_DIR="$REPO_ROOT/deploy/helm/bootstrap/ontologies"
GRAPHS_DIR="$REPO_ROOT/deploy/helm/bootstrap/graphs"
IMAGE_NAME="ubiquia-core-flow-service"

AGENT_COUNT=3
BUILD=true

# --- Argument parsing ---
while [[ $# -gt 0 ]]; do
    case $1 in
        --count|-n)
            AGENT_COUNT="$2"
            shift 2
            ;;
        --build|-b)
            BUILD=true
            shift
            ;;
        --help|-h)
            sed -n '3,9p' "$0" | sed 's/^# //'
            exit 0
            ;;
        *)
            echo "ERROR: Unknown option: $1"
            echo "Run with --help for usage."
            exit 1
            ;;
    esac
done

# --- Dependency checks ---
if ! command -v docker &>/dev/null; then
    echo "ERROR: 'docker' not found. Please install it and ensure it's in your PATH."
    exit 1
fi

# --- Verify jar exists ---
if [[ ! -f "$JAR_PATH" ]]; then
    echo "ERROR: JAR not found at:"
    echo "  $JAR_PATH"
    echo ""
    echo "Build it first:"
    echo "  cd $SERVICE_DIR && ./gradlew build"
    exit 1
fi

echo "Using JAR: $JAR_PATH"

# --- Convert Git Bash /c/... paths to C:/... for Docker on Windows ---
to_docker_path() {
    echo "$1" | sed 's|^/\([a-zA-Z]\)/|\1:/|'
}

generate_uuid() {
    if command -v uuidgen &>/dev/null; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    elif [[ -f /proc/sys/kernel/random/uuid ]]; then
        cat /proc/sys/kernel/random/uuid
    else
        local b
        b=$(od -x /dev/urandom | head -1 | awk '{print $2$3"-"$4"-"$5"-"$6"-"$7$8$9}')
        echo "$b"
    fi
}

# --- Build image once ---
if [[ "$BUILD" == "true" ]]; then
    echo "Building image: $IMAGE_NAME..."
    docker build \
        --build-arg OPENJDK_VERSION=21 \
        -t "$IMAGE_NAME" \
        "$(to_docker_path "$SERVICE_DIR")"
fi

# --- Stop and remove any previously started containers from this script ---
for i in $(seq 1 "$AGENT_COUNT"); do
    NAME="flow-service-$i"
    if docker ps -a --format '{{.Names}}' | grep -q "^${NAME}$"; then
        echo "Removing existing container: $NAME"
        docker rm -f "$NAME" > /dev/null
    fi
done

# --- Launch containers ---
echo ""
echo "Starting $AGENT_COUNT flow service(s)..."
echo ""

D_COMPOSE_DIR=$(to_docker_path "$COMPOSE_DIR")
D_ONTOLOGIES_DIR=$(to_docker_path "$ONTOLOGIES_DIR")

GRAPHS_MOUNT=()
if [[ -d "$GRAPHS_DIR" ]]; then
    GRAPHS_MOUNT=(-v "$(to_docker_path "$GRAPHS_DIR"):/app/etc/graphs:ro")
fi

for i in $(seq 1 "$AGENT_COUNT"); do
    AGENT_ID=$(generate_uuid)
    NAME="flow-service-$i"

    docker run -d \
        --name "$NAME" \
        -p "8080" \
        -e "UBIQUIA_AGENT_ID=$AGENT_ID" \
        -v "$D_COMPOSE_DIR/config/application.yaml:/app/etc/application.yaml:ro" \
        -v "$D_ONTOLOGIES_DIR:/app/etc/domain-ontologies:ro" \
        "${GRAPHS_MOUNT[@]+"${GRAPHS_MOUNT[@]}"}" \
        "$IMAGE_NAME" > /dev/null

    echo "  $NAME  ->  agent ID: $AGENT_ID"
done

echo ""
echo "All containers started. To follow logs:"
echo "  docker logs -f flow-service-1"
echo ""
echo "To stop all:"
echo "  docker rm -f \$(docker ps -a --filter name=flow-service --format '{{.Names}}')"

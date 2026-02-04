#!/bin/bash

# Script to build and run the Cloud Kitchens test application in Docker
# Usage: ./run-docker.sh [options]

set -e

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Load environment variables from .env file
if [ -f "${PROJECT_ROOT}/.env" ]; then
    echo "Loading environment from .env file..."
    export $(grep -v '^#' "${PROJECT_ROOT}/.env" | xargs)
else
    echo "Warning: .env file not found at ${PROJECT_ROOT}/.env"
    echo "Please create one from .env.example"
    exit 1
fi

# Configuration
IMAGE_NAME="cloudkitchens-test"

# Validate required variables
if [ -z "${AUTH_TOKEN}" ]; then
    echo "Error: AUTH_TOKEN not set in .env file"
    exit 1
fi

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Cloud Kitchens Docker Runner${NC}"
echo -e "${BLUE}========================================${NC}"

# Build the Docker image
echo -e "\n${GREEN}Building Docker image...${NC}"
cd "${PROJECT_ROOT}"
docker build -t ${IMAGE_NAME} .

# Build command arguments from environment variables
CMD_ARGS="--auth ${AUTH_TOKEN}"

if [ -n "${ENDPOINT}" ]; then
    CMD_ARGS="${CMD_ARGS} --endpoint ${ENDPOINT}"
fi

if [ -n "${PROBLEM_NAME}" ]; then
    CMD_ARGS="${CMD_ARGS} --name ${PROBLEM_NAME}"
fi

if [ -n "${PROBLEM_SEED}" ]; then
    CMD_ARGS="${CMD_ARGS} --seed ${PROBLEM_SEED}"
fi

if [ -n "${RATE}" ]; then
    CMD_ARGS="${CMD_ARGS} --rate ${RATE}"
fi

if [ -n "${MIN}" ]; then
    CMD_ARGS="${CMD_ARGS} --min ${MIN}"
fi

if [ -n "${MAX}" ]; then
    CMD_ARGS="${CMD_ARGS} --max ${MAX}"
fi

# Run the container with the auth token
echo -e "\n${GREEN}Running container with configuration from .env...${NC}"
docker run --rm ${IMAGE_NAME} ${CMD_ARGS} "$@"

echo -e "\n${GREEN}Done!${NC}"

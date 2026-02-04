#!/bin/bash

# Script to run the Cloud Kitchens application locally
# Usage: ./run.sh [additional args]

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

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Cloud Kitchens Local Runner${NC}"
echo -e "${BLUE}========================================${NC}"

# Change to project root
cd "${PROJECT_ROOT}"

# Run with Gradle
echo -e "\n${GREEN}Starting application...${NC}"
./gradlew run "$@"

echo -e "\n${GREEN}Done!${NC}"

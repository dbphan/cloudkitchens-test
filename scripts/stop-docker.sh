#!/bin/bash

# Script to stop and clean up Cloud Kitchens Docker containers and images
# Usage: ./stop-docker.sh [--all]

set -e

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Configuration
IMAGE_NAME="cloudkitchens-test"

# Colors for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Cloud Kitchens Docker Cleanup${NC}"
echo -e "${BLUE}========================================${NC}"

# Stop running containers
echo -e "\n${YELLOW}Checking for running containers...${NC}"
RUNNING_CONTAINERS=$(docker ps -q --filter ancestor=${IMAGE_NAME})

if [ -n "$RUNNING_CONTAINERS" ]; then
    echo -e "${YELLOW}Stopping running containers...${NC}"
    docker stop ${RUNNING_CONTAINERS}
    echo -e "${GREEN}Containers stopped.${NC}"
else
    echo -e "${GREEN}No running containers found.${NC}"
fi

# Remove stopped containers
echo -e "\n${YELLOW}Checking for stopped containers...${NC}"
STOPPED_CONTAINERS=$(docker ps -aq --filter ancestor=${IMAGE_NAME})

if [ -n "$STOPPED_CONTAINERS" ]; then
    echo -e "${YELLOW}Removing stopped containers...${NC}"
    docker rm ${STOPPED_CONTAINERS}
    echo -e "${GREEN}Containers removed.${NC}"
else
    echo -e "${GREEN}No stopped containers found.${NC}"
fi

# Remove image if --all flag is provided
if [ "$1" == "--all" ]; then
    echo -e "\n${YELLOW}Removing Docker image...${NC}"
    if docker images -q ${IMAGE_NAME} | grep -q .; then
        docker rmi ${IMAGE_NAME}
        echo -e "${GREEN}Image removed.${NC}"
    else
        echo -e "${GREEN}No image found.${NC}"
    fi
fi

echo -e "\n${GREEN}Cleanup complete!${NC}"

# Show remaining images
echo -e "\n${BLUE}Remaining ${IMAGE_NAME} images:${NC}"
docker images ${IMAGE_NAME} || echo "None"

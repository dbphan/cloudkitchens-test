#!/bin/bash

# Script to stop running Cloud Kitchens application
# Usage: ./stop.sh

set -e

# Colors for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Cloud Kitchens Stop${NC}"
echo -e "${BLUE}========================================${NC}"

# Find and kill Gradle daemon processes
echo -e "\n${YELLOW}Checking for Gradle processes...${NC}"
GRADLE_PIDS=$(pgrep -f "GradleDaemon" || true)

if [ -n "$GRADLE_PIDS" ]; then
    echo -e "${YELLOW}Stopping Gradle daemon...${NC}"
    ./gradlew --stop
    echo -e "${GREEN}Gradle daemon stopped.${NC}"
else
    echo -e "${GREEN}No Gradle daemon running.${NC}"
fi

# Find and kill any running application processes
echo -e "\n${YELLOW}Checking for application processes...${NC}"
APP_PIDS=$(pgrep -f "com.css.challenge.MainKt" || true)

if [ -n "$APP_PIDS" ]; then
    echo -e "${YELLOW}Stopping application processes...${NC}"
    echo "$APP_PIDS" | xargs kill -9 2>/dev/null || true
    echo -e "${GREEN}Application processes stopped.${NC}"
else
    echo -e "${GREEN}No application processes running.${NC}"
fi

echo -e "\n${GREEN}Cleanup complete!${NC}"

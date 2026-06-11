#!/bin/bash

# ZeroDayMMO Server Startup Script

# Load environment variables
if [ -f "/root/ZeroDayMMO/config/.env" ]; then
    export $(grep -v '^#' /root/ZeroDayMMO/config/.env | xargs)
fi

# Create data directory if it doesn't exist
mkdir -p /root/ZeroDayMMO/data

# Start the server
cd /root/ZeroDayMMO/zeroday-server
./gradlew run
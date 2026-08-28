#!/bin/bash

# Source the environment variables
source "$(dirname "$0")/initEnv.sh"

# Start the Spring Boot application in debug mode (background)
cd "$(dirname "$0")"

# Open browser after a delay to allow app startup
(sleep 10 && open "http://localhost:9898") &

mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

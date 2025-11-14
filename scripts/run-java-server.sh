#!/bin/bash

# This script runs the Java EDS server with IPv4 preference

# Kill any existing Java processes using port 18000
PID=$(lsof -t -i:18000 2>/dev/null)
if [ ! -z "$PID" ]; then
  echo "Killing existing process on port 18000..."
  kill $PID
  sleep 1
fi

# Run the Java server with IPv4 preference
echo "Starting Java EDS server with IPv4 preference..."
java -Djava.net.preferIPv4Stack=true -jar target/envoyjavaedsserver-1.0-SNAPSHOT-jar-with-dependencies.jar

# Note: If the above doesn't work, try this alternative
# java -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -jar target/envoyjavaedsserver-1.0-SNAPSHOT-jar-with-dependencies.jar
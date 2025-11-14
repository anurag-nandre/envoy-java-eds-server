#!/bin/bash

# This script runs Envoy in a Docker container connected to the EDS server

# Get the host's IP address - needed for the EDS server to be reachable from Docker
# This works on Mac and Linux, might need adjustment for other platforms
#HOST_IP=$(ifconfig | grep -E "([0-9]{1,3}\.){3}[0-9]{1,3}" | grep -v 127.0.0.1 | awk '{ print $2 }' | cut -f2 -d: | head -n1)
#
#echo "Host IP appears to be: $HOST_IP"
#echo "If this is incorrect, please edit the script to set the correct IP address"

# Run Envoy with the configuration
docker run --rm \
  -v $(pwd)/envoy-config.yaml:/etc/envoy/envoy.yaml \
  -p 10000:10000 \
  -p 9901:9901 \
  --network="bridge" \
  --sysctl net.ipv6.conf.all.disable_ipv6=1 \
  --add-host=host.docker.internal:host-gateway \
  envoyproxy/envoy:v1.26-latest \
  -c /etc/envoy/envoy.yaml -l debug
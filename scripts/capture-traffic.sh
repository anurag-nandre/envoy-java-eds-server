#!/bin/bash

# This script captures the gRPC traffic between Envoy and the EDS server

# Set interface (update this if needed)
INTERFACE="lo0"  # loopback interface on macOS, use "lo" on Linux

# Set port (EDS server port)
PORT=18000

# Create a directory for the capture files
mkdir -p captures

# Generate timestamp for the capture file
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
CAPTURE_FILE="captures/eds-envoy-${TIMESTAMP}.pcap"

echo "Capturing traffic on port $PORT to file $CAPTURE_FILE"
echo "Press Ctrl+C to stop capturing"

# Use sudo as packet capture typically requires root privileges
tcpdump -i $INTERFACE -s 0 -w $CAPTURE_FILE "tcp port $PORT"

echo "Capture saved to $CAPTURE_FILE"
echo ""
echo "To analyze the capture with Wireshark, run:"
echo "wireshark $CAPTURE_FILE"
# Testing the Envoy EDS Integration

This document explains how to run and test the Envoy EDS (Endpoint Discovery Service) integration with the Java EDS server.

## Prerequisites

- Docker installed for running Envoy
- Java 17 or higher for running the EDS server
- Maven for building the project
- Wireshark (optional, for analyzing the captured traffic)
- tcpdump (for capturing network traffic)

## Steps to Run and Test

### 1. Build and Run the EDS Server

First, build and run the Java EDS server:

```bash
# Build the project
mvn clean package

# Run the EDS server (use the jar-with-dependencies version)
java -jar target/envoyjavaedsserver-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The server will start on port 18000 by default and register some example endpoints for the "example-cluster" and "other-service" clusters.

### 2. Start Capturing Network Traffic

In a new terminal window, run the capture script:

```bash
./capture-traffic.sh
```

This will start capturing TCP traffic on port 18000 (the EDS server port). The capture will continue until you press Ctrl+C.

### 3. Run Envoy with Docker

In another terminal window, run the docker script to start Envoy:

```bash
./docker-run.sh
```

This will start Envoy in a Docker container with the configuration that points to your EDS server.

### 4. Test the Setup

Once everything is running, you can test that the setup works by:

1. Checking the Envoy admin interface at http://localhost:9901
2. Looking at the EDS server logs to see the requests from Envoy
3. Sending a request to the Envoy proxy at http://localhost:10000 (should route to services in example_cluster)

### 5. Modify Endpoints to See Updates

To see the dynamic update process in action, use the EDS server's API to add or remove endpoints:

```java
// This would be implemented as a REST API or JMX operation in a real-world scenario
// For now, you can modify the endpoints programmatically
EndpointDiscoveryServiceImpl eds = server.getEndpointDiscoveryService();
eds.addEndpoint("example-cluster", "10.0.0.3", 9090);
eds.removeEndpoint("example-cluster", "10.0.0.1", 9090);
```

Watch the EDS server logs to see the updates being pushed to Envoy.

### 6. Stop Capturing and Analyze Traffic

1. Press Ctrl+C in the terminal where tcpdump is running to stop capturing
2. Analyze the captured traffic with Wireshark:

```bash
wireshark captures/eds-envoy-[timestamp].pcap
```

## Understanding the xDS Protocol Flow

When analyzing the captured traffic, look for these key interactions:

1. **Initial Discovery Request**: Envoy sends a `DiscoveryRequest` for endpoint information
   - Look for protobuf messages with resource names (cluster names)

2. **Discovery Response**: The EDS server responds with a `DiscoveryResponse`
   - Contains `ClusterLoadAssignment` resources with endpoint information
   - Includes version identifier and nonce

3. **Acknowledgment**: Envoy sends another `DiscoveryRequest` with the version and nonce
   - This acknowledges the successful application of the configuration

4. **Push Updates**: When endpoints change, the server pushes updates
   - Server sends a new `DiscoveryResponse` with an incremented version number
   - Envoy acknowledges with a new `DiscoveryRequest`

## Filtering in Wireshark

To better analyze the gRPC messages in Wireshark:

1. Filter by port: `tcp.port == 18000`
2. Look for HTTP2 frames: `http2`
3. If you have the protobuf definitions loaded, you can filter by specific message types

## Common Issues and Troubleshooting

1. **Connection Refused**: Ensure the EDS server is running before starting Envoy
2. **No Endpoints Available**: Check the EDS server logs to ensure endpoints are registered
3. **Docker Connectivity**: If Envoy can't reach the EDS server, ensure the `host.docker.internal` hostname is resolving correctly

## Next Steps

1. Implement a REST API to manage endpoints
2. Add metrics to track the number of discovery requests and responses
3. Implement delta xDS for more efficient updates
4. Add support for other xDS services (LDS, RDS, CDS)




grpcurl -protoset envoy_full.proto -d @ <req.json -plaintext 127.0.0.1:9996 envoy.service.endpoint.v3.EndpointDiscoveryService.StreamEndpoints
{
"versionInfo": "v0",
"resources": [
{
"@type": "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment",
"clusterName": "envoy-eds-info:/v1/anurag-test-role-1/anurag-traffic-split-abba?realm=dev-us1\u0026stack=default\u0026middleware_port=17001\u0026healthcheck_port=17006"
}
],
"typeUrl": "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment",
"nonce": "n0"
}


grpcurl -protoset envoy_full.proto -d @ <req.json -plaintext 127.0.0.1:9996 envoy.service.endpoint.v3.EndpointDiscoveryService.StreamEndpoints
{
"versionInfo": "v0",
"resources": [
{
"@type": "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment",
"clusterName": "envoy-eds-info:/v1/anurag-test-role-1/anurag-traffic-split-abba?realm=dev-us1&stack=default&middleware_port=17001&healthcheck_port=17006",
"endpoints": [
{
"locality": {
"region": "us-east-1",
"zone": "use1-az6"
},
"lbEndpoints": [
{
"endpoint": {
"address": {
"socketAddress": {
"address": "10.213.150.19",
"portValue": 17001
}
},
"healthCheckConfig": {
"portValue": 17006
},
"hostname": "anurag-traffic-split-abba.dev-us1.svc.twilio.com"
},
"metadata": {
"filterMetadata": {
"envoy.lb": {
"canary": "0",
"realm": "dev-us1",
"stack": "default"
}
}
}
}
]
},
{
"locality": {
"region": "us-east-1",
"zone": "use1-az4"
},
"lbEndpoints": [
{
"endpoint": {
"address": {
"socketAddress": {
"address": "10.211.241.234",
"portValue": 17001
}
},
"healthCheckConfig": {
"portValue": 17006
},
"hostname": "anurag-traffic-split-abba.dev-us1.svc.twilio.com"
},
"metadata": {
"filterMetadata": {
"envoy.lb": {
"canary": "0",
"realm": "dev-us1",
"stack": "default"
}
}
}
}
]
}
]
}
],
"typeUrl": "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment",
"nonce": "n0"
}
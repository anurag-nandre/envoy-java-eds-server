# Envoy Java EDS Server

A simple Java implementation of Envoy's xDS protocol, specifically focusing on the Endpoint Discovery Service (EDS).

## Overview

This project provides a basic implementation of Envoy's xDS (the collective name for Envoy's discovery services) protocol, focusing on the Endpoint Discovery Service (EDS). The implementation demonstrates how to create a control plane for Envoy that can dynamically update the endpoints that Envoy proxies traffic to.

## xDS Protocol Flow

### What is xDS?

xDS is the collective term for Envoy's set of discovery services:
- **LDS**: Listener Discovery Service
- **RDS**: Route Discovery Service
- **CDS**: Cluster Discovery Service
- **EDS**: Endpoint Discovery Service
- **SDS**: Secret Discovery Service

Each service allows dynamic configuration of different aspects of Envoy's proxy behavior.

### EDS (Endpoint Discovery Service) Flow

This project focuses on the Endpoint Discovery Service (EDS), which allows dynamic discovery of upstream hosts in a cluster. Here's how the EDS protocol works:

1. **Initial Connection**:
   - Envoy connects to the EDS server via gRPC
   - Envoy sends a `DiscoveryRequest` for endpoint information
   - The request includes resource names (cluster names) that Envoy needs information for

2. **Server Response**:
   - The EDS server responds with a `DiscoveryResponse` containing `ClusterLoadAssignment` resources
   - Each `ClusterLoadAssignment` provides information about the endpoints for a specific cluster
   - The response includes a version identifier and a nonce

3. **Acknowledgment**:
   - Envoy processes the response and applies the configuration
   - Envoy sends another `DiscoveryRequest` with the version and nonce from the response
   - This serves as an acknowledgment (ACK) that the configuration was successfully applied

4. **Error Handling**:
   - If Envoy encounters an error processing the response, it sends a `DiscoveryRequest` with an error detail
   - This serves as a negative acknowledgment (NACK)
   - The server should address the error and send a corrected response

5. **Push Updates**:
   - The server can push updates to Envoy at any time by sending a new `DiscoveryResponse`
   - Each update must have a new version identifier
   - Envoy will ACK or NACK the update as described above

### Communication Modes

The xDS protocol supports three communication modes:

1. **State of the World (SotW)**:
   - The server sends the complete state in each response
   - This is simpler but less efficient for large configurations
   - This is the mode implemented in this example project

2. **Delta**:
   - The server only sends changes (additions, modifications, removals)
   - More efficient for large configurations with frequent small changes
   - Requires more complex client and server logic

3. **Incremental**:
   - Similar to Delta but with a simplified protocol
   - Provides a middle ground between SotW and Delta

## Implementation Details

This project implements a simple EDS server that:

1. Starts a gRPC server that implements the Endpoint Discovery Service API
2. Handles `streamEndpoints` for long-lived connections
3. Supports dynamic addition and removal of endpoints
4. Pushes updates to connected clients when endpoints change
5. Demonstrates basic error handling for NACK responses

## Usage

### Running the Server

```bash
mvn clean package
java -jar target/envoyjavaedsserver-1.0-SNAPSHOT-jar-with-dependencies.jar [port]
```

### Adding/Removing Endpoints Programmatically

```java
// Get the EDS implementation from the server
EndpointDiscoveryServiceImpl eds = server.getEndpointDiscoveryService();

// Add endpoints
eds.addEndpoint("my-cluster", "10.0.0.1", 9090);
eds.addEndpoint("my-cluster", "10.0.0.2", 9090);

// Remove an endpoint
eds.removeEndpoint("my-cluster", "10.0.0.1", 9090);
```

### Connecting with Envoy

To configure Envoy to connect to this EDS server, use a configuration like:

```yaml
dynamic_resources:
  eds_config:
    resource_api_version: V3
    api_config_source:
      api_type: GRPC
      transport_api_version: V3
      grpc_services:
        - envoy_grpc:
            cluster_name: eds_cluster
static_resources:
  clusters:
    - name: eds_cluster
      connect_timeout: 0.25s
      type: STATIC
      lb_policy: ROUND_ROBIN
      http2_protocol_options: {}
      load_assignment:
        cluster_name: eds_cluster
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: 127.0.0.1
                      port_value: 18000
    - name: example-cluster
      connect_timeout: 0.25s
      type: EDS
      lb_policy: ROUND_ROBIN
      eds_cluster_config:
        eds_config:
          resource_api_version: V3
          api_config_source:
            api_type: GRPC
            transport_api_version: V3
            grpc_services:
              - envoy_grpc:
                  cluster_name: eds_cluster
```

## Further Extensions

To expand this project:

1. Implement the other xDS services (LDS, RDS, CDS, SDS)
2. Add persistent storage for endpoint data
3. Implement Delta xDS for more efficient updates
4. Add a REST API for managing endpoints
5. Add metrics and monitoring
6. Implement more sophisticated load balancing configurations

## References

- [Envoy xDS API Overview](https://www.envoyproxy.io/docs/envoy/latest/api-docs/xds_protocol)
- [Data Plane API](https://github.com/envoyproxy/data-plane-api)
- [Envoy Control Plane for Java](https://github.com/envoyproxy/java-control-plane)

grpcurl -protoset envoy_full.proto -d @ <req.json -plaintext 127.0.0.1:18000 envoy.service.endpoint.v3.EndpointDiscoveryService.StreamEndpoints
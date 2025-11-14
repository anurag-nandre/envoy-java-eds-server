package com.twilio.envoy.grpc.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a service endpoint in the Envoy service mesh.
 * <p>
 * This class contains information about a single endpoint (host, port, and optional metadata)
 * that can be registered with Envoy's Endpoint Discovery Service (EDS).
 */
public class ServiceEndpoint {
    private final String host;
    private final int port;
    private final Map<String, String> metadata;
    
    /**
     * Create a service endpoint with the specified host and port.
     *
     * @param host The endpoint hostname or IP address
     * @param port The endpoint port
     */
    public ServiceEndpoint(String host, int port) {
        this(host, port, new HashMap<>());
    }
    
    /**
     * Create a service endpoint with the specified host, port, and metadata.
     *
     * @param host     The endpoint hostname or IP address
     * @param port     The endpoint port
     * @param metadata Additional metadata associated with this endpoint
     */
    public ServiceEndpoint(String host, int port, Map<String, String> metadata) {
        this.host = host;
        this.port = port;
        this.metadata = metadata;
    }
    
    /**
     * Get the endpoint's hostname or IP address.
     *
     * @return The hostname or IP address
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Get the endpoint's port.
     *
     * @return The port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Get the endpoint's metadata.
     *
     * @return The metadata as a map
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "ServiceEndpoint{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", metadata=" + metadata +
                '}';
    }
}
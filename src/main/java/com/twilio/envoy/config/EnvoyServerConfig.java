package com.twilio.envoy.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.Min;

/**
 * Configuration for the Envoy Control Plane server.
 * <p>
 * This configuration extends the standard Dropwizard Configuration and adds
 * specific properties for the Envoy gRPC server.
 */
public class EnvoyServerConfig extends Configuration {
    
    // Dropwizard handles its own server configuration via server: block in config.yml
    private int grpcPort = 18000;

    /**
     * Creates a new EnvoyServerConfig with the specified gRPC port.
     *
     * @param grpcPort The port for the gRPC server to listen on
     */
    @JsonCreator
    public EnvoyServerConfig(@JsonProperty("grpcPort") int grpcPort) {
        this.grpcPort = grpcPort;
    }

    /**
     * Get the gRPC server port.
     *
     * @return The gRPC server port
     */
    @Min(1024)
    public int getGrpcPort() {
        return grpcPort;
    }

    /**
     * Set the gRPC server port.
     *
     * @param port The gRPC server port
     */
    public void setGrpcPort(int port) {
        this.grpcPort = port;
    }
}
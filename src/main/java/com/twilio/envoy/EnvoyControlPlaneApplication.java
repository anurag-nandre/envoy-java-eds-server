package com.twilio.envoy;

import com.twilio.envoy.config.EnvoyServerConfig;
import com.twilio.envoy.grpc.EnvoyGrpcServer;
import com.twilio.envoy.grpc.discovery.EndpointDiscoveryService;
import com.twilio.envoy.health.GrpcServerHealthCheck;
import com.twilio.envoy.views.EnvoyControlPlaneView;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for the Envoy Control Plane server.
 * <p>
 * This application integrates a gRPC server implementing Envoy's xDS protocol
 * with a Dropwizard web server for administration and metrics.
 */
public class EnvoyControlPlaneApplication extends Application<EnvoyServerConfig> {

    private static final Logger logger = LoggerFactory.getLogger(EnvoyControlPlaneApplication.class);
    private static final String DEFAULT_HOST = "0.0.0.0";

    private EnvoyServerConfig config;
    private EnvoyGrpcServer grpcServer;
    private EnvoyControlPlaneView envoyControlPlaneView;

    /**
     * Application entry point.
     *
     * @param args Command line arguments
     * @throws Exception If an error occurs during startup
     */
    public static void main(String[] args) throws Exception {
        new EnvoyControlPlaneApplication().run("server", "config.yml");
    }

    @Override
    public String getName() {
        return "envoy-control-plane";
    }

    @Override
    public void run(EnvoyServerConfig config, Environment environment) {
        try {
            this.config = config;
            runInternal(environment);
        } catch (Exception ex) {
            logger.error("Exception during application startup: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to start application", ex);
        }
    }

    private void runInternal(final Environment environment) throws Exception {
        logger.info("Starting Envoy Control Plane server...");


        // Create and configure the Envoy gRPC server
        final EndpointDiscoveryService discoveryService = new EndpointDiscoveryService();
        this.grpcServer = new EnvoyGrpcServer(
                DEFAULT_HOST,
                config.getGrpcPort(),
                discoveryService
        );

        this.envoyControlPlaneView = new EnvoyControlPlaneView();
        logger.info("gRPC server registered with Dropwizard (port: {})", config.getGrpcPort());

        // Add example endpoints
        setupExampleEndpoints(discoveryService);

        setupEnvironment(environment);
    }

    private void setupEnvironment(Environment environment) {
        environment
                .jersey()
                .register(envoyControlPlaneView);

        environment.lifecycle().manage(grpcServer);
        environment.healthChecks().register("grpcServer", new GrpcServerHealthCheck());
    }

    private void setupExampleEndpoints(EndpointDiscoveryService discoveryService) {
        // Add some example endpoints to demonstrate functionality
        discoveryService.addEndpoint("example-cluster", "10.0.0.1", 9090);
        discoveryService.addEndpoint("example-cluster", "10.0.0.2", 9090);
        discoveryService.addEndpoint("example-cluster", "10.0.0.3", 9090);
        discoveryService.addEndpoint("example-cluster", "10.0.0.4", 9090);
        discoveryService.addEndpoint("example-cluster", "10.0.0.5", 9090);
        discoveryService.addEndpoint("example-cluster", "10.0.0.6", 9090);
        discoveryService.addEndpoint("example-cluster", "10.0.0.7", 9090);
        discoveryService.addEndpoint("other-service", "10.1.1.1", 8080);
        discoveryService.addEndpoint("other-service1", "10.1.1.2", 8080);
        discoveryService.addEndpoint("other-service2", "10.1.1.3", 8080);

        logger.info("Added example endpoints to clusters: example-cluster, other-service");
    }
}
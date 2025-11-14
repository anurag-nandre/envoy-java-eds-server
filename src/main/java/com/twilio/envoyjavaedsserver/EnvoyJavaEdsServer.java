package com.twilio.envoyjavaedsserver;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

/**
 * A simple Envoy Discovery Service (EDS) server implementation.
 * <p>
 * This server implements the xDS protocol to provide endpoint discovery capabilities
 * for Envoy proxies. It supports dynamic endpoint registration and updates.
 */
public class EnvoyJavaEdsServer {
    private static final Logger logger = LoggerFactory.getLogger(EnvoyJavaEdsServer.class);
    private static final int DEFAULT_PORT = 18000;
    private static final String DEFAULT_HOST = "0.0.0.0";
    
    private final int port;
    private final String host;
    private final EndpointDiscoveryServiceImpl edsImpl;
    private Server server;

    /**
     * Create a server with default settings.
     */
    public EnvoyJavaEdsServer() {
        this(DEFAULT_PORT);
    }
    
    /**
     * Create a server listening on the specified port.
     *
     * @param port The port to listen on
     */
    public EnvoyJavaEdsServer(int port) {
        this(DEFAULT_HOST, port);
    }
    
    /**
     * Create a server listening on the specified host and port.
     *
     * @param host The host address to bind to
     * @param port The port to listen on
     */
    public EnvoyJavaEdsServer(String host, int port) {
        this(host, port, new EndpointDiscoveryServiceImpl());
    }
    
    /**
     * Create a server with the specified host, port and service implementation.
     *
     * @param host    The host address to bind to
     * @param port    The port to listen on
     * @param edsImpl The endpoint discovery service implementation
     */
    public EnvoyJavaEdsServer(String host, int port, EndpointDiscoveryServiceImpl edsImpl) {
        this.host = host;
        this.port = port;
        this.edsImpl = edsImpl;
    }
    





    public static void main(String[] args) throws IOException, InterruptedException {
        int port = DEFAULT_PORT;
        
        // Parse command line arguments for port if provided
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("Invalid port specified, using default: " + DEFAULT_PORT);
            }
        }
        
        final EnvoyJavaEdsServer server = new EnvoyJavaEdsServer(DEFAULT_HOST, port);
        server.start();
        
        // Register some example endpoints
        EndpointDiscoveryServiceImpl edsImpl = server.getEndpointDiscoveryService();
        edsImpl.addEndpoint("example-cluster", "10.0.0.1", 9090);
        edsImpl.addEndpoint("example-cluster", "10.0.0.2", 9090);
        edsImpl.addEndpoint("example-cluster", "10.0.0.3", 9090);
        edsImpl.addEndpoint("example-cluster", "10.0.0.4", 9090);
        edsImpl.addEndpoint("example-cluster", "10.0.0.5", 9090);
        edsImpl.addEndpoint("example-cluster", "10.0.0.6", 9090);
        edsImpl.addEndpoint("example-cluster", "10.0.0.7", 9090);
        edsImpl.addEndpoint("other-service", "10.1.1.1", 8080);
        edsImpl.addEndpoint("other-service1", "10.1.1.2", 8080);
        edsImpl.addEndpoint("other-service2", "10.1.1.3", 8080);
        
        logger.info("Added example endpoints to clusters: example-cluster, other-service");
        logger.info("Server ready to accept requests");
        
        server.blockUntilShutdown();
    }

    /**
     * Get the endpoint discovery service implementation.
     *
     * @return The endpoint discovery service implementation
     */
    public EndpointDiscoveryServiceImpl getEndpointDiscoveryService() {
        return edsImpl;
    }
    
    /**
     * Start the server.
     *
     * @throws IOException If the server cannot be started
     */
    private void start() throws IOException {
        server = NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
                .addService(edsImpl)
                .executor(Executors.newFixedThreadPool(10))
                .build()
                .start();
                
        logger.info("EDS Server started, listening on {}:{}", host, port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutting down EDS server due to JVM shutdown");
                try {
                    EnvoyJavaEdsServer.this.stop();
                } catch (InterruptedException e) {
                    logger.error("Error during server shutdown", e);
                }
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}

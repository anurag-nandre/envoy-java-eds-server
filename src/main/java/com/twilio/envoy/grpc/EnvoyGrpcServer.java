package com.twilio.envoy.grpc;

import com.twilio.envoy.grpc.discovery.EndpointDiscoveryService;
import io.dropwizard.lifecycle.Managed;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A gRPC server that implements Envoy's xDS (discovery services) protocol.
 * <p>
 * This server specifically implements the Endpoint Discovery Service (EDS) for Envoy proxies,
 * allowing dynamic service endpoint registration and updates. The server integrates with
 * Dropwizard's lifecycle management for controlled startup and shutdown.
 */
public class EnvoyGrpcServer implements Managed {
    private static final Logger logger = LoggerFactory.getLogger(EnvoyGrpcServer.class);
    private static final int DEFAULT_PORT = 18000;
    private static final String DEFAULT_HOST = "0.0.0.0";
    
    private final int port;
    private final String host;
    private final EndpointDiscoveryService discoveryService;
    private Server server;

    /**
     * Create a server with default settings.
     */
    public EnvoyGrpcServer() {
        this(DEFAULT_PORT);
    }
    
    /**
     * Create a server listening on the specified port.
     *
     * @param port The port to listen on
     */
    public EnvoyGrpcServer(int port) {
        this(DEFAULT_HOST, port);
    }
    
    /**
     * Create a server listening on the specified host and port.
     *
     * @param host The host address to bind to
     * @param port The port to listen on
     */
    public EnvoyGrpcServer(String host, int port) {
        this(host, port, new EndpointDiscoveryService());
    }
    
    /**
     * Create a server with the specified host, port and service implementation.
     *
     * @param host            The host address to bind to
     * @param port            The port to listen on
     * @param discoveryService The endpoint discovery service implementation
     */
    public EnvoyGrpcServer(String host, int port, EndpointDiscoveryService discoveryService) {
        this.host = host;
        this.port = port;
        this.discoveryService = discoveryService;
    }
    
    /**
     * Get the endpoint discovery service implementation.
     *
     * @return The endpoint discovery service implementation
     */
    public EndpointDiscoveryService getDiscoveryService() {
        return discoveryService;
    }
    
    /**
     * Start the gRPC server. Implements the Managed interface from Dropwizard.
     *
     * @throws Exception If the server cannot be started
     */
    @Override
    public void start() throws Exception {
        server = NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
                .addService(discoveryService)
                .executor(Executors.newFixedThreadPool(10))
                .build()
                .start();
                
        logger.info("Envoy gRPC server started, listening on {}:{}", host, port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutting down Envoy gRPC server due to JVM shutdown");
                try {
                    server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("Error during server shutdown", e);
                }
            }
        });
    }

    /**
     * Stop the gRPC server. Implements the Managed interface from Dropwizard.
     *
     * @throws Exception If the server cannot be stopped
     */
    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Block the current thread until the server is shut down.
     * This is typically used in standalone mode.
     * 
     * @throws InterruptedException If the wait is interrupted
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
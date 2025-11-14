package com.twilio.envoy.health;

import com.codahale.metrics.health.HealthCheck;

/**
 * Health check for the Envoy gRPC server.
 * <p>
 * This health check is registered with Dropwizard to report the health status 
 * of the gRPC server component. Dropwizard will expose this status through its admin
 * interface at /healthcheck.
 */
public class GrpcServerHealthCheck extends HealthCheck {
    
    @Override
    protected Result check() throws Exception {
        // A simple health check that always reports healthy.
        // In a real-world scenario, this would check connectivity to the gRPC server,
        // possibly by making a test request or checking server metrics.
        return Result.healthy();
    }
}
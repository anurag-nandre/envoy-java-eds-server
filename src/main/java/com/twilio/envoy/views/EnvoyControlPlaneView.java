package com.twilio.envoy.views;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/envoy")
@Produces(MediaType.APPLICATION_JSON)
public class EnvoyControlPlaneView {


    @GET
    public String getConfig() {
        return "{ \"status\": \"Envoy Control Plane is running\" }";
    }

}
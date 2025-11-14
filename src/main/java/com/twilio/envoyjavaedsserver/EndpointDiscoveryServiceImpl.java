package com.twilio.envoyjavaedsserver;

import com.google.protobuf.Any;
import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.envoyproxy.envoy.service.endpoint.v3.EndpointDiscoveryServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EndpointDiscoveryServiceImpl extends EndpointDiscoveryServiceGrpc.EndpointDiscoveryServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(EndpointDiscoveryServiceImpl.class);
    private final AtomicInteger versionCounter = new AtomicInteger(1);
    
    // Store endpoints by cluster name
    private final Map<String, List<ServiceEndpoint>> endpoints = new ConcurrentHashMap<>();

    Map<String,DiscoveryResponse> versionedDiscoveryResponseMap = new ConcurrentHashMap<>();
    
    // Keep track of all active streams for pushing updates
    private final Set<StreamObserver<DiscoveryResponse>> activeStreams = ConcurrentHashMap.newKeySet();
    
    /**
     * Represents a service endpoint
     */
    public static class ServiceEndpoint {
        private final String host;
        private final int port;
        private final Map<String, String> metadata;
        
        public ServiceEndpoint(String host, int port) {
            this(host, port, new HashMap<>());
        }
        
        public ServiceEndpoint(String host, int port, Map<String, String> metadata) {
            this.host = host;
            this.port = port;
            this.metadata = metadata;
        }
        
        public String getHost() {
            return host;
        }
        
        public int getPort() {
            return port;
        }
        
        public Map<String, String> getMetadata() {
            return metadata;
        }
    }

    @Override
    public StreamObserver<DiscoveryRequest> streamEndpoints(StreamObserver<DiscoveryResponse> responseObserver) {
        logger.info("Received streamEndpoints request");
        
        // Add this stream to active streams for future updates
        activeStreams.add(responseObserver);
        
        return new StreamObserver<DiscoveryRequest>() {
            private List<String> lastRequestedResources = new ArrayList<>();
            
            @Override
            public void onNext(DiscoveryRequest request) {
                logger.info("Received discovery request: type_url={}, version_info={}, response_nonce={}, resource_names={}",
                        request.getTypeUrl(),
                        request.getVersionInfo(),
                        request.getResponseNonce(),
                        request.getResourceNamesList());

                if (request.hasErrorDetail()) {
                    logger.error("Request contains error: {}", request.getErrorDetail().getMessage());
                    // This is a NACK - client is reporting an error with our previous response
                    // In a real implementation, you might want to fix the issue
                    return;
                }
                
                // Save the requested resources for future pushes
                lastRequestedResources = new ArrayList<>(request.getResourceNamesList());

                // Generate response
                DiscoveryResponse response = generateResponse(request);
                responseObserver.onNext(response);

                logger.info("Sent discovery response: version_info={}, nonce={}, resources.size={}",
                        response.getVersionInfo(),
                        response.getNonce(),
                        response.getResourcesCount());
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error in streamEndpoints", t);
                activeStreams.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                logger.info("streamEndpoints completed");
                activeStreams.remove(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }

//    @Override
//    public StreamObserver<DiscoveryRequest> deltaEndpoints(StreamObserver<io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse> responseObserver) {
//        logger.info("Received deltaEndpoints request - Delta xDS not implemented");
//
//        return new StreamObserver<DiscoveryRequest>() {
//            @Override
//            public void onNext(DiscoveryRequest value) {
//                logger.error("Delta xDS not supported by this implementation");
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                logger.error("Error in deltaEndpoints", t);
//            }
//
//            @Override
//            public void onCompleted() {
//                logger.info("deltaEndpoints completed");
//                responseObserver.onCompleted();
//            }
//        };
//    }

    @Override
    public void fetchEndpoints(DiscoveryRequest request, StreamObserver<DiscoveryResponse> responseObserver) {
        logger.info("Received fetchEndpoints request");
        responseObserver.onNext(generateResponse(request));
        responseObserver.onCompleted();
    }

    private DiscoveryResponse generateResponse(DiscoveryRequest request) {


          if(!request.getVersionInfo().isEmpty()&&request.getVersionInfo()!=null){
                logger.info("Request has version info: {}", request.getVersionInfo());
                if(versionedDiscoveryResponseMap.containsKey(request.getVersionInfo())){

                    DiscoveryResponse response = versionedDiscoveryResponseMap.get(request.getVersionInfo());

                    logger.info("Returning cached response for version: {}", request.getVersionInfo());
                    return response;
                } else {
                    logger.info("No cached response for version: {}, generating new response", request.getVersionInfo());
                }
          }else {

              String version = String.valueOf(versionCounter.getAndIncrement());
              String nonce = UUID.randomUUID().toString();

              DiscoveryResponse.Builder responseBuilder = DiscoveryResponse.newBuilder()
                      .setVersionInfo(version)
                      .setNonce(nonce)
                      .setTypeUrl(request.getTypeUrl());

              // Add requested endpoints
              for (String resourceName : request.getResourceNamesList()) {
                  ClusterLoadAssignment cla = createClusterLoadAssignment(resourceName);
                  responseBuilder.addResources(Any.pack(cla));
              }
              responseBuilder.setTypeUrl(request.getTypeUrl());
              versionedDiscoveryResponseMap.put(version, responseBuilder.build());
              return responseBuilder.build();
          }
          return   null;
    }

    private ClusterLoadAssignment createClusterLoadAssignment(String clusterName) {
        // Extract service name from the resource name if it follows a pattern
        String serviceName = clusterName;
        if (clusterName.contains("/")) {
            serviceName = clusterName.substring(clusterName.lastIndexOf('/') + 1);
        }

        logger.info("Creating endpoints for cluster: {}", serviceName);

        List<ServiceEndpoint> serviceEndpoints = endpoints.getOrDefault(serviceName, getDefaultEndpoints(serviceName));
        
        List<LbEndpoint> lbEndpoints = serviceEndpoints.stream()
                .map(this::createLbEndpoint)
                .collect(Collectors.toList());
        
        // Create a locality with those endpoints
        LocalityLbEndpoints localityLbEndpoints = LocalityLbEndpoints.newBuilder()
                .addAllLbEndpoints(lbEndpoints)
                .build();

        // Create the cluster load assignment
        return ClusterLoadAssignment.newBuilder()
                .setClusterName(serviceName)
                .addEndpoints(localityLbEndpoints)
                .build();
    }
    
    private LbEndpoint createLbEndpoint(ServiceEndpoint serviceEndpoint) {
        return LbEndpoint.newBuilder()
                .setEndpoint(Endpoint.newBuilder()
                        .setAddress(Address.newBuilder()
                                .setSocketAddress(SocketAddress.newBuilder()
                                        .setAddress(serviceEndpoint.getHost())
                                        .setPortValue(serviceEndpoint.getPort())
                                        .build())
                                .build())
                        .build())
                .build();
    }
    
    /**
     * Returns default endpoints for a service if none are registered
     */
    private List<ServiceEndpoint> getDefaultEndpoints(String serviceName) {
        // Default to localhost with ports 8080 and 8081
        return Arrays.asList(
                new ServiceEndpoint("127.0.0.1", 8080),
                new ServiceEndpoint("127.0.0.1", 8081)
        );
    }
    
    /**
     * Add an endpoint to a cluster
     * @param clusterName The name of the cluster
     * @param host The endpoint host
     * @param port The endpoint port
     */
    public void addEndpoint(String clusterName, String host, int port) {
        addEndpoint(clusterName, new ServiceEndpoint(host, port));
    }
    
    /**
     * Add an endpoint to a cluster
     * @param clusterName The name of the cluster
     * @param endpoint The endpoint to add
     */
    public void addEndpoint(String clusterName, ServiceEndpoint endpoint) {
        endpoints.computeIfAbsent(clusterName, k -> new ArrayList<>()).add(endpoint);
        logger.info("Added endpoint {}:{} to cluster {}", endpoint.getHost(), endpoint.getPort(), clusterName);
        pushUpdatesToAllStreams();
    }
    
    /**
     * Remove an endpoint from a cluster
     * @param clusterName The name of the cluster
     * @param host The endpoint host
     * @param port The endpoint port
     * @return true if the endpoint was removed, false otherwise
     */
    public boolean removeEndpoint(String clusterName, String host, int port) {
        List<ServiceEndpoint> clusterEndpoints = endpoints.get(clusterName);
        if (clusterEndpoints == null) {
            return false;
        }
        
        boolean removed = clusterEndpoints.removeIf(e -> e.getHost().equals(host) && e.getPort() == port);
        if (removed) {
            logger.info("Removed endpoint {}:{} from cluster {}", host, port, clusterName);
            pushUpdatesToAllStreams();
        }
        return removed;
    }
    
    /**
     * Get all endpoints for a cluster
     * @param clusterName The name of the cluster
     * @return The endpoints for the cluster, or an empty list if the cluster doesn't exist
     */
    public List<ServiceEndpoint> getEndpoints(String clusterName) {
        return endpoints.getOrDefault(clusterName, Collections.emptyList());
    }
    
    /**
     * Push updates to all active streams
     */
    private void pushUpdatesToAllStreams() {
        // Increment version to indicate a change
        String version = String.valueOf(versionCounter.getAndIncrement());
        
        for (StreamObserver<DiscoveryResponse> stream : activeStreams) {
            // In a real implementation, we would need to track requested resources per stream
            // For now, push all resources to all streams
            
            DiscoveryResponse response = DiscoveryResponse.newBuilder()
                    .setVersionInfo(version)
                    .setNonce(UUID.randomUUID().toString())
                    .setTypeUrl("type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment")
                    .addAllResources(endpoints.keySet().stream()
                            .map(this::createClusterLoadAssignment)
                            .map(cla -> Any.pack(cla))
                            .collect(Collectors.toList()))
                    .build();
                    
            stream.onNext(response);
            logger.info("Pushed update to stream, version={}, resources.size={}", 
                    version, response.getResourcesCount());
        }
    }
}

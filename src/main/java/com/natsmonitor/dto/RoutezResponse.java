package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoutezResponse(
    @JsonProperty("server_id") String serverId,
    @JsonProperty("server_name") String serverName,
    @JsonProperty("now") String now,
    @JsonProperty("num_routes") int numRoutes,
    @JsonProperty("routes") List<RouteInfo> routes
) {}

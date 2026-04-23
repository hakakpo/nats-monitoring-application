package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectionsResponse(
    @JsonProperty("server_id") String serverId,
    @JsonProperty("now") String now,
    @JsonProperty("num_connections") int numConnections,
    @JsonProperty("total") int total,
    @JsonProperty("offset") int offset,
    @JsonProperty("limit") int limit,
    @JsonProperty("connections") List<ConnectionInfo> connections
) {}

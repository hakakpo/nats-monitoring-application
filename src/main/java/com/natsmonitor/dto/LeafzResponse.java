package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeafzResponse(
        @JsonProperty("server_id") String serverId,
        @JsonProperty("now") String now,
        @JsonProperty("leafnodes") int leafnodes,
        @JsonProperty("leafs") List<LeafInfo> leafs
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LeafInfo(
            @JsonProperty("name") String name,
            @JsonProperty("is_spoke") boolean isSpoke,
            @JsonProperty("account") String account,
            @JsonProperty("ip") String ip,
            @JsonProperty("port") int port,
            @JsonProperty("rtt") String rtt,
            @JsonProperty("in_msgs") long inMsgs,
            @JsonProperty("out_msgs") long outMsgs,
            @JsonProperty("in_bytes") long inBytes,
            @JsonProperty("out_bytes") long outBytes,
            @JsonProperty("subscriptions") int subscriptions
    ) {}
}

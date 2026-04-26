package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteInfo(
        @JsonProperty("rid") long rid,
        @JsonProperty("remote_id") String remoteId,
        @JsonProperty("did_solicit") boolean didSolicit,
        @JsonProperty("ip") String ip,
        @JsonProperty("port") int port,
        @JsonProperty("pending_size") long pendingSize,
        @JsonProperty("in_msgs") long inMsgs,
        @JsonProperty("out_msgs") long outMsgs,
        @JsonProperty("subscriptions") int subscriptions
) {
}

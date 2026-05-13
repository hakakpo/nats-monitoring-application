package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GatewayzResponse(
        @JsonProperty("server_id") String serverId,
        @JsonProperty("now") String now,
        @JsonProperty("name") String name,
        @JsonProperty("host") String host,
        @JsonProperty("port") int port,
        @JsonProperty("outbound_gateways") Map<String, OutboundGateway> outboundGateways,
        @JsonProperty("inbound_gateways") Map<String, InboundGateway> inboundGateways
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutboundGateway(
            @JsonProperty("configured") boolean configured,
            @JsonProperty("connection") GatewayConnection connection
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InboundGateway(
            @JsonProperty("connections") java.util.List<GatewayConnection> connections
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GatewayConnection(
            @JsonProperty("cid") long cid,
            @JsonProperty("ip") String ip,
            @JsonProperty("port") int port,
            @JsonProperty("name") String name,
            @JsonProperty("rtt") String rtt,
            @JsonProperty("in_msgs") long inMsgs,
            @JsonProperty("out_msgs") long outMsgs,
            @JsonProperty("in_bytes") long inBytes,
            @JsonProperty("out_bytes") long outBytes,
            @JsonProperty("subscriptions") int subscriptions,
            @JsonProperty("uptime") String uptime
    ) {}
}

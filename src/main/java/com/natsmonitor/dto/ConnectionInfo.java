package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectionInfo(
        @JsonProperty("cid") long cid,
        @JsonProperty("kind") String kind,
        @JsonProperty("type") String type,
        @JsonProperty("ip") String ip,
        @JsonProperty("port") int port,
        @JsonProperty("start") String start,
        @JsonProperty("last_activity") String lastActivity,
        @JsonProperty("name") String name,
        @JsonProperty("lang") String lang,
        @JsonProperty("version") String version,
        @JsonProperty("account") String account,
        @JsonProperty("pending_bytes") long pendingBytes,
        @JsonProperty("subscriptions") int subscriptions,
        @JsonProperty("subscriptions_list") List<String> subscriptionsList,
        @JsonProperty("in_msgs") long inMsgs,
        @JsonProperty("out_msgs") long outMsgs,
        @JsonProperty("in_bytes") long inBytes,
        @JsonProperty("out_bytes") long outBytes,
        @JsonProperty("slow_consumer") boolean slowConsumer,
        @JsonProperty("uptime") String uptime,
        @JsonProperty("idle") String idle,
        @JsonProperty("rtt") String rtt,
        @JsonProperty("stop") String stop,
        @JsonProperty("reason") String reason
) {
    public ConnectionInfo(long cid, String kind, String type, String ip, int port, String start,
                          String lastActivity, String name, String lang, String version,
                          String account, long pendingBytes, int subscriptions,
                          List<String> subscriptionsList, long inMsgs, long outMsgs,
                          long inBytes, long outBytes, boolean slowConsumer,
                          String uptime, String idle, String rtt) {
        this(cid, kind, type, ip, port, start, lastActivity, name, lang, version, account,
                pendingBytes, subscriptions, subscriptionsList, inMsgs, outMsgs, inBytes, outBytes,
                slowConsumer, uptime, idle, rtt, null, null);
    }
}

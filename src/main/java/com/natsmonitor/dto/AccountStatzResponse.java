package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountStatzResponse(
        @JsonProperty("server_id") String serverId,
        @JsonProperty("now") String now,
        @JsonProperty("account_statz") List<AccountStat> accountStatz
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountStat(
            @JsonProperty("acc") String acc,
            @JsonProperty("conns") int conns,
            @JsonProperty("leafnodes") int leafnodes,
            @JsonProperty("total_conns") int totalConns,
            @JsonProperty("num_subscriptions") int numSubscriptions,
            @JsonProperty("sent") MsgBytes sent,
            @JsonProperty("received") MsgBytes received,
            @JsonProperty("slow_consumers") long slowConsumers
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MsgBytes(
            @JsonProperty("msgs") long msgs,
            @JsonProperty("bytes") long bytes
    ) {}
}


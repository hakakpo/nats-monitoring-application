package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubszResponse(
        @JsonProperty("num_subscriptions") int numSubscriptions,
        @JsonProperty("num_cache") int numCache,
        @JsonProperty("num_inserts") long numInserts,
        @JsonProperty("num_removes") long numRemoves,
        @JsonProperty("num_matches") long numMatches,
        @JsonProperty("cache_hit_rate") double cacheHitRate,
        @JsonProperty("max_fanout") int maxFanout,
        @JsonProperty("avg_fanout") double avgFanout
) {
}


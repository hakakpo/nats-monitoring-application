package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JetStreamInfo(
        @JsonProperty("memory") long memory,
        @JsonProperty("storage") long storage,
        @JsonProperty("reserved_memory") long reservedMemory,
        @JsonProperty("reserved_storage") long reservedStorage,
        @JsonProperty("accounts") int accounts,
        @JsonProperty("ha_assets") int haAssets,
        @JsonProperty("server_id") String serverId,
        @JsonProperty("now") String now,
        @JsonProperty("streams") int streams,
        @JsonProperty("consumers") int consumers,
        @JsonProperty("messages") long messages,
        @JsonProperty("bytes") long bytes,
        @JsonProperty("total") int total,
        @JsonProperty("api") ApiStats api,
        @JsonProperty("config") JetStreamConfig config
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiStats(
            @JsonProperty("total") long total,
            @JsonProperty("errors") long errors
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JetStreamConfig(
            @JsonProperty("max_memory") long maxMemory,
            @JsonProperty("max_storage") long maxStorage,
            @JsonProperty("store_dir") String storeDir,
            @JsonProperty("sync_interval") long syncInterval
    ) {
    }
}

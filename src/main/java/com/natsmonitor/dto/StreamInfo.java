package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StreamInfo(
        @JsonProperty("name") String name,
        @JsonProperty("config") StreamConfig config,
        @JsonProperty("state") StreamState state,
        @JsonProperty("created") String created
) {
    private static final StreamConfig EMPTY_CONFIG = new StreamConfig(
            null, List.of(), null, 0, 0, 0, 0, null, 0, null, 0
    );
    private static final StreamState EMPTY_STATE = new StreamState(
            0, 0, 0, 0, 0, null, null, 0, 0
    );

    public StreamConfig safeConfig() {
        return config != null ? config : EMPTY_CONFIG;
    }

    public StreamState safeState() {
        return state != null ? state : EMPTY_STATE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StreamConfig(
            @JsonProperty("name") String name,
            @JsonProperty("subjects") List<String> subjects,
            @JsonProperty("retention") String retention,
            @JsonProperty("max_consumers") int maxConsumers,
            @JsonProperty("max_msgs") long maxMsgs,
            @JsonProperty("max_bytes") long maxBytes,
            @JsonProperty("max_age") long maxAge,
            @JsonProperty("storage") String storage,
            @JsonProperty("num_replicas") int numReplicas,
            @JsonProperty("discard") String discard,
            @JsonProperty("max_msg_size") long maxMsgSize
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StreamState(
            @JsonProperty("messages") long messages,
            @JsonProperty("bytes") long bytes,
            @JsonProperty("first_seq") long firstSeq,
            @JsonProperty("last_seq") long lastSeq,
            @JsonProperty("consumer_count") int consumerCount,
            @JsonProperty("first_ts") String firstTs,
            @JsonProperty("last_ts") String lastTs,
            @JsonProperty("num_subjects") int numSubjects,
            @JsonProperty("num_deleted") long numDeleted
    ) {
    }
}

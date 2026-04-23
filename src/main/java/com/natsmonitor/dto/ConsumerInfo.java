package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsumerInfo(
    @JsonProperty("stream_name") String streamName,
    @JsonProperty("name") String name,
    @JsonProperty("config") ConsumerConfig config,
    @JsonProperty("delivered") SequenceInfo delivered,
    @JsonProperty("ack_floor") SequenceInfo ackFloor,
    @JsonProperty("num_ack_pending") long numAckPending,
    @JsonProperty("num_redelivered") long numRedelivered,
    @JsonProperty("num_waiting") long numWaiting,
    @JsonProperty("num_pending") long numPending,
    @JsonProperty("created") String created
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConsumerConfig(
        @JsonProperty("durable_name") String durableName,
        @JsonProperty("deliver_subject") String deliverSubject,
        @JsonProperty("deliver_policy") String deliverPolicy,
        @JsonProperty("ack_policy") String ackPolicy,
        @JsonProperty("ack_wait") long ackWait,
        @JsonProperty("max_deliver") int maxDeliver,
        @JsonProperty("filter_subject") String filterSubject,
        @JsonProperty("replay_policy") String replayPolicy
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SequenceInfo(
        @JsonProperty("consumer_seq") long consumerSeq,
        @JsonProperty("stream_seq") long streamSeq
    ) {}

    public long lag() {
        if (delivered != null && ackFloor != null) {
            return delivered.streamSeq() - ackFloor.streamSeq();
        }
        return numPending + numAckPending;
    }
}

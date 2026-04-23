package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StreamListResponse(
    @JsonProperty("total") int total,
    @JsonProperty("offset") int offset,
    @JsonProperty("limit") int limit,
    @JsonProperty("streams") List<StreamInfo> streams
) {}

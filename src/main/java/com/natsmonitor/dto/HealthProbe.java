package com.natsmonitor.dto;

public record HealthProbe(
        boolean success,
        long latencyMs,
        String body,
        String error
) {
}

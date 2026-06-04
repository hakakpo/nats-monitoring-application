package com.natsmonitor.dto;

import java.time.Instant;
import java.util.List;

public record HealthDiagnostic(
        Status status,
        int score,
        boolean connected,
        boolean jetStreamEnabled,
        long healthLatencyMs,
        long monitoringLatencyMs,
        String serverId,
        String serverName,
        String uptime,
        String lastError,
        Instant checkedAt,
        List<Check> checks
) {
    public enum Status {
        OK,
        DEGRADED,
        CRITICAL
    }

    public record Check(
            String name,
            Status status,
            String message
    ) {
    }
}

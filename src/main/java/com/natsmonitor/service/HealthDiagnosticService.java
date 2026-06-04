package com.natsmonitor.service;

import com.natsmonitor.dto.HealthDiagnostic;
import com.natsmonitor.dto.HealthProbe;
import com.natsmonitor.dto.ServerInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class HealthDiagnosticService {
    private final NatsMonitoringService natsService;

    public HealthDiagnosticService(NatsMonitoringService natsService) {
        this.natsService = Objects.requireNonNull(natsService, "natsService must not be null");
    }

    public HealthDiagnostic diagnose() {
        List<HealthDiagnostic.Check> checks = new ArrayList<>();
        HealthProbe health = natsService.checkHealth(false);
        HealthProbe jsHealth = natsService.checkHealth(true);
        ServerInfo serverInfo = natsService.getServerInfo();

        int score = 100;
        if (!health.success()) {
            score -= 60;
            checks.add(new HealthDiagnostic.Check("NATS health", HealthDiagnostic.Status.CRITICAL,
                    defaultMessage(health.error(), "NATS health endpoint is not reachable")));
        } else {
            checks.add(new HealthDiagnostic.Check("NATS health", HealthDiagnostic.Status.OK,
                    "NATS accepts connections in " + health.latencyMs() + " ms"));
        }

        boolean jetStreamEnabled = serverInfo != null && serverInfo.jetstream();
        if (!jsHealth.success()) {
            score -= 15;
            checks.add(new HealthDiagnostic.Check("JetStream health", HealthDiagnostic.Status.DEGRADED,
                    defaultMessage(jsHealth.error(), "JetStream health check failed or JetStream is disabled")));
        } else {
            jetStreamEnabled = true;
            checks.add(new HealthDiagnostic.Check("JetStream health", HealthDiagnostic.Status.OK,
                    "JetStream health endpoint returned OK in " + jsHealth.latencyMs() + " ms"));
        }

        if (serverInfo == null) {
            score -= 25;
            checks.add(new HealthDiagnostic.Check("Server info", HealthDiagnostic.Status.CRITICAL,
                    "Unable to retrieve /varz server information"));
        } else {
            checks.add(new HealthDiagnostic.Check("Server info", HealthDiagnostic.Status.OK,
                    "Server " + safe(serverInfo.serverName(), serverInfo.serverId()) + " running for " + serverInfo.uptime()));
            if (serverInfo.slowConsumers() > 0) {
                score -= 15;
                checks.add(new HealthDiagnostic.Check("Slow consumers", HealthDiagnostic.Status.DEGRADED,
                        serverInfo.slowConsumers() + " slow consumers detected"));
            }
            if (serverInfo.connections() >= Math.max(1, serverInfo.maxConnections()) * 0.9) {
                score -= 20;
                checks.add(new HealthDiagnostic.Check("Connection capacity", HealthDiagnostic.Status.CRITICAL,
                        "Connection usage is above 90% of max_connections"));
            }
            if (serverInfo.cpu() > 85) {
                score -= 10;
                checks.add(new HealthDiagnostic.Check("CPU", HealthDiagnostic.Status.DEGRADED,
                        "CPU usage is above 85%"));
            }
        }

        score = Math.max(0, score);
        HealthDiagnostic.Status status = resolveStatus(score, health.success());
        String lastError = !health.success() ? health.error() : (!jsHealth.success() ? jsHealth.error() : null);
        return new HealthDiagnostic(
                status,
                score,
                health.success(),
                jetStreamEnabled,
                health.latencyMs(),
                serverInfo != null ? health.latencyMs() : -1,
                serverInfo != null ? serverInfo.serverId() : null,
                serverInfo != null ? serverInfo.serverName() : null,
                serverInfo != null ? serverInfo.uptime() : null,
                lastError,
                Instant.now(),
                List.copyOf(checks)
        );
    }

    private HealthDiagnostic.Status resolveStatus(int score, boolean connected) {
        if (!connected || score < 50) {
            return HealthDiagnostic.Status.CRITICAL;
        }
        if (score < 90) {
            return HealthDiagnostic.Status.DEGRADED;
        }
        return HealthDiagnostic.Status.OK;
    }

    private String defaultMessage(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safe(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}

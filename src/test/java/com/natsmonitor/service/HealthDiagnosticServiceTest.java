package com.natsmonitor.service;

import com.natsmonitor.dto.HealthProbe;
import com.natsmonitor.dto.ServerInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthDiagnosticServiceTest {
    private final NatsMonitoringService natsService = mock(NatsMonitoringService.class);
    private final HealthDiagnosticService service = new HealthDiagnosticService(natsService);

    @Test
    void shouldReturnOkDiagnosticWhenServerAndJetStreamAreHealthy() {
        when(natsService.checkHealth(false)).thenReturn(new HealthProbe(true, 4, "ok", null));
        when(natsService.checkHealth(true)).thenReturn(new HealthProbe(true, 5, "ok", null));
        when(natsService.getServerInfo()).thenReturn(serverInfo(2, 100, 0, 10));

        var diagnostic = service.diagnose();

        assertEquals(com.natsmonitor.dto.HealthDiagnostic.Status.OK, diagnostic.status());
        assertTrue(diagnostic.connected());
        assertTrue(diagnostic.jetStreamEnabled());
        assertEquals(100, diagnostic.score());
    }

    @Test
    void shouldReturnCriticalDiagnosticWhenHealthFails() {
        when(natsService.checkHealth(false)).thenReturn(new HealthProbe(false, 30, null, "connection refused"));
        when(natsService.checkHealth(true)).thenReturn(new HealthProbe(false, 31, null, "jetstream disabled"));
        when(natsService.getServerInfo()).thenReturn(null);

        var diagnostic = service.diagnose();

        assertEquals(com.natsmonitor.dto.HealthDiagnostic.Status.CRITICAL, diagnostic.status());
        assertFalse(diagnostic.connected());
        assertEquals("connection refused", diagnostic.lastError());
    }

    @Test
    void shouldReturnDegradedWhenSlowConsumersArePresent() {
        when(natsService.checkHealth(false)).thenReturn(new HealthProbe(true, 4, "ok", null));
        when(natsService.checkHealth(true)).thenReturn(new HealthProbe(false, 5, null, "JetStream disabled"));
        when(natsService.getServerInfo()).thenReturn(serverInfo(95, 100, 4, 100));

        var diagnostic = service.diagnose();

        assertEquals(com.natsmonitor.dto.HealthDiagnostic.Status.DEGRADED, diagnostic.status());
        assertTrue(diagnostic.score() < 90);
    }

    private ServerInfo serverInfo(int connections, long mem, long slowConsumers, int maxConnections) {
        return new ServerInfo("sid", "server", "1", "go", "localhost", 4222, 1024, 1,
                true, "1m", mem, 1.0, connections, connections, 10, slowConsumers,
                100, 100, 1024, 1024, 0, 0, 0, maxConnections, 4, 4, "now", "git");
    }
}

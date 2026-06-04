package com.natsmonitor.scheduler;

import com.natsmonitor.dto.ServerInfo;
import com.natsmonitor.service.AlertService;
import com.natsmonitor.service.NatsMonitoringService;
import com.natsmonitor.service.SnapshotService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MetricsSchedulerTest {

    private final NatsMonitoringService natsService = mock(NatsMonitoringService.class);
    private final AlertService alertService = mock(AlertService.class);
    private final SnapshotService snapshotService = mock(SnapshotService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

    private final MetricsScheduler scheduler = new MetricsScheduler(natsService, alertService, snapshotService, messagingTemplate);

    @Test
    void shouldPublishMetricsWhenServerInfoIsAvailable() {
        ServerInfo info = new ServerInfo("id", "name", "1.0", "go", "localhost", 4222, 0, 1,
                true, "1m", 4096, 0.5, 3, 3, 8, 1, 10, 20, 1024, 2048, 0, 0, 0, 65536, 8, 8, "2026-04-27T00:00:00Z", "abc123");
        when(natsService.getServerInfo()).thenReturn(info);
        when(natsService.formatBytes(4096)).thenReturn("4.0 KB");
        when(natsService.formatBytes(1024)).thenReturn("1.0 KB");
        when(natsService.formatBytes(2048)).thenReturn("2.0 KB");
        when(natsService.getMessageRateHistory()).thenReturn(Map.of("inRate", List.of(1L, 2L)));
        when(natsService.getByteRateHistory()).thenReturn(Map.of("outRate", List.of(3L)));

        scheduler.pollMetrics();

        verify(natsService).updateRateMetrics(info);
        ArgumentCaptor<Map<String, Object>> updateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(org.mockito.Mockito.eq("/topic/metrics"), updateCaptor.capture());
        Map<String, Object> update = updateCaptor.getValue();
        assertEquals(true, update.get("connected"));
        assertEquals(0.5, update.get("cpu"));
        assertEquals("4.0 KB", update.get("memFormatted"));
        assertEquals("1.0 KB", update.get("inBytes"));
        assertEquals("2.0 KB", update.get("outBytes"));
    }

    @Test
    void shouldPublishDisconnectedPayloadWhenServerInfoIsMissing() {
        when(natsService.getServerInfo()).thenReturn(null);

        scheduler.pollMetrics();

        verify(natsService, never()).updateRateMetrics(any());
        verify(messagingTemplate).convertAndSend("/topic/metrics", Map.of("connected", false));
    }

    @Test
    void shouldPublishDisconnectedPayloadWhenPollingFails() {
        when(natsService.getServerInfo()).thenThrow(new RuntimeException("boom"));

        scheduler.pollMetrics();

        verify(messagingTemplate).convertAndSend("/topic/metrics", Map.of("connected", false));
    }

    @Test
    void shouldEvaluateAlertsOnSchedule() {
        scheduler.checkAlerts();

        verify(alertService).evaluateAllRules();
    }

    @Test
    void shouldSwallowAlertEvaluationErrors() {
        doThrow(new RuntimeException("boom")).when(alertService).evaluateAllRules();

        scheduler.checkAlerts();

        verify(alertService).evaluateAllRules();
        assertTrue(true);
    }
}


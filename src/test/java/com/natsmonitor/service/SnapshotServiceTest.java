package com.natsmonitor.service;

import com.natsmonitor.config.NatsMonitoringConfig;
import com.natsmonitor.dto.ConnectionsResponse;
import com.natsmonitor.dto.ConsumerInfo;
import com.natsmonitor.dto.JetStreamInfo;
import com.natsmonitor.dto.ServerInfo;
import com.natsmonitor.dto.StreamInfo;
import com.natsmonitor.dto.StreamListResponse;
import com.natsmonitor.model.AlertHistoryRepository;
import com.natsmonitor.model.Incident;
import com.natsmonitor.model.NatsConsumerSnapshot;
import com.natsmonitor.model.NatsMetricSnapshot;
import com.natsmonitor.repository.IncidentRepository;
import com.natsmonitor.repository.NatsConsumerSnapshotRepository;
import com.natsmonitor.repository.NatsEventRepository;
import com.natsmonitor.repository.NatsMetricSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SnapshotServiceTest {
    private final NatsMetricSnapshotRepository metricRepository = mock(NatsMetricSnapshotRepository.class);
    private final NatsConsumerSnapshotRepository consumerRepository = mock(NatsConsumerSnapshotRepository.class);
    private final NatsEventRepository eventRepository = mock(NatsEventRepository.class);
    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final AlertHistoryRepository alertHistoryRepository = mock(AlertHistoryRepository.class);
    private final NatsMonitoringConfig config = new NatsMonitoringConfig();
    private final SnapshotService service = new SnapshotService(
            metricRepository,
            consumerRepository,
            eventRepository,
            incidentRepository,
            alertHistoryRepository,
            config
    );

    @Test
    void shouldCaptureServerAndConsumerSnapshots() {
        config.setSnapshotRetentionHours(2);
        when(metricRepository.save(any(NatsMetricSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consumerRepository.save(any(NatsConsumerSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.capture(serverInfo(), jetStreamInfo(), streams(), new ConnectionsResponse("sid", "now", 1, 1, 0, 1, List.of()));

        verify(metricRepository).save(any(NatsMetricSnapshot.class));
        verify(consumerRepository).save(any(NatsConsumerSnapshot.class));
        verify(metricRepository).deleteByCapturedAtBefore(any());
        verify(consumerRepository).deleteByCapturedAtBefore(any());
    }

    @Test
    void shouldCleanupMonitoringHistoryUsingFiveDayDefaultRetention() {
        service.cleanupMonitoringHistory();

        verify(metricRepository).deleteByCapturedAtBefore(any());
        verify(consumerRepository).deleteByCapturedAtBefore(any());
        verify(eventRepository).deleteByReceivedAtBefore(any());
        verify(alertHistoryRepository).deleteByTriggeredAtBefore(any());
        verify(incidentRepository).deleteByStatusAndResolvedAtBefore(org.mockito.Mockito.eq(Incident.Status.RESOLVED), any());
    }

    @Test
    void shouldSkipCaptureWhenServerInfoIsNull() {
        service.capture(null, null, null, null);
        verify(metricRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldListRecentSnapshots() {
        NatsMetricSnapshot metric = new NatsMetricSnapshot();
        NatsConsumerSnapshot consumer = new NatsConsumerSnapshot();
        when(metricRepository.findAllByOrderByCapturedAtDesc(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(metric)));
        when(consumerRepository.findAllByOrderByCapturedAtDesc(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(consumer)));

        assertEquals(1, service.recentServerSnapshots(10).size());
        assertEquals(1, service.recentConsumerSnapshots(10).size());
    }

    private ServerInfo serverInfo() {
        return new ServerInfo("sid", "server", "1", "go", "localhost", 4222, 1024, 1,
                true, "1m", 1024, 1.0, 1, 1, 10, 0,
                100, 100, 1024, 1024, 0, 0, 0, 100, 4, 4, "now", "git");
    }

    private JetStreamInfo jetStreamInfo() {
        return new JetStreamInfo(0, 0, 0, 0, 1, 0, "sid", "now", 1, 1, 0, 0, 0, null, null);
    }

    private StreamListResponse streams() {
        ConsumerInfo consumer = new ConsumerInfo("ORDERS", "worker", null,
                new ConsumerInfo.SequenceInfo(10, 20), new ConsumerInfo.SequenceInfo(8, 12),
                3, 1, 0, 5, "now");
        StreamInfo stream = new StreamInfo("ORDERS", null,
                new StreamInfo.StreamState(0, 0, 1, 20, 1, null, null, 0, 0),
                null, List.of(consumer));
        return new StreamListResponse(1, 0, 1, List.of(stream));
    }
}

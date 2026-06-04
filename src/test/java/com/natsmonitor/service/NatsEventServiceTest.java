package com.natsmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsmonitor.model.Incident;
import com.natsmonitor.model.NatsEvent;
import com.natsmonitor.repository.NatsEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NatsEventServiceTest {
    private final NatsEventRepository repository = mock(NatsEventRepository.class);
    private final IncidentService incidentService = mock(IncidentService.class);
    private final NatsEventService service = new NatsEventService(repository, incidentService, new ObjectMapper());

    @Test
    void shouldRecordAuthErrorAndCreateIncident() {
        when(repository.save(any(NatsEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NatsEvent event = service.record("$SYS.SERVER.s1.CLIENT.AUTH.ERR", "{}".getBytes(StandardCharsets.UTF_8));

        assertEquals(NatsEvent.EventCategory.AUTH_ERROR, event.getCategory());
        assertEquals(Incident.Severity.WARNING, event.getSeverity());
        assertEquals("server:s1", event.getResource());
        verify(incidentService).recordEvent(event);
    }

    @Test
    void shouldRecordJetStreamAdvisoryAndExtractConsumerResource() {
        when(repository.save(any(NatsEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String payload = "{\"stream\":\"ORDERS\",\"consumer\":\"worker\",\"error\":\"max deliveries\"}";

        NatsEvent event = service.record("$JS.EVENT.ADVISORY.CONSUMER.MAX_DELIVERIES", payload.getBytes(StandardCharsets.UTF_8));

        assertEquals(NatsEvent.EventCategory.JETSTREAM_ADVISORY, event.getCategory());
        assertEquals("consumer:ORDERS/worker", event.getResource());
        assertTrue(event.getSummary().contains("JetStream advisory"));
        verify(incidentService).recordEvent(event);
    }

    @Test
    void shouldRecordConnectEventWithoutIncidentAndListEvents() {
        when(repository.save(any(NatsEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NatsEvent stored = new NatsEvent();
        when(repository.findAllByOrderByReceivedAtDesc(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(stored)));

        NatsEvent event = service.record("$SYS.ACCOUNT.A.CONNECT", "hello".getBytes(StandardCharsets.UTF_8));

        assertEquals(NatsEvent.EventCategory.CONNECT, event.getCategory());
        assertEquals(Incident.Severity.INFO, event.getSeverity());
        assertEquals("account:A", event.getResource());
        assertEquals(1, service.recentEvents(5).size());
    }
}

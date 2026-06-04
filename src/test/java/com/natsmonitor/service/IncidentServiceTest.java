package com.natsmonitor.service;

import com.natsmonitor.model.AlertHistory;
import com.natsmonitor.model.AlertRule;
import com.natsmonitor.model.Incident;
import com.natsmonitor.model.NatsEvent;
import com.natsmonitor.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentServiceTest {
    private final IncidentRepository repository = mock(IncidentRepository.class);
    private final IncidentService service = new IncidentService(repository);

    @Test
    void shouldCreateIncidentFromAlertHistory() {
        when(repository.findFirstByStatusAndTypeAndResourceOrderByLastSeenDesc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AlertHistory history = new AlertHistory();
        history.setRuleName("lag");
        history.setAlertType(AlertRule.AlertType.CONSUMER_LAG);
        history.setSeverity(Incident.Severity.CRITICAL);
        history.setStreamName("ORDERS");
        history.setConsumerName("worker");
        history.setMessage("lag is high");

        Incident incident = service.recordAlert(history);

        assertEquals("CONSUMER_LAG", incident.getType());
        assertEquals("ORDERS/worker", incident.getResource());
        assertEquals(Incident.Severity.CRITICAL, incident.getSeverity());
    }

    @Test
    void shouldTouchExistingIncidentFromEvent() {
        Incident existing = new Incident();
        existing.setType("AUTH_ERROR");
        existing.setResource("server:s1");
        existing.setSeverity(Incident.Severity.WARNING);
        when(repository.findFirstByStatusAndTypeAndResourceOrderByLastSeenDesc(any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NatsEvent event = new NatsEvent();
        event.setCategory(NatsEvent.EventCategory.AUTH_ERROR);
        event.setSeverity(Incident.Severity.WARNING);
        event.setResource("server:s1");
        event.setSubject("$SYS.SERVER.s1.CLIENT.AUTH.ERR");
        event.setSummary("auth failed");

        Incident incident = service.recordEvent(event);

        assertEquals(2, incident.getEventCount());
        assertEquals("auth failed", incident.getLastMessage());
    }

    @Test
    void shouldResolveAndListIncidents() {
        Incident incident = new Incident();
        when(repository.findById(1L)).thenReturn(Optional.of(incident));
        when(repository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findAllByOrderByLastSeenDesc(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(incident)));
        when(repository.findByStatusOrderByLastSeenDesc(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(incident)));

        assertEquals(Incident.Status.RESOLVED, service.resolveIncident(1L).getStatus());
        assertEquals(1, service.getRecentIncidents(5).size());
        assertEquals(1, service.getOpenIncidents(5).size());
        assertNotNull(incident.getResolvedAt());
    }
}

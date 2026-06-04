package com.natsmonitor.service;

import com.natsmonitor.model.AlertHistory;
import com.natsmonitor.model.Incident;
import com.natsmonitor.model.NatsEvent;
import com.natsmonitor.repository.IncidentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository must not be null");
    }

    @Transactional(readOnly = true)
    public List<Incident> getRecentIncidents(int limit) {
        return incidentRepository.findAllByOrderByLastSeenDesc(PageRequest.of(0, Math.max(1, limit))).getContent();
    }

    @Transactional(readOnly = true)
    public List<Incident> getOpenIncidents(int limit) {
        return incidentRepository.findByStatusOrderByLastSeenDesc(
                Incident.Status.OPEN,
                PageRequest.of(0, Math.max(1, limit))
        ).getContent();
    }

    @Transactional
    public Incident resolveIncident(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Incident not found: " + id));
        incident.resolve();
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident recordAlert(AlertHistory history) {
        String resource = resolveAlertResource(history);
        return upsertIncident(
                history.getAlertType().name(),
                resource,
                history.getSeverity(),
                "Alert: " + history.getRuleName(),
                history.getMessage(),
                history.getAlertType().name()
        );
    }

    @Transactional
    public Incident recordEvent(NatsEvent event) {
        String resource = event.getResource() != null && !event.getResource().isBlank()
                ? event.getResource()
                : event.getSubject();
        return upsertIncident(
                event.getCategory().name(),
                resource,
                event.getSeverity(),
                "NATS event: " + event.getCategory(),
                event.getSummary(),
                event.getSubject()
        );
    }

    private Incident upsertIncident(String type, String resource, Incident.Severity severity,
                                    String title, String message, String eventType) {
        Incident.Severity effectiveSeverity = severity != null ? severity : Incident.Severity.WARNING;
        return incidentRepository.findFirstByStatusAndTypeAndResourceOrderByLastSeenDesc(
                        Incident.Status.OPEN,
                        type,
                        resource
                )
                .map(existing -> {
                    existing.touch(effectiveSeverity, eventType, message);
                    return incidentRepository.save(existing);
                })
                .orElseGet(() -> {
                    Incident incident = new Incident();
                    incident.setType(type);
                    incident.setResource(resource);
                    incident.setSeverity(effectiveSeverity);
                    incident.setTitle(title);
                    incident.setSummary(message);
                    incident.setLastEventType(eventType);
                    incident.setLastMessage(message);
                    return incidentRepository.save(incident);
                });
    }

    private String resolveAlertResource(AlertHistory history) {
        if (history.getStreamName() != null && !history.getStreamName().isBlank()) {
            if (history.getConsumerName() != null && !history.getConsumerName().isBlank()) {
                return history.getStreamName() + "/" + history.getConsumerName();
            }
            return history.getStreamName();
        }
        return "server";
    }
}

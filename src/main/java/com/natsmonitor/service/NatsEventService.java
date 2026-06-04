package com.natsmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsmonitor.model.Incident;
import com.natsmonitor.model.NatsEvent;
import com.natsmonitor.repository.NatsEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Service
public class NatsEventService {
    private final NatsEventRepository eventRepository;
    private final IncidentService incidentService;
    private final ObjectMapper objectMapper;

    public NatsEventService(NatsEventRepository eventRepository,
                            IncidentService incidentService,
                            ObjectMapper objectMapper) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
        this.incidentService = Objects.requireNonNull(incidentService, "incidentService must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Transactional
    public NatsEvent record(String subject, byte[] data) {
        String payload = data != null ? new String(data, StandardCharsets.UTF_8) : "";
        NatsEvent event = new NatsEvent();
        event.setSubject(subject);
        event.setPayload(payload);
        event.setCategory(resolveCategory(subject));
        event.setSeverity(resolveSeverity(event.getCategory(), subject));
        enrichFromSubject(event, subject);
        enrichFromPayload(event, payload);
        event.setSummary(resolveSummary(event, payload));
        NatsEvent saved = eventRepository.save(event);
        if (saved.getSeverity() != Incident.Severity.INFO) {
            incidentService.recordEvent(saved);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NatsEvent> recentEvents(int limit) {
        return eventRepository.findAllByOrderByReceivedAtDesc(PageRequest.of(0, Math.max(1, limit))).getContent();
    }

    private NatsEvent.EventCategory resolveCategory(String subject) {
        if (subject == null) {
            return NatsEvent.EventCategory.OTHER;
        }
        if (subject.contains(".CONNECT")) return NatsEvent.EventCategory.CONNECT;
        if (subject.contains(".DISCONNECT")) return NatsEvent.EventCategory.DISCONNECT;
        if (subject.contains(".CLIENT.AUTH.ERR")) return NatsEvent.EventCategory.AUTH_ERROR;
        if (subject.contains(".STATSZ")) return NatsEvent.EventCategory.SERVER_STATS;
        if (subject.startsWith("$JS.EVENT.ADVISORY.")) return NatsEvent.EventCategory.JETSTREAM_ADVISORY;
        if (subject.startsWith("$SYS.")) return NatsEvent.EventCategory.SYSTEM;
        return NatsEvent.EventCategory.OTHER;
    }

    private Incident.Severity resolveSeverity(NatsEvent.EventCategory category, String subject) {
        return switch (category) {
            case AUTH_ERROR, JETSTREAM_ADVISORY -> Incident.Severity.WARNING;
            case DISCONNECT -> Incident.Severity.INFO;
            case CONNECT, SERVER_STATS, SYSTEM, OTHER -> Incident.Severity.INFO;
        };
    }

    private void enrichFromSubject(NatsEvent event, String subject) {
        if (subject == null) return;
        String[] parts = subject.split("\\.");
        if (parts.length >= 3 && "$SYS".equals(parts[0]) && "ACCOUNT".equals(parts[1])) {
            event.setAccountId(parts[2]);
            event.setResource("account:" + parts[2]);
        }
        if (parts.length >= 3 && "$SYS".equals(parts[0]) && "SERVER".equals(parts[1])) {
            event.setServerId(parts[2]);
            event.setResource("server:" + parts[2]);
        }
        if (subject.startsWith("$JS.EVENT.ADVISORY.")) {
            event.setResource(subject);
        }
    }

    private void enrichFromPayload(NatsEvent event, String payload) {
        if (payload == null || payload.isBlank()) return;
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (event.getServerId() == null && root.has("server")) {
                event.setServerId(root.path("server").asText(null));
            }
            if (event.getResource() == null && root.has("stream")) {
                event.setResource("stream:" + root.path("stream").asText());
            }
            if (root.has("consumer")) {
                String stream = root.has("stream") ? root.path("stream").asText() : "unknown";
                event.setResource("consumer:" + stream + "/" + root.path("consumer").asText());
            }
            if (root.has("error")) {
                event.setSeverity(Incident.Severity.WARNING);
            }
        } catch (IOException ignored) {
            // Payloads are not guaranteed to be JSON for all system subjects.
        }
    }

    private String resolveSummary(NatsEvent event, String payload) {
        if (event.getCategory() == NatsEvent.EventCategory.AUTH_ERROR) {
            return "NATS authentication error";
        }
        if (event.getCategory() == NatsEvent.EventCategory.JETSTREAM_ADVISORY) {
            return "JetStream advisory on " + event.getSubject();
        }
        if (payload != null && !payload.isBlank()) {
            return payload.length() > 512 ? payload.substring(0, 512) : payload;
        }
        return event.getSubject();
    }
}

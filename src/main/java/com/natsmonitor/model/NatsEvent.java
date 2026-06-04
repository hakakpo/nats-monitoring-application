package com.natsmonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "nats_events")
public class NatsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category = EventCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Incident.Severity severity = Incident.Severity.INFO;

    private String serverId;
    private String accountId;
    private String resource;

    @Column(length = 2048)
    private String summary;

    @Lob
    private String payload;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    public Incident.Severity getSeverity() { return severity; }
    public void setSeverity(Incident.Severity severity) { this.severity = severity; }
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NatsEvent natsEvent)) return false;
        return id != null && Objects.equals(id, natsEvent.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    public enum EventCategory {
        CONNECT,
        DISCONNECT,
        AUTH_ERROR,
        SERVER_STATS,
        JETSTREAM_ADVISORY,
        SYSTEM,
        OTHER
    }
}

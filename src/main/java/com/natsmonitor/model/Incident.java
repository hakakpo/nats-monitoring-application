package com.natsmonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.WARNING;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private String title;

    @Column(length = 2048)
    private String summary;

    @Column(nullable = false)
    private LocalDateTime firstSeen = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastSeen = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private long eventCount = 1;

    private String lastEventType;

    @Column(length = 2048)
    private String lastMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }
    public String getLastEventType() { return lastEventType; }
    public void setLastEventType(String lastEventType) { this.lastEventType = lastEventType; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public void touch(Severity newSeverity, String eventType, String message) {
        this.lastSeen = LocalDateTime.now();
        this.eventCount++;
        this.lastEventType = eventType;
        this.lastMessage = message;
        if (newSeverity.ordinal() > this.severity.ordinal()) {
            this.severity = newSeverity;
        }
    }

    public void resolve() {
        this.status = Status.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Incident incident)) return false;
        return id != null && Objects.equals(id, incident.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    public enum Status {
        OPEN,
        RESOLVED
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}

package com.natsmonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Rule name is required")
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Alert type is required")
    @Column(nullable = false)
    private AlertType type;

    private String streamName;

    private String consumerName;

    @Positive(message = "Threshold must be positive")
    @Column(nullable = false)
    private long threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Incident.Severity severity = Incident.Severity.WARNING;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    @Column(nullable = false)
    private String emailRecipient;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean emailEnabled = true;

    @Column(length = 2048)
    private String webhookUrl;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean webhookEnabled = false;

    private LocalDateTime lastTriggered;

    private LocalDateTime lastNotified;

    @Positive(message = "Cooldown must be positive")
    @Column(nullable = false)
    private int cooldownMinutes = 15;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }
    public String getStreamName() { return streamName; }
    public void setStreamName(String streamName) { this.streamName = streamName; }
    public String getConsumerName() { return consumerName; }
    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
    public long getThreshold() { return threshold; }
    public void setThreshold(long threshold) { this.threshold = threshold; }
    public Incident.Severity getSeverity() { return severity; }
    public void setSeverity(Incident.Severity severity) { this.severity = severity; }
    public String getEmailRecipient() { return emailRecipient; }
    public void setEmailRecipient(String emailRecipient) { this.emailRecipient = emailRecipient; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public boolean isWebhookEnabled() { return webhookEnabled; }
    public void setWebhookEnabled(boolean webhookEnabled) { this.webhookEnabled = webhookEnabled; }
    public LocalDateTime getLastTriggered() { return lastTriggered; }
    public void setLastTriggered(LocalDateTime lastTriggered) { this.lastTriggered = lastTriggered; }
    public LocalDateTime getLastNotified() { return lastNotified; }
    public void setLastNotified(LocalDateTime lastNotified) { this.lastNotified = lastNotified; }
    public int getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AlertRule alertRule)) {
            return false;
        }
        return id != null && Objects.equals(id, alertRule.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public enum AlertType {
        STUCK_MESSAGES,
        CONSUMER_LAG,
        SLOW_CONSUMERS,
        HIGH_MEMORY,
        HIGH_PENDING_ACKS,
        STREAM_MESSAGE_COUNT,
        CONNECTION_COUNT
    }
}

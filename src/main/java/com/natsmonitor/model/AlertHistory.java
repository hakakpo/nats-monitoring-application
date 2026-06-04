package com.natsmonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alert_history")
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertRule.AlertType alertType;

    @Column(nullable = false, length = 2048)
    private String message;

    private String streamName;
    private String consumerName;

    @Column(nullable = false)
    private long currentValue;

    @Column(nullable = false)
    private long threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Incident.Severity severity = Incident.Severity.WARNING;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = false)
    private String emailSentTo;

    @Column(nullable = false)
    private boolean emailSent;

    @Column(length = 2048)
    private String webhookUrl;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean webhookSent;

    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime triggeredAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public AlertRule.AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertRule.AlertType alertType) { this.alertType = alertType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStreamName() { return streamName; }
    public void setStreamName(String streamName) { this.streamName = streamName; }
    public String getConsumerName() { return consumerName; }
    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
    public long getCurrentValue() { return currentValue; }
    public void setCurrentValue(long currentValue) { this.currentValue = currentValue; }
    public long getThreshold() { return threshold; }
    public void setThreshold(long threshold) { this.threshold = threshold; }
    public Incident.Severity getSeverity() { return severity; }
    public void setSeverity(Incident.Severity severity) { this.severity = severity; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public String getEmailSentTo() { return emailSentTo; }
    public void setEmailSentTo(String emailSentTo) { this.emailSentTo = emailSentTo; }
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public boolean isWebhookSent() { return webhookSent; }
    public void setWebhookSent(boolean webhookSent) { this.webhookSent = webhookSent; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AlertHistory alertHistory)) {
            return false;
        }
        return id != null && Objects.equals(id, alertHistory.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

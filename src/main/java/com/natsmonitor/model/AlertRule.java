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

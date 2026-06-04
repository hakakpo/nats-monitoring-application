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

    @Column(nullable = false)
    private String message;

    private String streamName;
    private String consumerName;

    @Column(nullable = false)
    private long currentValue;

    @Column(nullable = false)
    private long threshold;

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

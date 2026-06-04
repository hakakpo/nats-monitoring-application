package com.natsmonitor.dto;

import com.natsmonitor.model.AlertRule;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlertRuleRequest(
        @NotBlank(message = "Rule name is required")
        String name,

        @NotNull(message = "Alert type is required")
        AlertRule.AlertType type,

        String streamName,

        String consumerName,

        @Positive(message = "Threshold must be positive")
        long threshold,

        com.natsmonitor.model.Incident.Severity severity,

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        String emailRecipient,

        Boolean enabled,

        Boolean emailEnabled,

        String webhookUrl,

        Boolean webhookEnabled,

        int cooldownMinutes
) {
    public AlertRule toEntity() {
        AlertRule rule = new AlertRule();
        rule.setName(name);
        rule.setType(type);
        rule.setStreamName(streamName);
        rule.setConsumerName(consumerName);
        rule.setThreshold(threshold);
        rule.setSeverity(severity != null ? severity : com.natsmonitor.model.Incident.Severity.WARNING);
        rule.setEmailRecipient(emailRecipient);
        rule.setEnabled(enabled != null ? enabled : true);
        rule.setEmailEnabled(emailEnabled != null ? emailEnabled : true);
        rule.setWebhookUrl(webhookUrl);
        rule.setWebhookEnabled(webhookEnabled != null ? webhookEnabled : webhookUrl != null && !webhookUrl.isBlank());
        rule.setCooldownMinutes(cooldownMinutes > 0 ? cooldownMinutes : 15);
        return rule;
    }
}

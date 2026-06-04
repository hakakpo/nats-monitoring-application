package com.natsmonitor.service;

import com.natsmonitor.dto.ConsumerInfo;
import com.natsmonitor.dto.ServerInfo;
import com.natsmonitor.dto.StreamInfo;
import com.natsmonitor.dto.StreamListResponse;
import com.natsmonitor.model.AlertHistory;
import com.natsmonitor.model.AlertHistoryRepository;
import com.natsmonitor.model.AlertRule;
import com.natsmonitor.model.AlertRuleRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final String ALERT_RULE_NOT_FOUND = "Alert rule not found: ";
    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final NatsMonitoringService natsService;
    private final IncidentService incidentService;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username:nats-monitor@localhost}")
    private String fromEmail;

    public AlertService(AlertRuleRepository alertRuleRepository,
                        AlertHistoryRepository alertHistoryRepository,
                        NatsMonitoringService natsService,
                        IncidentService incidentService,
                        JavaMailSender mailSender) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.natsService = natsService;
        this.incidentService = incidentService;
        this.mailSender = mailSender;
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }

    public List<AlertRule> getAllRules() {
        return alertRuleRepository.findAll();
    }

    public AlertRule saveRule(AlertRule rule) {
        validateRule(rule);
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(LocalDateTime.now());
        }
        if (rule.getCooldownMinutes() <= 0) {
            rule.setCooldownMinutes(15);
        }
        if (rule.getSeverity() == null) {
            rule.setSeverity(com.natsmonitor.model.Incident.Severity.WARNING);
        }
        return alertRuleRepository.save(rule);
    }

    public void deleteRule(Long id) {
        alertRuleRepository.deleteById(id);
    }

    public AlertRule toggleRule(Long id) {
        return alertRuleRepository.findById(id).map(rule -> {
            rule.setEnabled(!rule.isEnabled());
            return alertRuleRepository.save(rule);
        }).orElseThrow(() -> new NoSuchElementException(ALERT_RULE_NOT_FOUND + id));
    }

    public AlertRule toggleEmailEnabled(Long id) {
        return alertRuleRepository.findById(id).map(rule -> {
            rule.setEmailEnabled(!rule.isEmailEnabled());
            return alertRuleRepository.save(rule);
        }).orElseThrow(() -> new NoSuchElementException(ALERT_RULE_NOT_FOUND + id));
    }

    public AlertRule toggleWebhookEnabled(Long id) {
        return alertRuleRepository.findById(id).map(rule -> {
            rule.setWebhookEnabled(!rule.isWebhookEnabled());
            return alertRuleRepository.save(rule);
        }).orElseThrow(() -> new NoSuchElementException(ALERT_RULE_NOT_FOUND + id));
    }

    public List<AlertHistory> getRecentHistory(int limit) {
        return alertHistoryRepository.findAllByOrderByTriggeredAtDesc(
                org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    public long getAlertCountLast24h() {
        return alertHistoryRepository.countByTriggeredAtAfter(LocalDateTime.now().minusHours(24));
    }

    public void evaluateAllRules() {
        List<AlertRule> enabledRules = alertRuleRepository.findByEnabledTrue();
        for (AlertRule rule : enabledRules) {
            try {
                evaluateRule(rule);
            } catch (Exception e) {
                log.error("Error evaluating alert rule '{}': {}", rule.getName(), e.getMessage());
            }
        }
    }

    private void evaluateRule(AlertRule rule) {
        long currentValue = getCurrentValueForRule(rule);
        if (currentValue < 0) return; // Could not get value

        if (currentValue >= rule.getThreshold()) {
            if (shouldNotify(rule)) {
                triggerAlert(rule, currentValue);
            }
        }
    }

    private long getCurrentValueForRule(AlertRule rule) {
        return switch (rule.getType()) {
            case STUCK_MESSAGES, STREAM_MESSAGE_COUNT -> getStreamMessageCount(rule.getStreamName());
            case CONSUMER_LAG -> getConsumerMetric(rule, ConsumerMetric.LAG);
            case SLOW_CONSUMERS -> {
                ServerInfo info = natsService.getServerInfo();
                yield info != null ? info.slowConsumers() : -1;
            }
            case HIGH_MEMORY -> {
                ServerInfo info = natsService.getServerInfo();
                yield info != null ? info.mem() / (1024 * 1024) : -1; // MB
            }
            case HIGH_PENDING_ACKS -> getConsumerMetric(rule, ConsumerMetric.ACK_PENDING);
            case CONNECTION_COUNT -> {
                ServerInfo info = natsService.getServerInfo();
                yield info != null ? info.connections() : -1;
            }
        };
    }

    private long getConsumerMetric(AlertRule rule, ConsumerMetric metric) {
        StreamListResponse streams = natsService.getStreams();
        if (streams == null || streams.streams() == null) {
            return -1;
        }

        boolean hasStreamFilter = rule.getStreamName() != null && !rule.getStreamName().isBlank();
        boolean hasConsumerFilter = rule.getConsumerName() != null && !rule.getConsumerName().isBlank();
        long total = 0;
        boolean matched = false;

        for (StreamInfo stream : streams.streams()) {
            if (stream == null || (hasStreamFilter && !rule.getStreamName().equals(stream.name()))) {
                continue;
            }
            for (ConsumerInfo consumer : stream.safeConsumers()) {
                if (consumer == null || (hasConsumerFilter && !rule.getConsumerName().equals(consumer.name()))) {
                    continue;
                }
                matched = true;
                total += switch (metric) {
                    case LAG -> Math.max(0, consumer.lag());
                    case ACK_PENDING -> Math.max(0, consumer.numAckPending());
                };
            }
        }

        return matched ? total : -1;
    }

    private long getStreamMessageCount(String streamName) {
        StreamListResponse streams = natsService.getStreams();
        if (streams == null || streams.streams() == null) {
            return -1;
        }

        boolean hasStreamFilter = streamName != null && !streamName.isBlank();

        return streams.streams().stream()
                .filter(Objects::nonNull)
                .filter(stream -> !hasStreamFilter || streamName.equals(stream.name()))
                .map(StreamInfo::state)
                .filter(Objects::nonNull)
                .mapToLong(StreamInfo.StreamState::messages)
                .reduce(hasStreamFilter ? -1 : 0, (left, right) -> left < 0 ? right : left + right);
    }

    private boolean shouldNotify(AlertRule rule) {
        if (rule.getLastNotified() == null) return true;
        return rule.getLastNotified()
                .plusMinutes(rule.getCooldownMinutes())
                .isBefore(LocalDateTime.now());
    }

    private void triggerAlert(AlertRule rule, long currentValue) {
        String message = String.format(
                "Alert: %s\nType: %s\nCurrent Value: %d\nThreshold: %d\nStream: %s\nConsumer: %s\nTime: %s",
                rule.getName(), rule.getType(), currentValue, rule.getThreshold(),
                rule.getStreamName() != null ? rule.getStreamName() : "N/A",
                rule.getConsumerName() != null ? rule.getConsumerName() : "N/A",
                LocalDateTime.now()
        );

        AlertHistory history = new AlertHistory();
        history.setRuleName(rule.getName());
        history.setAlertType(rule.getType());
        history.setMessage(message);
        history.setStreamName(rule.getStreamName());
        history.setConsumerName(rule.getConsumerName());
        history.setCurrentValue(currentValue);
        history.setThreshold(rule.getThreshold());
        history.setSeverity(rule.getSeverity());
        history.setEmailSentTo(rule.getEmailRecipient());
        history.setTriggeredAt(LocalDateTime.now());

        boolean emailSent = sendAlertEmail(rule, currentValue);
        boolean webhookSent = sendAlertWebhook(rule, currentValue, message);
        history.setEmailSent(emailSent);
        history.setWebhookUrl(rule.getWebhookUrl());
        history.setWebhookSent(webhookSent);
        history.setErrorMessage(resolveNotificationError(rule, emailSent, webhookSent));

        alertHistoryRepository.save(history);
        incidentService.recordAlert(history);

        rule.setLastTriggered(LocalDateTime.now());
        if (emailSent || webhookSent) {
            rule.setLastNotified(LocalDateTime.now());
        }
        alertRuleRepository.save(rule);

        log.warn("Alert triggered: {} (value={}, threshold={})", rule.getName(), currentValue, rule.getThreshold());
    }

    private String resolveNotificationError(AlertRule rule, boolean emailSent, boolean webhookSent) {
        List<String> errors = new ArrayList<>();
        if (!emailSent && !hasWebhookConfigured(rule)) {
            if (!rule.isEmailEnabled()) {
                errors.add("Email notification disabled for this rule");
            } else {
                errors.add("Failed to send email notification");
            }
        } else if (!emailSent && rule.isEmailEnabled()) {
            errors.add("Failed to send email notification");
        }

        if (hasWebhookConfigured(rule) && !webhookSent) {
            if (!rule.isWebhookEnabled()) {
                errors.add("Webhook notification disabled for this rule");
            } else {
                errors.add("Failed to send webhook notification");
            }
        }

        return errors.isEmpty() ? null : String.join("; ", errors);
    }

    private boolean hasWebhookConfigured(AlertRule rule) {
        return rule.getWebhookUrl() != null && !rule.getWebhookUrl().isBlank();
    }

    private boolean sendAlertEmail(AlertRule rule, long currentValue) {
        if (!rule.isEmailEnabled()) {
            log.info("Email sending disabled for rule '{}'", rule.getName());
            return false;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(resolveFromEmail());
            helper.setTo(rule.getEmailRecipient());
            helper.setSubject("[NATS Monitor Alert] " + rule.getName());
            helper.setText(buildHtmlEmail(rule, currentValue), true);
            mailSender.send(mimeMessage);
            log.info("Alert email sent to {} for rule '{}'", rule.getEmailRecipient(), rule.getName());
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send alert email for rule '{}': {}", rule.getName(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending alert email: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendAlertWebhook(AlertRule rule, long currentValue, String message) {
        if (!hasWebhookConfigured(rule)) {
            return false;
        }
        if (!rule.isWebhookEnabled()) {
            log.info("Webhook sending disabled for rule '{}'", rule.getName());
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    rule.getWebhookUrl(),
                    new HttpEntity<>(buildWebhookPayload(rule, currentValue, message), headers),
                    Void.class
            );
            boolean sent = response.getStatusCode().is2xxSuccessful();
            if (sent) {
                log.info("Alert webhook sent to {} for rule '{}'", rule.getWebhookUrl(), rule.getName());
            } else {
                log.error("Alert webhook failed for rule '{}' with status {}", rule.getName(), response.getStatusCode());
            }
            return sent;
        } catch (Exception e) {
            log.error("Failed to send alert webhook for rule '{}': {}", rule.getName(), e.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildWebhookPayload(AlertRule rule, long currentValue, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruleName", rule.getName());
        payload.put("type", rule.getType());
        payload.put("currentValue", currentValue);
        payload.put("threshold", rule.getThreshold());
        payload.put("streamName", rule.getStreamName());
        payload.put("consumerName", rule.getConsumerName());
        payload.put("triggeredAt", LocalDateTime.now().toString());
        payload.put("message", message);
        return payload;
    }

    private String resolveFromEmail() {
        return (fromEmail == null || fromEmail.isBlank()) ? "nats-monitor@localhost" : fromEmail;
    }

    private String buildHtmlEmail(AlertRule rule, long currentValue) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f4f4f4;">
                <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="background: #dc3545; color: white; padding: 20px;">
                        <h2 style="margin:0;">NATS Monitor Alert</h2>
                    </div>
                    <div style="padding: 20px;">
                        <h3 style="color: #333;">%s</h3>
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr><td style="padding: 8px; border-bottom: 1px solid #eee; font-weight: bold;">Type</td><td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td></tr>
                            <tr><td style="padding: 8px; border-bottom: 1px solid #eee; font-weight: bold;">Current Value</td><td style="padding: 8px; border-bottom: 1px solid #eee; color: #dc3545; font-weight: bold;">%d</td></tr>
                            <tr><td style="padding: 8px; border-bottom: 1px solid #eee; font-weight: bold;">Threshold</td><td style="padding: 8px; border-bottom: 1px solid #eee;">%d</td></tr>
                            <tr><td style="padding: 8px; border-bottom: 1px solid #eee; font-weight: bold;">Stream</td><td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td></tr>
                            <tr><td style="padding: 8px; font-weight: bold;">Consumer</td><td style="padding: 8px;">%s</td></tr>
                        </table>
                        <p style="color: #666; font-size: 12px; margin-top: 20px;">Sent by NATS Monitoring Application</p>
                    </div>
                </div>
                </body>
                </html>
                """.formatted(
                rule.getName(), rule.getType(),
                currentValue, rule.getThreshold(),
                rule.getStreamName() != null ? rule.getStreamName() : "All",
                rule.getConsumerName() != null ? rule.getConsumerName() : "All"
        );
    }

    private void validateRule(AlertRule rule) {
        if (rule.getType() == null) {
            throw new IllegalArgumentException("Alert type is required");
        }
        if (rule.getEmailRecipient() == null || rule.getEmailRecipient().isBlank()) {
            throw new IllegalArgumentException("Email recipient is required");
        }
        if (hasWebhookConfigured(rule) && !isValidWebhookUrl(rule.getWebhookUrl())) {
            throw new IllegalArgumentException("Webhook URL must be a valid http or https URL");
        }
        if (rule.isWebhookEnabled() && !hasWebhookConfigured(rule)) {
            throw new IllegalArgumentException("Webhook URL is required when webhook notification is enabled");
        }
        if (!isSupportedType(rule.getType())) {
            throw new IllegalArgumentException("Alert type '%s' is not supported yet".formatted(rule.getType()));
        }
    }

    private boolean isValidWebhookUrl(String webhookUrl) {
        try {
            URI uri = URI.create(webhookUrl);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isSupportedType(AlertRule.AlertType type) {
        return switch (type) {
            case STUCK_MESSAGES, SLOW_CONSUMERS, HIGH_MEMORY, STREAM_MESSAGE_COUNT,
                 CONNECTION_COUNT, CONSUMER_LAG, HIGH_PENDING_ACKS -> true;
        };
    }

    private enum ConsumerMetric {
        LAG,
        ACK_PENDING
    }
}

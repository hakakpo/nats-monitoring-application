package com.natsmonitor.service;

import com.natsmonitor.dto.ConsumerInfo;
import com.natsmonitor.dto.ServerInfo;
import com.natsmonitor.dto.StreamInfo;
import com.natsmonitor.dto.StreamListResponse;
import com.natsmonitor.model.AlertHistory;
import com.natsmonitor.model.AlertHistoryRepository;
import com.natsmonitor.model.AlertRule;
import com.natsmonitor.model.AlertRuleRepository;
import com.sun.net.httpserver.HttpServer;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private NatsMonitoringService natsService;

    @Mock
    private IncidentService incidentService;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AlertService alertService;

    @Captor
    private ArgumentCaptor<AlertHistory> historyCaptor;

    @Captor
    private ArgumentCaptor<AlertRule> ruleCaptor;

    private static AlertRule baseRule(AlertRule.AlertType type, long threshold) {
        AlertRule rule = new AlertRule();
        rule.setName("cpu-alert");
        rule.setType(type);
        rule.setThreshold(threshold);
        rule.setEmailRecipient("ops@example.com");
        rule.setCooldownMinutes(15);
        rule.setEnabled(true);
        rule.setEmailEnabled(true);
        return rule;
    }

    private static StreamListResponse streamsResponse(String streamName, long messages) {
        return new StreamListResponse(1, 0, 1, List.of(
                new StreamInfo(streamName, null, new StreamInfo.StreamState(messages, 256, 1, messages, 0,
                        null, null, 0, 0), null, null)
        ));
    }

    private static StreamListResponse consumerStreamsResponse(String streamName, String consumerName,
                                                              long delivered, long ackFloor,
                                                              long pending, long ackPending) {
        ConsumerInfo consumer = new ConsumerInfo(
                streamName,
                consumerName,
                new ConsumerInfo.ConsumerConfig(consumerName, null, "all", "explicit", 0, -1, null, "instant"),
                new ConsumerInfo.SequenceInfo(10, delivered),
                new ConsumerInfo.SequenceInfo(8, ackFloor),
                ackPending,
                0,
                0,
                pending,
                "2026-04-23T10:00:00Z"
        );
        return new StreamListResponse(1, 0, 1, List.of(
                new StreamInfo(streamName, null, new StreamInfo.StreamState(0, 0, 1, delivered, 1,
                        null, null, 0, 0), null, List.of(consumer))
        ));
    }

    private static ServerInfo serverInfo(int connections, long memBytes, long slowConsumers) {
        return new ServerInfo("id", "name", "1.0", "go", "localhost", 4222, 0, 1, true,
                "1m", memBytes, 0.5, connections, connections, 1, slowConsumers,
                10, 20, 1024, 2048, 0, 0, 0, 65536, 8, 8, "2026-04-27T00:00:00Z", "abc123");
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertService, "fromEmail", "alerts@example.com");
    }

    @Test
    void shouldSaveRuleWithDefaultsWhenMissingOptionalValues() {
        AlertRule rule = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 10);
        rule.setCreatedAt(null);
        rule.setCooldownMinutes(0);
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertRule saved = alertService.saveRule(rule);

        assertSame(rule, saved);
        assertNotNull(saved.getCreatedAt());
        assertEquals(15, saved.getCooldownMinutes());
        verify(alertRuleRepository).save(rule);
    }

    @Test
    void shouldRejectInvalidOrUnsupportedRules() {
        AlertRule missingType = baseRule(null, 10);
        assertThrows(IllegalArgumentException.class, () -> alertService.saveRule(missingType));

        AlertRule missingEmail = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 10);
        missingEmail.setEmailRecipient(" ");
        assertThrows(IllegalArgumentException.class, () -> alertService.saveRule(missingEmail));

        AlertRule invalidWebhook = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 10);
        invalidWebhook.setWebhookUrl("ftp://hooks.example.com/alerts");
        invalidWebhook.setWebhookEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> alertService.saveRule(invalidWebhook));

        AlertRule missingWebhookUrl = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 10);
        missingWebhookUrl.setWebhookEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> alertService.saveRule(missingWebhookUrl));

        AlertRule consumerLag = baseRule(AlertRule.AlertType.CONSUMER_LAG, 10);
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(AlertRule.AlertType.CONSUMER_LAG, alertService.saveRule(consumerLag).getType());
    }

    @Test
    void shouldToggleRuleAndEmailFlags() {
        AlertRule rule = baseRule(AlertRule.AlertType.CONNECTION_COUNT, 2);
        rule.setEnabled(true);
        rule.setEmailEnabled(true);
        rule.setWebhookEnabled(true);
        when(alertRuleRepository.findById(7L)).thenReturn(java.util.Optional.of(rule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertRule toggledRule = alertService.toggleRule(7L);
        AlertRule toggledEmailRule = alertService.toggleEmailEnabled(7L);
        AlertRule toggledWebhookRule = alertService.toggleWebhookEnabled(7L);

        assertFalse(toggledRule.isEnabled());
        assertFalse(toggledEmailRule.isEmailEnabled());
        assertFalse(toggledWebhookRule.isWebhookEnabled());
        verify(alertRuleRepository, times(3)).save(rule);
    }

    @Test
    void shouldThrowWhenTogglingMissingRule() {
        when(alertRuleRepository.findById(anyLong())).thenReturn(java.util.Optional.empty());

        assertThrows(NoSuchElementException.class, () -> alertService.toggleRule(1L));
        assertThrows(NoSuchElementException.class, () -> alertService.toggleEmailEnabled(1L));
        assertThrows(NoSuchElementException.class, () -> alertService.toggleWebhookEnabled(1L));
    }

    @Test
    void shouldDelegateSimpleRepositoryOperations() {
        AlertRule rule = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 10);
        AlertHistory history = new AlertHistory();
        when(alertRuleRepository.findAll()).thenReturn(List.of(rule));
        when(alertHistoryRepository.findAllByOrderByTriggeredAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(history)));
        when(alertHistoryRepository.countByTriggeredAtAfter(any())).thenReturn(3L);
        doNothing().when(alertRuleRepository).deleteById(9L);

        assertEquals(List.of(rule), alertService.getAllRules());
        assertEquals(List.of(history), alertService.getRecentHistory(5));
        assertEquals(3L, alertService.getAlertCountLast24h());
        alertService.deleteRule(9L);

        verify(alertRuleRepository).deleteById(9L);
    }

    @Test
    void shouldTriggerAlertAndSendEmailForMatchingStreamRule() {
        AlertRule rule = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 10);
        rule.setStreamName("ORDERS");
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(natsService.getStreams()).thenReturn(streamsResponse("ORDERS", 25));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        alertService.evaluateAllRules();

        verify(mailSender).send(any(MimeMessage.class));
        verify(alertHistoryRepository).save(historyCaptor.capture());
        verify(alertRuleRepository).save(ruleCaptor.capture());

        AlertHistory history = historyCaptor.getValue();
        assertEquals("cpu-alert", history.getRuleName());
        assertEquals(25, history.getCurrentValue());
        assertTrue(history.isEmailSent());
        assertNull(history.getErrorMessage());

        AlertRule savedRule = ruleCaptor.getValue();
        assertNotNull(savedRule.getLastTriggered());
        assertNotNull(savedRule.getLastNotified());
    }

    @Test
    void shouldRecordDisabledEmailInsteadOfSendingNotification() {
        AlertRule rule = baseRule(AlertRule.AlertType.CONNECTION_COUNT, 2);
        rule.setEmailEnabled(false);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(natsService.getServerInfo()).thenReturn(serverInfo(5, 0, 0));

        alertService.evaluateAllRules();

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(alertHistoryRepository).save(historyCaptor.capture());
        AlertHistory history = historyCaptor.getValue();
        assertFalse(history.isEmailSent());
        assertEquals("Email notification disabled for this rule", history.getErrorMessage());
        assertNull(rule.getLastNotified());
        assertNotNull(rule.getLastTriggered());
    }

    @Test
    void shouldSendWebhookWhenConfigured() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/alerts", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            AlertRule rule = baseRule(AlertRule.AlertType.CONNECTION_COUNT, 2);
            rule.setEmailEnabled(false);
            rule.setWebhookUrl("http://localhost:%d/alerts".formatted(server.getAddress().getPort()));
            rule.setWebhookEnabled(true);
            when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(natsService.getServerInfo()).thenReturn(serverInfo(5, 0, 0));

            alertService.evaluateAllRules();

            verify(mailSender, never()).send(any(MimeMessage.class));
            verify(alertHistoryRepository).save(historyCaptor.capture());
            AlertHistory history = historyCaptor.getValue();
            assertFalse(history.isEmailSent());
            assertTrue(history.isWebhookSent());
            assertEquals(rule.getWebhookUrl(), history.getWebhookUrl());
            assertNull(history.getErrorMessage());
            assertNotNull(rule.getLastNotified());
            assertTrue(requestBody.get().contains("\"ruleName\":\"cpu-alert\""));
            assertTrue(requestBody.get().contains("\"currentValue\":5"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRespectCooldownAndSkipNotification() {
        AlertRule rule = baseRule(AlertRule.AlertType.HIGH_MEMORY, 1);
        rule.setLastNotified(LocalDateTime.now());
        rule.setCooldownMinutes(30);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(natsService.getServerInfo()).thenReturn(serverInfo(1, 16L * 1024 * 1024, 0));

        alertService.evaluateAllRules();

        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
        verify(alertRuleRepository, never()).save(any(AlertRule.class));
    }

    @Test
    void shouldContinueEvaluatingRulesWhenOneFails() {
        AlertRule failingRule = baseRule(AlertRule.AlertType.HIGH_MEMORY, 1);
        failingRule.setName("failing-rule");
        AlertRule succeedingRule = baseRule(AlertRule.AlertType.CONNECTION_COUNT, 1);
        succeedingRule.setName("second-rule");
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(failingRule, succeedingRule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(natsService.getServerInfo())
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(serverInfo(4, 0, 0));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        alertService.evaluateAllRules();

        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertEquals("second-rule", historyCaptor.getValue().getRuleName());
    }

    @Test
    void shouldEvaluateSlowConsumersRule() {
        AlertRule rule = baseRule(AlertRule.AlertType.SLOW_CONSUMERS, 2);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(natsService.getServerInfo()).thenReturn(serverInfo(1, 0, 5));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        alertService.evaluateAllRules();

        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertEquals(5, historyCaptor.getValue().getCurrentValue());
    }

    @Test
    void shouldEvaluateStreamMessageCountWithoutStreamFilter() {
        AlertRule rule = baseRule(AlertRule.AlertType.STREAM_MESSAGE_COUNT, 5);
        rule.setStreamName(null);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(natsService.getStreams()).thenReturn(new StreamListResponse(2, 0, 2, List.of(
                new StreamInfo("S1", null, new StreamInfo.StreamState(10, 0, 1, 10, 0, null, null, 0, 0), null, null),
                new StreamInfo("S2", null, new StreamInfo.StreamState(8, 0, 1, 8, 0, null, null, 0, 0), null, null)
        )));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        alertService.evaluateAllRules();

        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertEquals(18, historyCaptor.getValue().getCurrentValue());
    }

    @Test
    void shouldNotTriggerWhenValueBelowThreshold() {
        AlertRule rule = baseRule(AlertRule.AlertType.CONNECTION_COUNT, 100);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(natsService.getServerInfo()).thenReturn(serverInfo(2, 0, 0));

        alertService.evaluateAllRules();

        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    void shouldEvaluateConsumerLagRule() {
        AlertRule rule = baseRule(AlertRule.AlertType.CONSUMER_LAG, 10);
        rule.setStreamName("ORDERS");
        rule.setConsumerName("worker");
        rule.setEmailEnabled(false);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(natsService.getStreams()).thenReturn(consumerStreamsResponse("ORDERS", "worker", 50, 25, 5, 2));

        alertService.evaluateAllRules();

        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertEquals(25, historyCaptor.getValue().getCurrentValue());
    }

    @Test
    void shouldEvaluateHighPendingAcksRule() {
        AlertRule rule = baseRule(AlertRule.AlertType.HIGH_PENDING_ACKS, 2);
        rule.setStreamName("ORDERS");
        rule.setConsumerName("worker");
        rule.setEmailEnabled(false);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(natsService.getStreams()).thenReturn(consumerStreamsResponse("ORDERS", "worker", 50, 45, 0, 7));

        alertService.evaluateAllRules();

        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertEquals(7, historyCaptor.getValue().getCurrentValue());
    }

    @Test
    void shouldReturnNegativeOneWhenServerInfoIsNullForSlowConsumers() {
        AlertRule rule = baseRule(AlertRule.AlertType.SLOW_CONSUMERS, 1);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(natsService.getServerInfo()).thenReturn(null);

        alertService.evaluateAllRules();

        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    void shouldReturnNegativeOneWhenServerInfoIsNullForHighMemory() {
        AlertRule rule = baseRule(AlertRule.AlertType.HIGH_MEMORY, 1);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(natsService.getServerInfo()).thenReturn(null);

        alertService.evaluateAllRules();

        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    void shouldReturnNegativeOneWhenServerInfoIsNullForConnectionCount() {
        AlertRule rule = baseRule(AlertRule.AlertType.CONNECTION_COUNT, 1);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(natsService.getServerInfo()).thenReturn(null);

        alertService.evaluateAllRules();

        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    void shouldReturnNegativeOneWhenStreamsResponseIsNull() {
        AlertRule rule = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 1);
        rule.setStreamName("ORDERS");
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(natsService.getStreams()).thenReturn(null);

        alertService.evaluateAllRules();

        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
    }

    @Test
    void shouldRecordFailedEmailSendInHistory() {
        AlertRule rule = baseRule(AlertRule.AlertType.CONNECTION_COUNT, 2);
        rule.setEmailEnabled(true);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(natsService.getServerInfo()).thenReturn(serverInfo(5, 0, 0));
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("mail error"));

        alertService.evaluateAllRules();

        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertFalse(historyCaptor.getValue().isEmailSent());
        assertEquals("Failed to send email notification", historyCaptor.getValue().getErrorMessage());
        assertNull(rule.getLastNotified());
    }

    @Test
    void shouldSaveRuleWithExistingCreatedAtAndValidCooldown() {
        AlertRule rule = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 10);
        LocalDateTime existingDate = LocalDateTime.of(2026, 1, 1, 0, 0);
        rule.setCreatedAt(existingDate);
        rule.setCooldownMinutes(30);
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertRule saved = alertService.saveRule(rule);

        assertEquals(existingDate, saved.getCreatedAt());
        assertEquals(30, saved.getCooldownMinutes());
    }

    @Test
    void shouldSaveHighPendingAcksType() {
        AlertRule rule = baseRule(AlertRule.AlertType.HIGH_PENDING_ACKS, 10);
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertRule saved = alertService.saveRule(rule);

        assertEquals(AlertRule.AlertType.HIGH_PENDING_ACKS, saved.getType());
    }

    @Test
    void shouldRejectNullEmailRecipient() {
        AlertRule rule = baseRule(AlertRule.AlertType.STUCK_MESSAGES, 10);
        rule.setEmailRecipient(null);
        assertThrows(IllegalArgumentException.class, () -> alertService.saveRule(rule));
    }
}


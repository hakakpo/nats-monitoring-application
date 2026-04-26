package com.natsmonitor.service;

import com.natsmonitor.dto.ServerInfo;
import com.natsmonitor.dto.StreamInfo;
import com.natsmonitor.dto.StreamListResponse;
import com.natsmonitor.model.AlertHistory;
import com.natsmonitor.model.AlertHistoryRepository;
import com.natsmonitor.model.AlertRule;
import com.natsmonitor.model.AlertRuleRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

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
                        null, null, 0, 0), null)
        ));
    }

    private static ServerInfo serverInfo(int connections, long memBytes, long slowConsumers) {
        return new ServerInfo("id", "name", "1.0", "go", "localhost", 4222, 0, 1, true,
                "1m", memBytes, 0.5, connections, connections, 1, slowConsumers,
                10, 20, 1024, 2048, 0, 0, 0);
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

        AlertRule unsupported = baseRule(AlertRule.AlertType.CONSUMER_LAG, 10);
        assertThrows(IllegalArgumentException.class, () -> alertService.saveRule(unsupported));
    }

    @Test
    void shouldToggleRuleAndEmailFlags() {
        AlertRule rule = baseRule(AlertRule.AlertType.CONNECTION_COUNT, 2);
        rule.setEnabled(true);
        rule.setEmailEnabled(true);
        when(alertRuleRepository.findById(7L)).thenReturn(java.util.Optional.of(rule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertRule toggledRule = alertService.toggleRule(7L);
        AlertRule toggledEmailRule = alertService.toggleEmailEnabled(7L);

        assertFalse(toggledRule.isEnabled());
        assertFalse(toggledEmailRule.isEmailEnabled());
        verify(alertRuleRepository, times(2)).save(rule);
    }

    @Test
    void shouldThrowWhenTogglingMissingRule() {
        when(alertRuleRepository.findById(anyLong())).thenReturn(java.util.Optional.empty());

        assertThrows(NoSuchElementException.class, () -> alertService.toggleRule(1L));
        assertThrows(NoSuchElementException.class, () -> alertService.toggleEmailEnabled(1L));
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
}

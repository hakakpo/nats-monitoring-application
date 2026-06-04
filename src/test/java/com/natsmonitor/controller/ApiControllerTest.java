package com.natsmonitor.controller;

import com.natsmonitor.dto.*;
import com.natsmonitor.model.AlertHistory;
import com.natsmonitor.model.AlertRule;
import com.natsmonitor.service.AlertService;
import com.natsmonitor.service.NatsMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiControllerTest {

    private final NatsMonitoringService natsService = mock(NatsMonitoringService.class);
    private final AlertService alertService = mock(AlertService.class);

    private MockMvc mockMvc;

    private static ServerInfo serverInfo() {
        return new ServerInfo("server-1", "n1", "2.10.29", "go1.24.2", "127.0.0.1", 4222, 1048576, 1,
                true, "1m", 4096, 1.0, 2, 3, 4, 0, 10, 20, 1024, 2048, 0, 0, 0, 65536, 8, 8, "2026-04-27T00:00:00Z", "f91ddd8");
    }

    private static AlertRule alertRule() {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        rule.setName("rule-1");
        rule.setType(AlertRule.AlertType.CONNECTION_COUNT);
        rule.setThreshold(5);
        rule.setEmailRecipient("ops@example.com");
        rule.setWebhookUrl("https://hooks.example.com/nats");
        rule.setWebhookEnabled(true);
        return rule;
    }

    private static String alertRuleJson() {
        return """
                {
                  "name": "rule-1",
                  "type": "CONNECTION_COUNT",
                  "threshold": 5,
                  "emailRecipient": "ops@example.com",
                  "webhookUrl": "https://hooks.example.com/nats",
                  "webhookEnabled": true
                }
                """;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ApiController(natsService, alertService)).build();
    }

    @Test
    void shouldReturnStatusPayload() throws Exception {
        when(natsService.isConnected()).thenReturn(true);
        when(natsService.getServerInfo()).thenReturn(serverInfo());
        when(natsService.getNatsUrl()).thenReturn("http://localhost:8222");

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.serverInfo.server_id").value("server-1"))
                .andExpect(jsonPath("$.natsUrl").value("http://localhost:8222"));
    }

    @Test
    void shouldReturnStatusWithEmptyMapWhenServerInfoIsNull() throws Exception {
        when(natsService.isConnected()).thenReturn(false);
        when(natsService.getServerInfo()).thenReturn(null);
        when(natsService.getNatsUrl()).thenReturn("http://localhost:8222");

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
    }

    @Test
    void shouldReturnServerInfoSuccessfully() throws Exception {
        when(natsService.getServerInfo()).thenReturn(serverInfo());

        mockMvc.perform(get("/api/server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server_id").value("server-1"))
                .andExpect(jsonPath("$.version").value("2.10.29"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenServerInfoIsMissing() throws Exception {
        when(natsService.getServerInfo()).thenReturn(null);

        mockMvc.perform(get("/api/server"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnJetStreamInfoSuccessfully() throws Exception {
        JetStreamInfo jsInfo = new JetStreamInfo(128, 256, 0, 0, 1, 0, "server-1",
                "now", 1, 2, 12, 256, 1,
                new JetStreamInfo.ApiStats(2, 0),
                new JetStreamInfo.JetStreamConfig(1024, 2048, "/data", 120000000000L));
        when(natsService.getJetStreamInfo()).thenReturn(jsInfo);

        mockMvc.perform(get("/api/jetstream"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streams").value(1))
                .andExpect(jsonPath("$.consumers").value(2));
    }

    @Test
    void shouldReturnServiceUnavailableWhenJetStreamInfoIsMissing() throws Exception {
        when(natsService.getJetStreamInfo()).thenReturn(null);

        mockMvc.perform(get("/api/jetstream"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnStreamDetailsWhenPresent() throws Exception {
        when(natsService.getStreamDetail("ORDERS")).thenReturn(new StreamInfo("ORDERS", null, null, null, null));

        mockMvc.perform(get("/api/streams/ORDERS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ORDERS"));
    }

    @Test
    void shouldReturnNotFoundWhenStreamIsMissing() throws Exception {
        when(natsService.getStreamDetail("MISSING")).thenReturn(null);

        mockMvc.perform(get("/api/streams/MISSING"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConnectionsSuccessfully() throws Exception {
        ConnectionsResponse connections = new ConnectionsResponse("server-1", "now", 2, 2, 0, 10, List.of());
        when(natsService.getConnections()).thenReturn(connections);

        mockMvc.perform(get("/api/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.num_connections").value(2));
    }

    @Test
    void shouldReturnServiceUnavailableWhenConnectionsAreMissing() throws Exception {
        when(natsService.getConnections()).thenReturn(null);

        mockMvc.perform(get("/api/connections"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnRoutesSuccessfully() throws Exception {
        RoutezResponse routez = new RoutezResponse("server-1", "n1", "now", 0, List.of());
        when(natsService.getRoutez()).thenReturn(routez);

        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.num_routes").value(0));
    }

    @Test
    void shouldReturnServiceUnavailableWhenRoutesAreMissing() throws Exception {
        when(natsService.getRoutez()).thenReturn(null);

        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnSubscriptionsSuccessfully() throws Exception {
        SubszResponse subsz = new SubszResponse(5, 2, 10, 1, 9, 85.0, 3, 1.5);
        when(natsService.getSubsz()).thenReturn(subsz);

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.num_subscriptions").value(5));
    }

    @Test
    void shouldReturnServiceUnavailableWhenSubscriptionsAreMissing() throws Exception {
        when(natsService.getSubsz()).thenReturn(null);

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnAccountStatsSuccessfully() throws Exception {
        AccountStatzResponse accStatz = new AccountStatzResponse("server-1", "now", List.of());
        when(natsService.getAccountStatz()).thenReturn(accStatz);

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server_id").value("server-1"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenAccountStatsAreMissing() throws Exception {
        when(natsService.getAccountStatz()).thenReturn(null);

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnLeafNodesSuccessfully() throws Exception {
        LeafzResponse leafz = new LeafzResponse("server-1", "now", 0, List.of());
        when(natsService.getLeafz()).thenReturn(leafz);

        mockMvc.perform(get("/api/leafnodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leafnodes").value(0));
    }

    @Test
    void shouldReturnServiceUnavailableWhenLeafNodesAreMissing() throws Exception {
        when(natsService.getLeafz()).thenReturn(null);

        mockMvc.perform(get("/api/leafnodes"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnGatewaysSuccessfully() throws Exception {
        GatewayzResponse gatewayz = new GatewayzResponse("server-1", "now", "gw", "host", 7222, Map.of(), Map.of());
        when(natsService.getGatewayz()).thenReturn(gatewayz);

        mockMvc.perform(get("/api/gateways"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("gw"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenGatewaysAreMissing() throws Exception {
        when(natsService.getGatewayz()).thenReturn(null);

        mockMvc.perform(get("/api/gateways"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnRateHistories() throws Exception {
        when(natsService.getMessageRateHistory()).thenReturn(Map.of("inRate", List.of(1L, 2L)));
        when(natsService.getByteRateHistory()).thenReturn(Map.of("outRate", List.of(5L)));

        mockMvc.perform(get("/api/metrics/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageRates.inRate[0]").value(1))
                .andExpect(jsonPath("$.byteRates.outRate[0]").value(5));
    }

    @Test
    void shouldCreateAlertRule() throws Exception {
        AlertRule rule = alertRule();
        when(alertService.saveRule(any(AlertRule.class))).thenReturn(rule);

        mockMvc.perform(post("/api/alerts/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertRuleJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("rule-1"))
                .andExpect(jsonPath("$.emailRecipient").value("ops@example.com"))
                .andExpect(jsonPath("$.webhookUrl").value("https://hooks.example.com/nats"))
                .andExpect(jsonPath("$.webhookEnabled").value(true));
    }

    @Test
    void shouldUpdateAlertRule() throws Exception {
        AlertRule rule = alertRule();
        when(alertService.saveRule(any(AlertRule.class))).thenReturn(rule);

        mockMvc.perform(put("/api/alerts/rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertRuleJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("rule-1"));
    }

    @Test
    void shouldDeleteAlertRule() throws Exception {
        doNothing().when(alertService).deleteRule(1L);

        mockMvc.perform(delete("/api/alerts/rules/1"))
                .andExpect(status().isNoContent());

        verify(alertService).deleteRule(1L);
    }

    @Test
    void shouldToggleEmailForRule() throws Exception {
        AlertRule rule = alertRule();
        rule.setEmailEnabled(false);
        when(alertService.toggleEmailEnabled(1L)).thenReturn(rule);

        mockMvc.perform(post("/api/alerts/rules/1/toggle-email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false));
    }

    @Test
    void shouldToggleWebhookForRule() throws Exception {
        AlertRule rule = alertRule();
        rule.setWebhookEnabled(false);
        when(alertService.toggleWebhookEnabled(1L)).thenReturn(rule);

        mockMvc.perform(post("/api/alerts/rules/1/toggle-webhook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookEnabled").value(false));
    }

    @Test
    void shouldReturnAlertHistory() throws Exception {
        AlertHistory history = new AlertHistory();
        history.setRuleName("test-rule");
        history.setAlertType(AlertRule.AlertType.CONNECTION_COUNT);
        history.setMessage("test");
        history.setCurrentValue(5);
        history.setThreshold(3);
        history.setEmailSentTo("ops@example.com");
        history.setWebhookUrl("https://hooks.example.com/nats");
        history.setWebhookSent(true);
        history.setTriggeredAt(LocalDateTime.now());
        when(alertService.getRecentHistory(50)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/alerts/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleName").value("test-rule"));
    }

    @Test
    void shouldReturnAlertHistoryWithCustomLimit() throws Exception {
        when(alertService.getRecentHistory(10)).thenReturn(List.of());

        mockMvc.perform(get("/api/alerts/history?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldHandleBadRequestExceptions() throws Exception {
        when(alertService.saveRule(any(AlertRule.class))).thenThrow(new IllegalArgumentException("Invalid rule"));

        mockMvc.perform(post("/api/alerts/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertRuleJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid rule"));
    }

    @Test
    void shouldHandleNotFoundExceptions() throws Exception {
        when(alertService.toggleRule(99L)).thenThrow(new NoSuchElementException("Alert rule not found: 99"));

        mockMvc.perform(post("/api/alerts/rules/99/toggle"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Alert rule not found: 99"));
    }

    @Test
    void shouldListAlertRules() throws Exception {
        when(alertService.getAllRules()).thenReturn(List.of(alertRule()));

        mockMvc.perform(get("/api/alerts/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("rule-1"));
    }

    @Test
    void shouldReturnStreamCollection() throws Exception {
        when(natsService.getStreams()).thenReturn(new StreamListResponse(1, 0, 10, List.of(
                new StreamInfo("ORDERS", null, null, null, null)
        )));

        mockMvc.perform(get("/api/streams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.streams[0].name").value("ORDERS"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenStreamsAreMissing() throws Exception {
        when(natsService.getStreams()).thenReturn(null);

        mockMvc.perform(get("/api/streams"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldToggleRule() throws Exception {
        AlertRule rule = alertRule();
        rule.setEnabled(false);
        when(alertService.toggleRule(1L)).thenReturn(rule);

        mockMvc.perform(post("/api/alerts/rules/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}

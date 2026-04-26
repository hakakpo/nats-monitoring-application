package com.natsmonitor.controller;

import com.natsmonitor.dto.ServerInfo;
import com.natsmonitor.dto.StreamInfo;
import com.natsmonitor.dto.StreamListResponse;
import com.natsmonitor.model.AlertRule;
import com.natsmonitor.service.AlertService;
import com.natsmonitor.service.NatsMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiControllerTest {

    private final NatsMonitoringService natsService = mock(NatsMonitoringService.class);
    private final AlertService alertService = mock(AlertService.class);

    private MockMvc mockMvc;

    private static ServerInfo serverInfo() {
        return new ServerInfo("server-1", "n1", "2.10.29", "go1.24.2", "127.0.0.1", 4222, 1048576, 1,
                true, "1m", 4096, 1.0, 2, 3, 4, 0, 10, 20, 1024, 2048, 0, 0, 0);
    }

    private static AlertRule alertRule() {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        rule.setName("rule-1");
        rule.setType(AlertRule.AlertType.CONNECTION_COUNT);
        rule.setThreshold(5);
        rule.setEmailRecipient("ops@example.com");
        return rule;
    }

    private static String alertRuleJson() {
        return """
                {
                  "name": "rule-1",
                  "type": "CONNECTION_COUNT",
                  "threshold": 5,
                  "emailRecipient": "ops@example.com"
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
    void shouldReturnServiceUnavailableWhenServerInfoIsMissing() throws Exception {
        when(natsService.getServerInfo()).thenReturn(null);

        mockMvc.perform(get("/api/server"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnStreamDetailsWhenPresent() throws Exception {
        when(natsService.getStreamDetail("ORDERS")).thenReturn(new StreamInfo("ORDERS", null, null, null));

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
                .andExpect(jsonPath("$.emailRecipient").value("ops@example.com"));
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
                new StreamInfo("ORDERS", null, null, null)
        )));

        mockMvc.perform(get("/api/streams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.streams[0].name").value("ORDERS"));
    }
}

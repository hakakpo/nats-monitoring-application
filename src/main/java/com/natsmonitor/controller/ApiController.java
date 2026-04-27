package com.natsmonitor.controller;

import com.natsmonitor.dto.*;
import com.natsmonitor.model.AlertRule;
import com.natsmonitor.service.AlertService;
import com.natsmonitor.service.NatsMonitoringService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final String MESSAGE_KEY = "message";

    private final NatsMonitoringService natsService;
    private final AlertService alertService;

    public ApiController(NatsMonitoringService natsService, AlertService alertService) {
        this.natsService = natsService;
        this.alertService = alertService;
    }

    // --- NATS Metrics API ---

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean connected = natsService.isConnected();
        ServerInfo info = natsService.getServerInfo();
        return ResponseEntity.ok(Map.of(
                "connected", connected,
                "serverInfo", info != null ? info : Map.of(),
                "natsUrl", natsService.getNatsUrl()
        ));
    }

    @GetMapping("/server")
    public ResponseEntity<ServerInfo> serverInfo() {
        ServerInfo info = natsService.getServerInfo();
        return info != null ? ResponseEntity.ok(info) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/jetstream")
    public ResponseEntity<JetStreamInfo> jetStreamInfo() {
        JetStreamInfo info = natsService.getJetStreamInfo();
        return info != null ? ResponseEntity.ok(info) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/streams")
    public ResponseEntity<StreamListResponse> streams() {
        StreamListResponse streams = natsService.getStreams();
        return streams != null ? ResponseEntity.ok(streams) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/streams/{name}")
    public ResponseEntity<StreamInfo> streamDetail(@PathVariable String name) {
        StreamInfo stream = natsService.getStreamDetail(name);
        return stream != null ? ResponseEntity.ok(stream) : ResponseEntity.notFound().build();
    }

    @GetMapping("/connections")
    public ResponseEntity<ConnectionsResponse> connections() {
        ConnectionsResponse connections = natsService.getConnections();
        return connections != null ? ResponseEntity.ok(connections) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/routes")
    public ResponseEntity<RoutezResponse> routes() {
        RoutezResponse routez = natsService.getRoutez();
        return routez != null ? ResponseEntity.ok(routez) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<SubszResponse> subscriptions() {
        SubszResponse subsz = natsService.getSubsz();
        return subsz != null ? ResponseEntity.ok(subsz) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/accounts")
    public ResponseEntity<AccountStatzResponse> accountStats() {
        AccountStatzResponse accStatz = natsService.getAccountStatz();
        return accStatz != null ? ResponseEntity.ok(accStatz) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/leafnodes")
    public ResponseEntity<LeafzResponse> leafNodes() {
        LeafzResponse leafz = natsService.getLeafz();
        return leafz != null ? ResponseEntity.ok(leafz) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/gateways")
    public ResponseEntity<GatewayzResponse> gateways() {
        GatewayzResponse gatewayz = natsService.getGatewayz();
        return gatewayz != null ? ResponseEntity.ok(gatewayz) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/metrics/rates")
    public ResponseEntity<Map<String, Object>> rates() {
        return ResponseEntity.ok(Map.of(
                "messageRates", natsService.getMessageRateHistory(),
                "byteRates", natsService.getByteRateHistory()
        ));
    }

    // --- Alert Rules API ---

    @GetMapping("/alerts/rules")
    public ResponseEntity<List<AlertRule>> listRules() {
        return ResponseEntity.ok(alertService.getAllRules());
    }

    @PostMapping("/alerts/rules")
    public ResponseEntity<AlertRule> createRule(@Valid @RequestBody AlertRuleRequest request) {
        return ResponseEntity.ok(alertService.saveRule(request.toEntity()));
    }

    @PutMapping("/alerts/rules/{id}")
    public ResponseEntity<AlertRule> updateRule(@PathVariable Long id, @Valid @RequestBody AlertRuleRequest request) {
        AlertRule rule = request.toEntity();
        rule.setId(id);
        return ResponseEntity.ok(alertService.saveRule(rule));
    }

    @DeleteMapping("/alerts/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        alertService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alerts/rules/{id}/toggle")
    public ResponseEntity<AlertRule> toggleRule(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.toggleRule(id));
    }

    @PostMapping("/alerts/rules/{id}/toggle-email")
    public ResponseEntity<AlertRule> toggleEmail(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.toggleEmailEnabled(id));
    }

    @GetMapping("/alerts/history")
    public ResponseEntity<List<?>> alertHistory(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(alertService.getRecentHistory(limit));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(404).body(Map.of(MESSAGE_KEY, ex.getMessage()));
    }
}

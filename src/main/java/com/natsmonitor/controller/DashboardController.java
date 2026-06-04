package com.natsmonitor.controller;

import com.natsmonitor.dto.*;
import com.natsmonitor.service.AlertService;
import com.natsmonitor.service.HealthDiagnosticService;
import com.natsmonitor.service.IncidentService;
import com.natsmonitor.service.NatsEventService;
import com.natsmonitor.service.NatsMonitoringService;
import com.natsmonitor.service.SnapshotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private static final String ATTR_CONNECTED = "connected";
    private static final String ATTR_FORMAT_BYTES = "formatBytes";
    private static final String ATTR_ACTIVE_PAGE = "activePage";
    private static final String ATTR_STREAMS = "streams";
    private static final String ATTR_CONNECTIONS = "connections";
    private static final String ATTR_SUBSZ = "subsz";
    private static final String ATTR_ROUTEZ = "routez";
    private static final String ATTR_ACC_STATZ = "accStatz";
    private static final String ATTR_LEAFZ = "leafz";
    private static final String ATTR_GATEWAYZ = "gatewayz";

    private final NatsMonitoringService natsService;
    private final AlertService alertService;
    private final HealthDiagnosticService healthDiagnosticService;
    private final IncidentService incidentService;
    private final NatsEventService eventService;
    private final SnapshotService snapshotService;

    public DashboardController(NatsMonitoringService natsService,
                               AlertService alertService,
                               HealthDiagnosticService healthDiagnosticService,
                               IncidentService incidentService,
                               NatsEventService eventService,
                               SnapshotService snapshotService) {
        this.natsService = natsService;
        this.alertService = alertService;
        this.healthDiagnosticService = healthDiagnosticService;
        this.incidentService = incidentService;
        this.eventService = eventService;
        this.snapshotService = snapshotService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        ServerInfo serverInfo = natsService.getServerInfo();
        JetStreamInfo jsInfo = natsService.getJetStreamInfo();
        StreamListResponse streams = natsService.getStreams();
        ConnectionsResponse connections = natsService.getConnections();
        SubszResponse subsz = natsService.getSubsz();
        RoutezResponse routez = natsService.getRoutez();
        AccountStatzResponse accStatz = natsService.getAccountStatz();
        LeafzResponse leafz = natsService.getLeafz();
        GatewayzResponse gatewayz = natsService.getGatewayz();

        model.addAttribute(ATTR_CONNECTED, serverInfo != null);
        model.addAttribute("serverInfo", serverInfo);
        model.addAttribute("jsInfo", jsInfo);
        model.addAttribute(ATTR_STREAMS, streams);
        model.addAttribute(ATTR_CONNECTIONS, connections);
        model.addAttribute(ATTR_SUBSZ, subsz);
        model.addAttribute(ATTR_ROUTEZ, routez);
        model.addAttribute(ATTR_ACC_STATZ, accStatz);
        model.addAttribute(ATTR_LEAFZ, leafz);
        model.addAttribute(ATTR_GATEWAYZ, gatewayz);
        model.addAttribute("natsUrl", natsService.getNatsUrl());
        model.addAttribute("alertCount24h", alertService.getAlertCountLast24h());
        model.addAttribute("openIncidents", incidentService.getOpenIncidents(5));
        model.addAttribute(ATTR_FORMAT_BYTES, natsService);
        model.addAttribute(ATTR_ACTIVE_PAGE, "dashboard");

        return "pages/dashboard";
    }

    @GetMapping("/streams")
    public String streams(Model model) {
        StreamListResponse streams = natsService.getStreams();
        model.addAttribute(ATTR_STREAMS, streams);
        model.addAttribute(ATTR_CONNECTED, natsService.isConnected());
        model.addAttribute(ATTR_FORMAT_BYTES, natsService);
        model.addAttribute(ATTR_ACTIVE_PAGE, "streams");
        return "pages/streams";
    }

    @GetMapping("/streams/{name}")
    public String streamDetail(@PathVariable String name, Model model) {
        StreamInfo stream = natsService.getStreamDetail(name);
        model.addAttribute("stream", stream);
        model.addAttribute(ATTR_CONNECTED, natsService.isConnected());
        model.addAttribute(ATTR_FORMAT_BYTES, natsService);
        model.addAttribute(ATTR_ACTIVE_PAGE, "streams");
        return "pages/stream-detail";
    }

    @GetMapping("/connections")
    public String connections(@RequestParam(value = "filter", required = false) String filter, Model model) {
        ConnectionsResponse connections = natsService.getConnections(
                null,
                "pending",
                "detail",
                null,
                1024,
                null
        );
        ConnectionsResponse closedConnections = natsService.getConnections("closed", "stop", "false", null, 100, null);
        RoutezResponse routez = natsService.getRoutez();
        SubszResponse subsz = natsService.getSubsz();
        AccountStatzResponse accStatz = natsService.getAccountStatz();
        LeafzResponse leafz = natsService.getLeafz();
        GatewayzResponse gatewayz = natsService.getGatewayz();
        model.addAttribute(ATTR_CONNECTIONS, connections);
        model.addAttribute("closedConnections", closedConnections);
        model.addAttribute(ATTR_ROUTEZ, routez);
        model.addAttribute(ATTR_SUBSZ, subsz);
        model.addAttribute(ATTR_ACC_STATZ, accStatz);
        model.addAttribute(ATTR_LEAFZ, leafz);
        model.addAttribute(ATTR_GATEWAYZ, gatewayz);
        model.addAttribute(ATTR_CONNECTED, natsService.isConnected());
        model.addAttribute("filter", filter != null ? filter : "");
        model.addAttribute(ATTR_FORMAT_BYTES, natsService);
        model.addAttribute(ATTR_ACTIVE_PAGE, "connections");
        return "pages/connections";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("rules", alertService.getAllRules());
        model.addAttribute("history", alertService.getRecentHistory(50));
        model.addAttribute(ATTR_CONNECTED, natsService.isConnected());
        model.addAttribute(ATTR_ACTIVE_PAGE, "alerts");
        return "pages/alerts";
    }

    @GetMapping("/diagnostic")
    public String diagnostic(Model model) {
        model.addAttribute("diagnostic", healthDiagnosticService.diagnose());
        model.addAttribute("incidents", incidentService.getOpenIncidents(20));
        model.addAttribute("events", eventService.recentEvents(20));
        model.addAttribute(ATTR_CONNECTED, natsService.isConnected());
        model.addAttribute(ATTR_ACTIVE_PAGE, "diagnostic");
        return "pages/diagnostic";
    }

    @GetMapping("/incidents")
    public String incidents(Model model) {
        model.addAttribute("incidents", incidentService.getRecentIncidents(100));
        model.addAttribute(ATTR_CONNECTED, natsService.isConnected());
        model.addAttribute(ATTR_ACTIVE_PAGE, "incidents");
        return "pages/incidents";
    }

    @GetMapping("/events")
    public String events(Model model) {
        model.addAttribute("events", eventService.recentEvents(100));
        model.addAttribute(ATTR_CONNECTED, natsService.isConnected());
        model.addAttribute(ATTR_ACTIVE_PAGE, "events");
        return "pages/events";
    }

    @GetMapping("/snapshots")
    public String snapshots(Model model) {
        model.addAttribute("serverSnapshots", snapshotService.recentServerSnapshots(100));
        model.addAttribute("consumerSnapshots", snapshotService.recentConsumerSnapshots(100));
        model.addAttribute(ATTR_CONNECTED, natsService.isConnected());
        model.addAttribute(ATTR_FORMAT_BYTES, natsService);
        model.addAttribute(ATTR_ACTIVE_PAGE, "snapshots");
        return "pages/snapshots";
    }
}

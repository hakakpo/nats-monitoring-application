package com.natsmonitor.controller;

import com.natsmonitor.dto.*;
import com.natsmonitor.service.AlertService;
import com.natsmonitor.service.NatsMonitoringService;
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

    public DashboardController(NatsMonitoringService natsService, AlertService alertService) {
        this.natsService = natsService;
        this.alertService = alertService;
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
        ConnectionsResponse connections = natsService.getConnections();
        RoutezResponse routez = natsService.getRoutez();
        SubszResponse subsz = natsService.getSubsz();
        AccountStatzResponse accStatz = natsService.getAccountStatz();
        LeafzResponse leafz = natsService.getLeafz();
        GatewayzResponse gatewayz = natsService.getGatewayz();
        model.addAttribute(ATTR_CONNECTIONS, connections);
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
}

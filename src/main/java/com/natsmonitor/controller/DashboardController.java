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

        model.addAttribute("connected", serverInfo != null);
        model.addAttribute("serverInfo", serverInfo);
        model.addAttribute("jsInfo", jsInfo);
        model.addAttribute("streams", streams);
        model.addAttribute("connections", connections);
        model.addAttribute("subsz", subsz);
        model.addAttribute("routez", routez);
        model.addAttribute("accStatz", accStatz);
        model.addAttribute("leafz", leafz);
        model.addAttribute("gatewayz", gatewayz);
        model.addAttribute("natsUrl", natsService.getNatsUrl());
        model.addAttribute("alertCount24h", alertService.getAlertCountLast24h());
        model.addAttribute("formatBytes", natsService);
        model.addAttribute("activePage", "dashboard");

        return "pages/dashboard";
    }

    @GetMapping("/streams")
    public String streams(Model model) {
        StreamListResponse streams = natsService.getStreams();
        model.addAttribute("streams", streams);
        model.addAttribute("connected", natsService.isConnected());
        model.addAttribute("formatBytes", natsService);
        model.addAttribute("activePage", "streams");
        return "pages/streams";
    }

    @GetMapping("/streams/{name}")
    public String streamDetail(@PathVariable String name, Model model) {
        StreamInfo stream = natsService.getStreamDetail(name);
        model.addAttribute("stream", stream);
        model.addAttribute("connected", natsService.isConnected());
        model.addAttribute("formatBytes", natsService);
        model.addAttribute("activePage", "streams");
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
        model.addAttribute("connections", connections);
        model.addAttribute("routez", routez);
        model.addAttribute("subsz", subsz);
        model.addAttribute("accStatz", accStatz);
        model.addAttribute("leafz", leafz);
        model.addAttribute("gatewayz", gatewayz);
        model.addAttribute("connected", natsService.isConnected());
        model.addAttribute("filter", filter != null ? filter : "");
        model.addAttribute("formatBytes", natsService);
        model.addAttribute("activePage", "connections");
        return "pages/connections";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("rules", alertService.getAllRules());
        model.addAttribute("history", alertService.getRecentHistory(50));
        model.addAttribute("connected", natsService.isConnected());
        model.addAttribute("activePage", "alerts");
        return "pages/alerts";
    }
}

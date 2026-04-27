package com.natsmonitor.controller;

import com.natsmonitor.dto.*;
import com.natsmonitor.model.AlertHistory;
import com.natsmonitor.model.AlertRule;
import com.natsmonitor.service.AlertService;
import com.natsmonitor.service.NatsMonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    private final NatsMonitoringService natsService = mock(NatsMonitoringService.class);
    private final AlertService alertService = mock(AlertService.class);
    private final DashboardController controller = new DashboardController(natsService, alertService);

    private static ServerInfo serverInfo() {
        return new ServerInfo("server-1", "n1", "2.10.29", "go1.24.2", "127.0.0.1", 4222, 1048576, 1,
                true, "1m", 4096, 1.0, 2, 3, 4, 0, 10, 20, 1024, 2048, 0, 0, 0, 65536, 8, 8, "2026-04-27T00:00:00Z", "f91ddd8");
    }

    @Test
    void shouldPopulateDashboardModel() {
        ExtendedModelMap model = new ExtendedModelMap();
        ServerInfo serverInfo = serverInfo();
        JetStreamInfo jetStreamInfo = new JetStreamInfo(128, 256, 0, 0, 1, 0, "server-1",
                "2026-04-23T10:54:26Z", 1, 2, 12, 256, 1,
                new JetStreamInfo.ApiStats(2, 0),
                new JetStreamInfo.JetStreamConfig(1024, 2048, "/data", 120000000000L));
        StreamListResponse streams = new StreamListResponse(1, 0, 10, List.of(new StreamInfo("ORDERS", null, null, null, null)));
        ConnectionsResponse connections = new ConnectionsResponse("server-1", "now", 1, 1, 0, 10, List.of());
        SubszResponse subsz = new SubszResponse(2, 1, 10, 1, 9, 99.0, 3, 1.5);
        RoutezResponse routez = new RoutezResponse("server-1", "n1", "now", 0, List.of());
        AccountStatzResponse accStatz = new AccountStatzResponse("server-1", "now", List.of());
        LeafzResponse leafz = new LeafzResponse("server-1", "now", 0, List.of());
        GatewayzResponse gatewayz = new GatewayzResponse("server-1", "now", "gw", "host", 7222, Map.of(), Map.of());
        when(natsService.getServerInfo()).thenReturn(serverInfo);
        when(natsService.getJetStreamInfo()).thenReturn(jetStreamInfo);
        when(natsService.getStreams()).thenReturn(streams);
        when(natsService.getConnections()).thenReturn(connections);
        when(natsService.getSubsz()).thenReturn(subsz);
        when(natsService.getRoutez()).thenReturn(routez);
        when(natsService.getAccountStatz()).thenReturn(accStatz);
        when(natsService.getLeafz()).thenReturn(leafz);
        when(natsService.getGatewayz()).thenReturn(gatewayz);
        when(natsService.getNatsUrl()).thenReturn("http://localhost:8222");
        when(alertService.getAlertCountLast24h()).thenReturn(5L);

        String view = controller.dashboard(model);

        assertEquals("pages/dashboard", view);
        assertEquals(true, model.getAttribute("connected"));
        assertSame(serverInfo, model.getAttribute("serverInfo"));
        assertSame(jetStreamInfo, model.getAttribute("jsInfo"));
        assertSame(streams, model.getAttribute("streams"));
        assertSame(connections, model.getAttribute("connections"));
        assertSame(subsz, model.getAttribute("subsz"));
        assertSame(routez, model.getAttribute("routez"));
        assertSame(accStatz, model.getAttribute("accStatz"));
        assertSame(leafz, model.getAttribute("leafz"));
        assertSame(gatewayz, model.getAttribute("gatewayz"));
        assertEquals("http://localhost:8222", model.getAttribute("natsUrl"));
        assertEquals(5L, model.getAttribute("alertCount24h"));
        assertSame(natsService, model.getAttribute("formatBytes"));
        assertEquals("dashboard", model.getAttribute("activePage"));
    }

    @Test
    void shouldSetConnectedFalseWhenServerInfoIsNull() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(natsService.getServerInfo()).thenReturn(null);
        when(natsService.getNatsUrl()).thenReturn("http://localhost:8222");
        when(alertService.getAlertCountLast24h()).thenReturn(0L);

        String view = controller.dashboard(model);

        assertEquals("pages/dashboard", view);
        assertEquals(false, model.getAttribute("connected"));
        assertNull(model.getAttribute("serverInfo"));
    }

    @Test
    void shouldPopulateStreamsPageModel() {
        ExtendedModelMap model = new ExtendedModelMap();
        StreamListResponse streams = new StreamListResponse(1, 0, 10, List.of(new StreamInfo("ORDERS", null, null, null, null)));
        when(natsService.getStreams()).thenReturn(streams);
        when(natsService.isConnected()).thenReturn(true);

        String view = controller.streams(model);

        assertEquals("pages/streams", view);
        assertSame(streams, model.getAttribute("streams"));
        assertEquals(true, model.getAttribute("connected"));
        assertSame(natsService, model.getAttribute("formatBytes"));
        assertEquals("streams", model.getAttribute("activePage"));
    }

    @Test
    void shouldPopulateStreamDetailPageModel() {
        ExtendedModelMap model = new ExtendedModelMap();
        StreamInfo stream = new StreamInfo("ORDERS", null, null, null, null);
        when(natsService.getStreamDetail("ORDERS")).thenReturn(stream);
        when(natsService.isConnected()).thenReturn(false);

        String view = controller.streamDetail("ORDERS", model);

        assertEquals("pages/stream-detail", view);
        assertSame(stream, model.getAttribute("stream"));
        assertEquals(false, model.getAttribute("connected"));
        assertSame(natsService, model.getAttribute("formatBytes"));
        assertEquals("streams", model.getAttribute("activePage"));
    }

    @Test
    void shouldPopulateConnectionsPageModel() {
        ExtendedModelMap model = new ExtendedModelMap();
        ConnectionsResponse connections = new ConnectionsResponse("server-1", "now", 1, 1, 0, 10, List.of());
        RoutezResponse routez = new RoutezResponse("server-1", "n1", "now", 0, List.of());
        SubszResponse subsz = new SubszResponse(2, 1, 10, 1, 9, 99.0, 3, 1.5);
        AccountStatzResponse accStatz = new AccountStatzResponse("server-1", "now", List.of());
        LeafzResponse leafz = new LeafzResponse("server-1", "now", 0, List.of());
        GatewayzResponse gatewayz = new GatewayzResponse("server-1", "now", "gw", "host", 7222, Map.of(), Map.of());
        when(natsService.getConnections()).thenReturn(connections);
        when(natsService.getRoutez()).thenReturn(routez);
        when(natsService.getSubsz()).thenReturn(subsz);
        when(natsService.getAccountStatz()).thenReturn(accStatz);
        when(natsService.getLeafz()).thenReturn(leafz);
        when(natsService.getGatewayz()).thenReturn(gatewayz);
        when(natsService.isConnected()).thenReturn(true);

        String view = controller.connections(null, model);

        assertEquals("pages/connections", view);
        assertSame(connections, model.getAttribute("connections"));
        assertSame(routez, model.getAttribute("routez"));
        assertSame(subsz, model.getAttribute("subsz"));
        assertSame(accStatz, model.getAttribute("accStatz"));
        assertSame(leafz, model.getAttribute("leafz"));
        assertSame(gatewayz, model.getAttribute("gatewayz"));
        assertEquals(true, model.getAttribute("connected"));
        assertEquals("", model.getAttribute("filter"));
        assertSame(natsService, model.getAttribute("formatBytes"));
        assertEquals("connections", model.getAttribute("activePage"));
    }

    @Test
    void shouldPopulateConnectionsPageModelWithFilter() {
        ExtendedModelMap model = new ExtendedModelMap();
        ConnectionsResponse connections = new ConnectionsResponse("server-1", "now", 1, 1, 0, 10, List.of());
        RoutezResponse routez = new RoutezResponse("server-1", "n1", "now", 0, List.of());
        SubszResponse subsz = new SubszResponse(2, 1, 10, 1, 9, 99.0, 3, 1.5);
        AccountStatzResponse accStatz = new AccountStatzResponse("server-1", "now", List.of());
        LeafzResponse leafz = new LeafzResponse("server-1", "now", 0, List.of());
        GatewayzResponse gatewayz = new GatewayzResponse("server-1", "now", "gw", "host", 7222, Map.of(), Map.of());
        when(natsService.getConnections()).thenReturn(connections);
        when(natsService.getRoutez()).thenReturn(routez);
        when(natsService.getSubsz()).thenReturn(subsz);
        when(natsService.getAccountStatz()).thenReturn(accStatz);
        when(natsService.getLeafz()).thenReturn(leafz);
        when(natsService.getGatewayz()).thenReturn(gatewayz);
        when(natsService.isConnected()).thenReturn(true);

        String view = controller.connections("slow", model);

        assertEquals("pages/connections", view);
        assertEquals("slow", model.getAttribute("filter"));
    }

    @Test
    void shouldPopulateAlertsPageModel() {
        ExtendedModelMap model = new ExtendedModelMap();
        AlertRule rule = new AlertRule();
        AlertHistory history = new AlertHistory();
        when(alertService.getAllRules()).thenReturn(List.of(rule));
        when(alertService.getRecentHistory(50)).thenReturn(List.of(history));
        when(natsService.isConnected()).thenReturn(true);

        String view = controller.alerts(model);

        assertEquals("pages/alerts", view);
        assertEquals(List.of(rule), model.getAttribute("rules"));
        assertEquals(List.of(history), model.getAttribute("history"));
        assertEquals(true, model.getAttribute("connected"));
        assertEquals("alerts", model.getAttribute("activePage"));
    }
}

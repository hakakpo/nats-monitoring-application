package com.natsmonitor.scheduler;

import com.natsmonitor.dto.ServerInfo;
import com.natsmonitor.service.AlertService;
import com.natsmonitor.service.NatsMonitoringService;
import com.natsmonitor.service.SnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MetricsScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricsScheduler.class);
    private static final String TOPIC_METRICS = "/topic/metrics";
    private static final String KEY_CONNECTED = "connected";

    private final NatsMonitoringService natsService;
    private final AlertService alertService;
    private final SnapshotService snapshotService;
    private final SimpMessagingTemplate messagingTemplate;

    public MetricsScheduler(NatsMonitoringService natsService,
                            AlertService alertService,
                            SnapshotService snapshotService,
                            SimpMessagingTemplate messagingTemplate) {
        this.natsService = natsService;
        this.alertService = alertService;
        this.snapshotService = snapshotService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelayString = "${nats.monitoring.poll-interval-seconds:5}000")
    public void pollMetrics() {
        try {
            ServerInfo info = natsService.getServerInfo();
            if (info != null) {
                natsService.updateRateMetrics(info);

                // Push real-time data to WebSocket clients
                Map<String, Object> update = new HashMap<>();
                update.put(KEY_CONNECTED, true);
                update.put("cpu", info.cpu());
                update.put("mem", info.mem());
                update.put("memFormatted", natsService.formatBytes(info.mem()));
                update.put("connections", info.connections());
                update.put("inMsgs", info.inMsgs());
                update.put("outMsgs", info.outMsgs());
                update.put("inBytes", natsService.formatBytes(info.inBytes()));
                update.put("outBytes", natsService.formatBytes(info.outBytes()));
                update.put("slowConsumers", info.slowConsumers());
                update.put("subscriptions", info.subscriptions());
                update.put("messageRateHistory", natsService.getMessageRateHistory());
                update.put("byteRateHistory", natsService.getByteRateHistory());

                messagingTemplate.convertAndSend(TOPIC_METRICS, update);
                snapshotService.capture(info, natsService.getJetStreamInfo(), natsService.getStreams(), natsService.getConnections());
            } else {
                messagingTemplate.convertAndSend(TOPIC_METRICS,
                        Map.of(KEY_CONNECTED, false));
            }
        } catch (Exception e) {
            log.debug("Metrics poll error: {}", e.getMessage());
            messagingTemplate.convertAndSend(TOPIC_METRICS,
                    Map.of(KEY_CONNECTED, false));
        }
    }

    @Scheduled(fixedDelayString = "${alerting.check-interval-seconds:30}000")
    public void checkAlerts() {
        try {
            alertService.evaluateAllRules();
        } catch (Exception e) {
            log.error("Error during alert evaluation: {}", e.getMessage());
        }
    }
}

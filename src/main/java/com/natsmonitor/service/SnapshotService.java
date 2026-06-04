package com.natsmonitor.service;

import com.natsmonitor.config.NatsMonitoringConfig;
import com.natsmonitor.dto.ConnectionsResponse;
import com.natsmonitor.dto.JetStreamInfo;
import com.natsmonitor.dto.ServerInfo;
import com.natsmonitor.dto.StreamInfo;
import com.natsmonitor.dto.StreamListResponse;
import com.natsmonitor.model.NatsConsumerSnapshot;
import com.natsmonitor.model.NatsMetricSnapshot;
import com.natsmonitor.repository.NatsConsumerSnapshotRepository;
import com.natsmonitor.repository.NatsMetricSnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SnapshotService {
    private final NatsMetricSnapshotRepository metricSnapshotRepository;
    private final NatsConsumerSnapshotRepository consumerSnapshotRepository;
    private final NatsMonitoringConfig config;

    public SnapshotService(NatsMetricSnapshotRepository metricSnapshotRepository,
                           NatsConsumerSnapshotRepository consumerSnapshotRepository,
                           NatsMonitoringConfig config) {
        this.metricSnapshotRepository = Objects.requireNonNull(metricSnapshotRepository, "metricSnapshotRepository must not be null");
        this.consumerSnapshotRepository = Objects.requireNonNull(consumerSnapshotRepository, "consumerSnapshotRepository must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    @Transactional
    public void capture(ServerInfo serverInfo, JetStreamInfo jetStreamInfo, StreamListResponse streams,
                        ConnectionsResponse connections) {
        if (serverInfo == null) {
            return;
        }

        NatsMetricSnapshot snapshot = new NatsMetricSnapshot();
        snapshot.setServerId(serverInfo.serverId());
        snapshot.setServerName(serverInfo.serverName());
        snapshot.setUptime(serverInfo.uptime());
        snapshot.setMemoryBytes(serverInfo.mem());
        snapshot.setCpu(serverInfo.cpu());
        snapshot.setConnections(serverInfo.connections());
        snapshot.setTotalConnections(serverInfo.totalConnections());
        snapshot.setSubscriptions(serverInfo.subscriptions());
        snapshot.setSlowConsumers(serverInfo.slowConsumers());
        snapshot.setInMsgs(serverInfo.inMsgs());
        snapshot.setOutMsgs(serverInfo.outMsgs());
        snapshot.setInBytes(serverInfo.inBytes());
        snapshot.setOutBytes(serverInfo.outBytes());
        snapshot.setRoutes(serverInfo.routes());
        snapshot.setLeafnodes(serverInfo.leafnodes());
        snapshot.setStreams(jetStreamInfo != null ? jetStreamInfo.streams() : 0);
        snapshot.setConsumers(jetStreamInfo != null ? jetStreamInfo.consumers() : 0);
        metricSnapshotRepository.save(snapshot);

        if (streams != null && streams.streams() != null) {
            for (StreamInfo stream : streams.streams()) {
                if (stream == null) {
                    continue;
                }
                for (var consumer : stream.safeConsumers()) {
                    NatsConsumerSnapshot consumerSnapshot = new NatsConsumerSnapshot();
                    consumerSnapshot.setStreamName(stream.name());
                    consumerSnapshot.setConsumerName(consumer.name());
                    consumerSnapshot.setDurableName(consumer.config() != null ? consumer.config().durableName() : null);
                    consumerSnapshot.setFilterSubject(consumer.config() != null ? consumer.config().filterSubject() : null);
                    consumerSnapshot.setLag(consumer.lag());
                    consumerSnapshot.setPendingMessages(consumer.numPending());
                    consumerSnapshot.setAckPending(consumer.numAckPending());
                    consumerSnapshot.setRedelivered(consumer.numRedelivered());
                    consumerSnapshot.setWaiting(consumer.numWaiting());
                    consumerSnapshotRepository.save(consumerSnapshot);
                }
            }
        }

        pruneOldSnapshots();
    }

    @Transactional(readOnly = true)
    public List<NatsMetricSnapshot> recentServerSnapshots(int limit) {
        return metricSnapshotRepository.findAllByOrderByCapturedAtDesc(PageRequest.of(0, Math.max(1, limit))).getContent();
    }

    @Transactional(readOnly = true)
    public List<NatsConsumerSnapshot> recentConsumerSnapshots(int limit) {
        return consumerSnapshotRepository.findAllByOrderByCapturedAtDesc(PageRequest.of(0, Math.max(1, limit))).getContent();
    }

    private void pruneOldSnapshots() {
        int retentionHours = Math.max(1, config.getSnapshotRetentionHours());
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
        metricSnapshotRepository.deleteByCapturedAtBefore(cutoff);
        consumerSnapshotRepository.deleteByCapturedAtBefore(cutoff);
    }
}

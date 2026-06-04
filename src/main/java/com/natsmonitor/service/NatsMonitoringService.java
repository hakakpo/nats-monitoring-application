package com.natsmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsmonitor.config.NatsMonitoringConfig;
import com.natsmonitor.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NatsMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(NatsMonitoringService.class);
    private static final String IN_RATE = "inRate";
    private static final String OUT_RATE = "outRate";

    private final RestClient restClient;
    private final NatsMonitoringConfig config;
    private final ObjectMapper objectMapper;

    // Cached metrics for historical data
    private final Map<String, List<Long>> messageRateHistory = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> byteRateHistory = new ConcurrentHashMap<>();
    private long lastInMsgs = 0;
    private long lastOutMsgs = 0;
    private long lastInBytes = 0;
    private long lastOutBytes = 0;

    public NatsMonitoringService(NatsMonitoringConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(config.getUrl())
                .build();
        messageRateHistory.put(IN_RATE, new ArrayList<>());
        messageRateHistory.put(OUT_RATE, new ArrayList<>());
        byteRateHistory.put(IN_RATE, new ArrayList<>());
        byteRateHistory.put(OUT_RATE, new ArrayList<>());
    }

    public ServerInfo getServerInfo() {
        try {
            return restClient.get()
                    .uri("/varz")
                    .retrieve()
                    .body(ServerInfo.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch server info from NATS: {}", e.getMessage());
            return null;
        }
    }

    public JetStreamInfo getJetStreamInfo() {
        try {
            String responseBody = restClient.get()
                    .uri("/jsz")
                    .retrieve()
                    .body(String.class);
            return responseBody != null ? parseJetStreamInfo(responseBody) : null;
        } catch (RestClientException | IOException e) {
            log.error("Failed to fetch JetStream info: {}", e.getMessage());
            return null;
        }
    }

    public StreamListResponse getStreams() {
        try {
            String responseBody = restClient.get()
                    .uri("/jsz?streams=true&consumers=true&config=true&raft=true")
                    .retrieve()
                    .body(String.class);
            return responseBody != null ? parseStreamsResponse(responseBody) : null;
        } catch (RestClientException | IOException e) {
            log.error("Failed to fetch streams: {}", e.getMessage());
            return null;
        }
    }

    public StreamInfo getStreamDetail(String streamName) {
        try {
            StreamListResponse response = getStreams();
            if (response != null && response.streams() != null) {
                return response.streams().stream()
                        .filter(s -> s.name().equals(streamName))
                        .findFirst()
                        .orElse(null);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch stream detail: {}", e.getMessage());
            return null;
        }
    }

    JetStreamInfo parseJetStreamInfo(String responseBody) throws IOException {
        return objectMapper.readValue(responseBody, JetStreamInfo.class);
    }

    StreamListResponse parseStreamsResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        List<StreamInfo> streams = new ArrayList<>();

        JsonNode accountDetails = root.path("account_details");
        if (accountDetails.isArray()) {
            for (JsonNode account : accountDetails) {
                JsonNode streamDetail = account.path("stream_detail");
                if (streamDetail.isArray()) {
                    for (JsonNode streamNode : streamDetail) {
                        streams.add(objectMapper.treeToValue(streamNode, StreamInfo.class));
                    }
                }
            }
        } else {
            JsonNode directStreams = root.path("streams");
            if (directStreams.isArray()) {
                for (JsonNode streamNode : directStreams) {
                    streams.add(objectMapper.treeToValue(streamNode, StreamInfo.class));
                }
            }
        }

        int total = resolveTotalStreams(root, streams.size());
        int offset = root.path("offset").asInt(0);
        int limit = root.path("limit").asInt(streams.isEmpty() ? total : streams.size());
        return new StreamListResponse(total, offset, limit, List.copyOf(streams));
    }

    private int resolveTotalStreams(JsonNode root, int parsedStreamCount) {
        if (root.has("total_streams")) {
            return root.path("total_streams").asInt(parsedStreamCount);
        }
        JsonNode streamsNode = root.path("streams");
        if (streamsNode.isIntegralNumber()) {
            return streamsNode.asInt(parsedStreamCount);
        }
        if (root.has("total") && parsedStreamCount == 0) {
            return root.path("total").asInt(0);
        }
        return parsedStreamCount;
    }

    public ConnectionsResponse getConnections() {
        try {
            String responseBody = restClient.get()
                    .uri("/connz?subs=true")
                    .retrieve()
                    .body(String.class);
            return responseBody != null ? parseConnectionsResponse(responseBody) : null;
        } catch (RestClientException | IOException e) {
            log.error("Failed to fetch connections: {}", e.getMessage());
            return null;
        }
    }

    public ConnectionsResponse getConnections(String state,
                                              String sort,
                                              String subs,
                                              Integer offset,
                                              Integer limit,
                                              Long cid) {
        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/connz");
                        if (state != null && !state.isBlank()) builder.queryParam("state", state);
                        if (sort != null && !sort.isBlank()) builder.queryParam("sort", sort);
                        builder.queryParam("subs", subs == null || subs.isBlank() ? "true" : subs);
                        if (offset != null) builder.queryParam("offset", offset);
                        if (limit != null) builder.queryParam("limit", limit);
                        if (cid != null) builder.queryParam("cid", cid);
                        return builder.build();
                    })
                    .retrieve()
                    .body(String.class);
            return responseBody != null ? parseConnectionsResponse(responseBody) : null;
        } catch (RestClientException | IOException e) {
            log.error("Failed to fetch filtered connections: {}", e.getMessage());
            return null;
        }
    }

    ConnectionsResponse parseConnectionsResponse(String responseBody) throws IOException {
        return objectMapper.readValue(responseBody, ConnectionsResponse.class);
    }

    public SubszResponse getSubsz() {
        try {
            return restClient.get()
                    .uri("/subsz")
                    .retrieve()
                    .body(SubszResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch subscriptions: {}", e.getMessage());
            return null;
        }
    }

    public RoutezResponse getRoutez() {
        try {
            String responseBody = restClient.get()
                    .uri("/routez")
                    .retrieve()
                    .body(String.class);
            return responseBody != null ? parseRoutezResponse(responseBody) : null;
        } catch (RestClientException | IOException e) {
            log.error("Failed to fetch routes: {}", e.getMessage());
            return null;
        }
    }

    RoutezResponse parseRoutezResponse(String responseBody) throws IOException {
        return objectMapper.readValue(responseBody, RoutezResponse.class);
    }

    public AccountStatzResponse getAccountStatz() {
        try {
            return restClient.get()
                    .uri("/accstatz")
                    .retrieve()
                    .body(AccountStatzResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch account stats: {}", e.getMessage());
            return null;
        }
    }

    public LeafzResponse getLeafz() {
        try {
            return restClient.get()
                    .uri("/leafz")
                    .retrieve()
                    .body(LeafzResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch leaf nodes: {}", e.getMessage());
            return null;
        }
    }

    public GatewayzResponse getGatewayz() {
        try {
            return restClient.get()
                    .uri("/gatewayz")
                    .retrieve()
                    .body(GatewayzResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch gateways: {}", e.getMessage());
            return null;
        }
    }

    public boolean isConnected() {
        return checkHealth(false).success();
    }

    public HealthProbe checkHealth(boolean requireJetStream) {
        Instant start = Instant.now();
        try {
            String uri = requireJetStream ? "/healthz?js-enabled-only=true" : "/healthz";
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            return new HealthProbe(true, elapsedMillis(start), body, null);
        } catch (Exception e) {
            return new HealthProbe(false, elapsedMillis(start), null, e.getMessage());
        }
    }

    private long elapsedMillis(Instant start) {
        return Math.max(0, Duration.between(start, Instant.now()).toMillis());
    }

    public void updateRateMetrics(ServerInfo info) {
        if (info == null) return;

        long inMsgRate = info.inMsgs() - lastInMsgs;
        long outMsgRate = info.outMsgs() - lastOutMsgs;
        long inByteRate = info.inBytes() - lastInBytes;
        long outByteRate = info.outBytes() - lastOutBytes;

        if (lastInMsgs > 0) {
            addToHistory(messageRateHistory.get(IN_RATE), Math.max(0, inMsgRate));
            addToHistory(messageRateHistory.get(OUT_RATE), Math.max(0, outMsgRate));
            addToHistory(byteRateHistory.get(IN_RATE), Math.max(0, inByteRate));
            addToHistory(byteRateHistory.get(OUT_RATE), Math.max(0, outByteRate));
        }

        lastInMsgs = info.inMsgs();
        lastOutMsgs = info.outMsgs();
        lastInBytes = info.inBytes();
        lastOutBytes = info.outBytes();
    }

    private void addToHistory(List<Long> history, long value) {
        history.add(value);
        if (history.size() > 60) {
            history.removeFirst();
        }
    }

    public Map<String, List<Long>> getMessageRateHistory() {
        return Collections.unmodifiableMap(messageRateHistory);
    }

    public Map<String, List<Long>> getByteRateHistory() {
        return Collections.unmodifiableMap(byteRateHistory);
    }

    public String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public String getNatsUrl() {
        return config.getUrl();
    }
}

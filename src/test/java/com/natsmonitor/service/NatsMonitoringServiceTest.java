package com.natsmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsmonitor.config.NatsMonitoringConfig;
import com.natsmonitor.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NatsMonitoringServiceTest {

    private final NatsMonitoringConfig config = new NatsMonitoringConfig();
    private final NatsMonitoringService service = new NatsMonitoringService(config, new ObjectMapper());

    private static NatsMonitoringService serviceWithRestClient(RestClient restClient) {
        NatsMonitoringService service = new NatsMonitoringService(new NatsMonitoringConfig(), new ObjectMapper());
        ReflectionTestUtils.setField(service, "restClient", restClient);
        return service;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RestClient.RequestHeadersSpec<?> stubUri(RestClient.RequestHeadersUriSpec<?> requestSpec, String uri) {
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        when(((RestClient.RequestHeadersUriSpec) requestSpec).uri(uri)).thenReturn(headersSpec);
        return headersSpec;
    }

    private static RestClient.ResponseSpec stubRetrieve(RestClient.RequestHeadersSpec<?> headersSpec) {
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        return responseSpec;
    }

    @Test
    void shouldDeserializeBasicJszPayload() throws Exception {
        String json = """
                {
                  "memory": 0,
                  "storage": 0,
                  "reserved_memory": 0,
                  "reserved_storage": 10737418240,
                  "accounts": 1,
                  "ha_assets": 0,
                  "api": {
                    "total": 2,
                    "errors": 0
                  },
                  "server_id": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3",
                  "now": "2026-04-23T10:54:26.789568672Z",
                  "config": {
                    "max_memory": 12309399552,
                    "max_storage": 692495846400,
                    "store_dir": "/data/jetstream",
                    "sync_interval": 120000000000
                  },
                  "streams": 1,
                  "consumers": 0,
                  "messages": 0,
                  "bytes": 0,
                  "total": 1
                }
                """;

        JetStreamInfo info = service.parseJetStreamInfo(json);

        assertNotNull(info);
        assertEquals(1, info.streams());
        assertEquals(0, info.consumers());
        assertEquals(2, info.api().total());
        assertEquals(120000000000L, info.config().syncInterval());
    }

    @Test
    void shouldFlattenDetailedJszStreamsFromAccountDetails() throws Exception {
        String json = """
                {
                  "server_id": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3",
                  "now": "2026-04-23T10:54:26.789568672Z",
                  "memory": 0,
                  "storage": 0,
                  "total_streams": 1,
                  "account_details": [
                    {
                      "name": "$G",
                      "stream_detail": [
                        {
                          "name": "ORDERS",
                          "config": {
                            "name": "ORDERS",
                            "subjects": ["orders.created"],
                            "retention": "limits",
                            "max_consumers": -1,
                            "max_msgs": -1,
                            "max_bytes": -1,
                            "max_age": 0,
                            "storage": "file",
                            "num_replicas": 1,
                            "discard": "old",
                            "max_msg_size": -1
                          },
                          "state": {
                            "messages": 12,
                            "bytes": 2048,
                            "first_seq": 1,
                            "last_seq": 12,
                            "consumer_count": 0,
                            "first_ts": "2026-04-23T10:00:00Z",
                            "last_ts": "2026-04-23T10:54:00Z",
                            "num_subjects": 1,
                            "num_deleted": 0
                          },
                          "created": "2026-04-23T09:00:00Z"
                        }
                      ]
                    }
                  ]
                }
                """;

        StreamListResponse response = service.parseStreamsResponse(json);

        assertNotNull(response);
        assertEquals(1, response.total());
        assertEquals(1, response.streams().size());
        assertEquals("ORDERS", response.streams().getFirst().name());
        assertEquals(12, response.streams().getFirst().state().messages());
    }

    @Test
    void shouldDeserializeJszStreamsPayloadWithoutConfigBlock() throws Exception {
        String json = """
                {
                  "memory": 0,
                  "storage": 0,
                  "reserved_memory": 0,
                  "reserved_storage": 10737418240,
                  "accounts": 1,
                  "ha_assets": 0,
                  "api": {
                    "total": 2,
                    "errors": 0
                  },
                  "server_id": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3",
                  "now": "2026-04-23T10:58:48.674915198Z",
                  "config": {
                    "max_memory": 12309399552,
                    "max_storage": 692495846400,
                    "store_dir": "/data/jetstream",
                    "sync_interval": 120000000000
                  },
                  "streams": 1,
                  "consumers": 0,
                  "messages": 0,
                  "bytes": 0,
                  "account_details": [
                    {
                      "name": "$G",
                      "id": "$G",
                      "memory": 0,
                      "storage": 0,
                      "reserved_memory": 18446744073709552000,
                      "reserved_storage": 18446744073709552000,
                      "accounts": 0,
                      "ha_assets": 0,
                      "api": {
                        "total": 2,
                        "errors": 0
                      },
                      "stream_detail": [
                        {
                          "name": "EMAILS",
                          "created": "2026-01-28T08:02:31.688032045Z",
                          "cluster": {
                            "leader": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3"
                          },
                          "state": {
                            "messages": 0,
                            "bytes": 0,
                            "first_seq": 0,
                            "first_ts": "0001-01-01T00:00:00Z",
                            "last_seq": 0,
                            "last_ts": "0001-01-01T00:00:00Z",
                            "consumer_count": 0
                          }
                        }
                      ]
                    }
                  ],
                  "total": 1
                }
                """;

        StreamListResponse response = service.parseStreamsResponse(json);

        assertNotNull(response);
        assertEquals(1, response.total());
        assertEquals(1, response.streams().size());
        assertEquals("EMAILS", response.streams().getFirst().name());
        assertEquals(0, response.streams().getFirst().safeState().consumerCount());
        assertEquals(0, response.streams().getFirst().safeState().numSubjects());
        assertEquals(0, response.streams().getFirst().safeConfig().maxConsumers());
    }

    @Test
    void shouldDeserializeConnzPayload() throws Exception {
        String json = """
                {
                  "server_id": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3",
                  "now": "2026-04-23T10:56:15.286008938Z",
                  "num_connections": 1,
                  "total": 1,
                  "offset": 0,
                  "limit": 1024,
                  "connections": [
                    {
                      "cid": 10,
                      "kind": "Client",
                      "type": "nats",
                      "ip": "172.27.0.1",
                      "port": 42028,
                      "start": "2026-04-23T08:19:42.953547565Z",
                      "last_activity": "2026-04-23T08:19:43.64349112Z",
                      "rtt": "2.351424ms",
                      "uptime": "2h36m32s",
                      "idle": "2h36m31s",
                      "pending_bytes": 0,
                      "in_msgs": 1,
                      "out_msgs": 1,
                      "in_bytes": 12,
                      "out_bytes": 855,
                      "subscriptions": 1,
                      "name": "email-subscriber",
                      "lang": "java",
                      "version": "2.20.4"
                    }
                  ]
                }
                """;

        ConnectionsResponse response = service.parseConnectionsResponse(json);

        assertNotNull(response);
        assertEquals(1, response.numConnections());
        assertEquals(1, response.connections().size());
        assertEquals(10, response.connections().getFirst().cid());
        assertEquals("email-subscriber", response.connections().getFirst().name());
        assertEquals("2026-04-23T08:19:42.953547565Z", response.connections().getFirst().start());
        assertEquals(0, response.connections().getFirst().pendingBytes());
    }

    @Test
    void shouldDeserializeConnzPayloadWithSubscriptionsList() throws Exception {
        String json = """
                {
                  "server_id": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3",
                  "now": "2026-04-23T11:00:14.90493228Z",
                  "num_connections": 1,
                  "total": 1,
                  "offset": 0,
                  "limit": 1024,
                  "connections": [
                    {
                      "cid": 10,
                      "kind": "Client",
                      "type": "nats",
                      "ip": "172.27.0.1",
                      "port": 42028,
                      "start": "2026-04-23T08:19:42.953547565Z",
                      "last_activity": "2026-04-23T08:19:43.64349112Z",
                      "rtt": "2.351424ms",
                      "uptime": "2h40m31s",
                      "idle": "2h40m31s",
                      "pending_bytes": 0,
                      "in_msgs": 1,
                      "out_msgs": 1,
                      "in_bytes": 12,
                      "out_bytes": 855,
                      "subscriptions": 1,
                      "name": "email-subscriber",
                      "lang": "java",
                      "version": "2.20.4",
                      "subscriptions_list": [
                        "_INBOX.ycVp0PGVyVnrozecUNdLi8.*"
                      ]
                    }
                  ]
                }
                """;

        ConnectionsResponse response = service.parseConnectionsResponse(json);

        assertNotNull(response);
        assertEquals(1, response.connections().getFirst().subscriptions());
        assertEquals(1, response.connections().getFirst().subscriptionsList().size());
        assertEquals("_INBOX.ycVp0PGVyVnrozecUNdLi8.*", response.connections().getFirst().subscriptionsList().getFirst());
    }

    @Test
    void shouldDeserializeRoutezPayload() throws Exception {
        String json = """
                {
                  "server_id": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3",
                  "server_name": "",
                  "now": "2026-04-23T11:13:59.405860186Z",
                  "num_routes": 0,
                  "routes": []
                }
                """;

        RoutezResponse response = service.parseRoutezResponse(json);

        assertNotNull(response);
        assertEquals("NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3", response.serverId());
        assertEquals(0, response.numRoutes());
        assertEquals(0, response.routes().size());
    }

    @Test
    void shouldParseDirectStreamsPayloadAndResolveMetadata() throws Exception {
        String json = """
                {
                  "streams": [
                    {
                      "name": "PAYMENTS",
                      "state": {
                        "messages": 7,
                        "bytes": 512,
                        "first_seq": 1,
                        "last_seq": 7,
                        "consumer_count": 1
                      }
                    }
                  ],
                  "offset": 2,
                  "limit": 5
                }
                """;

        StreamListResponse response = service.parseStreamsResponse(json);

        assertNotNull(response);
        assertEquals(1, response.total());
        assertEquals(2, response.offset());
        assertEquals(5, response.limit());
        assertEquals("PAYMENTS", response.streams().getFirst().name());
    }

    @Test
    void shouldUseNumericStreamsAsTotalWhenDirectStreamArrayIsAbsent() throws Exception {
        String json = """
                {
                  "streams": 4,
                  "offset": 0,
                  "limit": 10
                }
                """;

        StreamListResponse response = service.parseStreamsResponse(json);

        assertNotNull(response);
        assertEquals(4, response.total());
        assertTrue(response.streams().isEmpty());
    }

    @Test
    void shouldReturnStreamDetailWhenPresent() {
        NatsMonitoringService detailService = new NatsMonitoringService(config, new ObjectMapper()) {
            @Override
            public StreamListResponse getStreams() {
                return new StreamListResponse(2, 0, 2, List.of(
                        new StreamInfo("ORDERS", null, null, null),
                        new StreamInfo("PAYMENTS", null, null, null)
                ));
            }
        };

        StreamInfo stream = detailService.getStreamDetail("PAYMENTS");

        assertNotNull(stream);
        assertEquals("PAYMENTS", stream.name());
        assertNull(detailService.getStreamDetail("MISSING"));
    }

    @Test
    void shouldTrackRateHistoryAndCapItAtSixtyEntries() {
        ServerInfo baseline = new ServerInfo("id", "name", "1.0", "go", "localhost", 4222, 0, 1,
                true, "1m", 1024, 1.0, 1, 1, 1, 0, 100, 200, 1024, 2048, 0, 0, 0);
        service.updateRateMetrics(baseline);

        for (int index = 1; index <= 61; index++) {
            ServerInfo info = new ServerInfo("id", "name", "1.0", "go", "localhost", 4222, 0, 1,
                    true, "1m", 1024, 1.0, 1, 1, 1, 0, 100 + index, 200 + index,
                    1024 + index, 2048 + index, 0, 0, 0);
            service.updateRateMetrics(info);
        }

        ServerInfo decreased = new ServerInfo("id", "name", "1.0", "go", "localhost", 4222, 0, 1,
                true, "1m", 1024, 1.0, 1, 1, 1, 0, 120, 220, 1000, 2000, 0, 0, 0);
        service.updateRateMetrics(decreased);

        assertEquals(60, service.getMessageRateHistory().get("inRate").size());
        assertEquals(60, service.getByteRateHistory().get("outRate").size());
        assertEquals(1L, service.getMessageRateHistory().get("inRate").getFirst());
        assertEquals(0L, service.getMessageRateHistory().get("inRate").getLast());
        assertEquals(0L, service.getByteRateHistory().get("outRate").getLast());
    }

    @Test
    void shouldFormatBytesAndExposeConfiguredUrl() {
        assertEquals("512 B", service.formatBytes(512));
        assertEquals(String.format(Locale.getDefault(), "%.1f KB", 1.0), service.formatBytes(1024));
        assertEquals(String.format(Locale.getDefault(), "%.1f MB", 1.0), service.formatBytes(1024 * 1024));
        assertEquals(String.format(Locale.getDefault(), "%.2f GB", 1.0), service.formatBytes(1024L * 1024 * 1024));
        assertEquals("http://localhost:8222", service.getNatsUrl());
    }

    @Test
    void shouldFetchMetricsFromConfiguredEndpoints() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec<?> requestSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestSpec);

        RestClient.RequestHeadersSpec<?> varzHeaders = stubUri(requestSpec, "/varz");
        RestClient.ResponseSpec varzResponse = stubRetrieve(varzHeaders);
        when(varzResponse.body(ServerInfo.class)).thenReturn(new ServerInfo(
                "server-1", "n1", "2.10.29", "go1.24.2", "127.0.0.1", 4222, 1048576, 1,
                true, "1m", 4096, 1.5, 3, 5, 8, 1, 10, 20, 1024, 2048, 0, 0, 0
        ));

        RestClient.RequestHeadersSpec<?> jszHeaders = stubUri(requestSpec, "/jsz");
        RestClient.ResponseSpec jszResponse = stubRetrieve(jszHeaders);
        when(jszResponse.body(String.class)).thenReturn("""
                {
                  "memory": 128,
                  "storage": 256,
                  "reserved_memory": 0,
                  "reserved_storage": 0,
                  "accounts": 1,
                  "ha_assets": 0,
                  "server_id": "server-1",
                  "now": "2026-04-23T10:54:26.789568672Z",
                  "streams": 1,
                  "consumers": 2,
                  "messages": 12,
                  "bytes": 256,
                  "total": 1,
                  "api": {
                    "total": 2,
                    "errors": 0
                  },
                  "config": {
                    "max_memory": 1024,
                    "max_storage": 2048,
                    "store_dir": "/data/jetstream",
                    "sync_interval": 120000000000
                  }
                }
                """);

        RestClient.RequestHeadersSpec<?> streamsHeaders = stubUri(requestSpec, "/jsz?streams=true&consumers=true");
        RestClient.ResponseSpec streamsResponse = stubRetrieve(streamsHeaders);
        when(streamsResponse.body(String.class)).thenReturn("""
                {
                  "total_streams": 1,
                  "account_details": [
                    {
                      "stream_detail": [
                        {
                          "name": "ORDERS",
                          "state": {
                            "messages": 12,
                            "bytes": 256,
                            "first_seq": 1,
                            "last_seq": 12,
                            "consumer_count": 2
                          }
                        }
                      ]
                    }
                  ]
                }
                """);

        RestClient.RequestHeadersSpec<?> connzHeaders = stubUri(requestSpec, "/connz?subs=true");
        RestClient.ResponseSpec connzResponse = stubRetrieve(connzHeaders);
        when(connzResponse.body(String.class)).thenReturn("""
                {
                  "server_id": "server-1",
                  "now": "2026-04-23T10:56:15.286008938Z",
                  "num_connections": 1,
                  "total": 1,
                  "offset": 0,
                  "limit": 1024,
                  "connections": [
                    {
                      "cid": 10,
                      "kind": "Client",
                      "type": "nats",
                      "ip": "127.0.0.1",
                      "port": 42028,
                      "subscriptions": 1,
                      "name": "email-subscriber"
                    }
                  ]
                }
                """);

        RestClient.RequestHeadersSpec<?> subszHeaders = stubUri(requestSpec, "/subsz");
        RestClient.ResponseSpec subszResponse = stubRetrieve(subszHeaders);
        when(subszResponse.body(com.natsmonitor.dto.SubszResponse.class)).thenReturn(
                new com.natsmonitor.dto.SubszResponse(2, 1, 10, 2, 8, 90.0, 3, 1.2)
        );

        RestClient.RequestHeadersSpec<?> routezHeaders = stubUri(requestSpec, "/routez");
        RestClient.ResponseSpec routezResponse = stubRetrieve(routezHeaders);
        when(routezResponse.body(String.class)).thenReturn("""
                {
                  "server_id": "server-1",
                  "server_name": "n1",
                  "now": "2026-04-23T11:13:59.405860186Z",
                  "num_routes": 0,
                  "routes": []
                }
                """);

        RestClient.RequestHeadersSpec<?> healthHeaders = stubUri(requestSpec, "/healthz");
        RestClient.ResponseSpec healthResponse = stubRetrieve(healthHeaders);
        when(healthResponse.body(String.class)).thenReturn("ok");

        NatsMonitoringService httpService = serviceWithRestClient(restClient);

        assertTrue(httpService.isConnected());
        assertEquals("server-1", httpService.getServerInfo().serverId());
        assertEquals(2, httpService.getJetStreamInfo().consumers());
        assertEquals(1, httpService.getStreams().total());
        assertEquals("ORDERS", httpService.getStreamDetail("ORDERS").name());
        assertEquals(1, httpService.getConnections().numConnections());
        assertEquals(2, httpService.getSubsz().numSubscriptions());
        assertEquals(0, httpService.getRoutez().numRoutes());
    }

    @Test
    void shouldReturnNullAndDisconnectedWhenEndpointsFail() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec<?> requestSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestSpec);

        RestClient.ResponseSpec varzResponse = stubRetrieve(stubUri(requestSpec, "/varz"));
        when(varzResponse.body(ServerInfo.class)).thenThrow(new RestClientException("boom"));

        RestClient.ResponseSpec jszResponse = stubRetrieve(stubUri(requestSpec, "/jsz"));
        when(jszResponse.body(String.class)).thenThrow(new RestClientException("boom"));

        RestClient.ResponseSpec streamsResponse = stubRetrieve(stubUri(requestSpec, "/jsz?streams=true&consumers=true"));
        when(streamsResponse.body(String.class)).thenThrow(new RestClientException("boom"));

        RestClient.ResponseSpec connzResponse = stubRetrieve(stubUri(requestSpec, "/connz?subs=true"));
        when(connzResponse.body(String.class)).thenThrow(new RestClientException("boom"));

        RestClient.ResponseSpec subszResponse = stubRetrieve(stubUri(requestSpec, "/subsz"));
        when(subszResponse.body(com.natsmonitor.dto.SubszResponse.class)).thenThrow(new RestClientException("boom"));

        RestClient.ResponseSpec routezResponse = stubRetrieve(stubUri(requestSpec, "/routez"));
        when(routezResponse.body(String.class)).thenThrow(new RestClientException("boom"));

        RestClient.ResponseSpec healthResponse = stubRetrieve(stubUri(requestSpec, "/healthz"));
        when(healthResponse.body(String.class)).thenThrow(new RuntimeException("boom"));

        NatsMonitoringService httpService = serviceWithRestClient(restClient);

        assertNull(httpService.getServerInfo());
        assertNull(httpService.getJetStreamInfo());
        assertNull(httpService.getStreams());
        assertNull(httpService.getConnections());
        assertNull(httpService.getSubsz());
        assertNull(httpService.getRoutez());
        assertFalse(httpService.isConnected());
    }
}

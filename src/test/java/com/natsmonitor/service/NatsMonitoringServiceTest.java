package com.natsmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsmonitor.config.NatsMonitoringConfig;
import com.natsmonitor.dto.ConnectionsResponse;
import com.natsmonitor.dto.JetStreamInfo;
import com.natsmonitor.dto.RoutezResponse;
import com.natsmonitor.dto.StreamListResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NatsMonitoringServiceTest {

    private final NatsMonitoringConfig config = new NatsMonitoringConfig();
    private final NatsMonitoringService service = new NatsMonitoringService(config, new ObjectMapper());

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
}

package com.natsmonitor.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerInfoDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeVarzWhenJetstreamIsAnObject() throws Exception {
        String json = """
                {
                  "server_id": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3",
                  "server_name": "NDHC6X6N7TB4NMBPZDPUF4EL2QVMOKREDOADDC273KGLQXACCOUUMSY3",
                  "version": "2.10.29",
                  "proto": 1,
                  "go": "go1.24.2",
                  "host": "0.0.0.0",
                  "port": 4222,
                  "max_payload": 1048576,
                  "jetstream": {
                    "config": {
                      "max_memory": 12309399552,
                      "max_storage": 692495846400
                    },
                    "stats": {
                      "memory": 0,
                      "storage": 0
                    }
                  },
                  "uptime": "17m59s",
                  "mem": 16470016,
                  "cpu": 1,
                  "connections": 1,
                  "total_connections": 3,
                  "routes": 0,
                  "remotes": 0,
                  "leafnodes": 0,
                  "in_msgs": 2,
                  "out_msgs": 2,
                  "in_bytes": 12,
                  "out_bytes": 1710,
                  "slow_consumers": 0,
                  "subscriptions": 66
                }
                """;

        ServerInfo serverInfo = objectMapper.readValue(json, ServerInfo.class);

        assertTrue(serverInfo.jetstream());
        assertEquals("2.10.29", serverInfo.version());
        assertEquals(1, serverInfo.connections());
        assertEquals(66, serverInfo.subscriptions());
    }
}


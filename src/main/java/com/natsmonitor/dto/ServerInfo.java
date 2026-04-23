package com.natsmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerInfo(
    @JsonProperty("server_id") String serverId,
    @JsonProperty("server_name") String serverName,
    @JsonProperty("version") String version,
    @JsonProperty("go") String goVersion,
    @JsonProperty("host") String host,
    @JsonProperty("port") int port,
    @JsonProperty("max_payload") long maxPayload,
    @JsonProperty("proto") int proto,
    @JsonDeserialize(using = JetStreamEnabledDeserializer.class)
    @JsonProperty("jetstream") boolean jetstream,
    @JsonProperty("uptime") String uptime,
    @JsonProperty("mem") long mem,
    @JsonProperty("cpu") double cpu,
    @JsonProperty("connections") int connections,
    @JsonProperty("total_connections") long totalConnections,
    @JsonProperty("subscriptions") long subscriptions,
    @JsonProperty("slow_consumers") long slowConsumers,
    @JsonProperty("in_msgs") long inMsgs,
    @JsonProperty("out_msgs") long outMsgs,
    @JsonProperty("in_bytes") long inBytes,
    @JsonProperty("out_bytes") long outBytes,
    @JsonProperty("routes") int routes,
    @JsonProperty("remotes") int remotes,
    @JsonProperty("leafnodes") int leafnodes
) {
    static class JetStreamEnabledDeserializer extends JsonDeserializer<Boolean> {
        @Override
        public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();

            if (token == JsonToken.VALUE_TRUE) {
                return true;
            }
            if (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_NULL) {
                return false;
            }
            if (token == JsonToken.START_OBJECT) {
                parser.skipChildren();
                return true;
            }

            return Boolean.parseBoolean(parser.getValueAsString());
        }
    }
}

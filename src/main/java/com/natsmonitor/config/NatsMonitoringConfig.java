package com.natsmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nats.monitoring")
public class NatsMonitoringConfig {

    private String url = "http://localhost:8222";
    private int pollIntervalSeconds = 5;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(int pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; }
}

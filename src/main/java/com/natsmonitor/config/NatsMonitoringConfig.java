package com.natsmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nats.monitoring")
public class NatsMonitoringConfig {

    private String url = "http://localhost:8222";
    private String serverUrl = "nats://localhost:4222";
    private String username;
    private String password;
    private boolean systemEventsEnabled = false;
    private int pollIntervalSeconds = 5;
    private int snapshotRetentionHours = 120;
    private int historyRetentionDays = 5;
    private int cleanupIntervalHours = 6;
    private NatsTlsConfig tls = new NatsTlsConfig();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSystemEventsEnabled() {
        return systemEventsEnabled;
    }

    public void setSystemEventsEnabled(boolean systemEventsEnabled) {
        this.systemEventsEnabled = systemEventsEnabled;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public int getSnapshotRetentionHours() {
        return snapshotRetentionHours;
    }

    public void setSnapshotRetentionHours(int snapshotRetentionHours) {
        this.snapshotRetentionHours = snapshotRetentionHours;
    }

    public int getHistoryRetentionDays() {
        return historyRetentionDays;
    }

    public void setHistoryRetentionDays(int historyRetentionDays) {
        this.historyRetentionDays = historyRetentionDays;
    }

    public int getCleanupIntervalHours() {
        return cleanupIntervalHours;
    }

    public void setCleanupIntervalHours(int cleanupIntervalHours) {
        this.cleanupIntervalHours = cleanupIntervalHours;
    }

    public NatsTlsConfig getTls() {
        return tls;
    }

    public void setTls(NatsTlsConfig tls) {
        this.tls = tls;
    }
}

package com.natsmonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "nats_metric_snapshots")
public class NatsMetricSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime capturedAt = LocalDateTime.now();

    private String serverId;
    private String serverName;
    private String uptime;
    private long memoryBytes;
    private double cpu;
    private int connections;
    private long totalConnections;
    private long subscriptions;
    private long slowConsumers;
    private long inMsgs;
    private long outMsgs;
    private long inBytes;
    private long outBytes;
    private int routes;
    private int leafnodes;
    private int streams;
    private int consumers;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getUptime() { return uptime; }
    public void setUptime(String uptime) { this.uptime = uptime; }
    public long getMemoryBytes() { return memoryBytes; }
    public void setMemoryBytes(long memoryBytes) { this.memoryBytes = memoryBytes; }
    public double getCpu() { return cpu; }
    public void setCpu(double cpu) { this.cpu = cpu; }
    public int getConnections() { return connections; }
    public void setConnections(int connections) { this.connections = connections; }
    public long getTotalConnections() { return totalConnections; }
    public void setTotalConnections(long totalConnections) { this.totalConnections = totalConnections; }
    public long getSubscriptions() { return subscriptions; }
    public void setSubscriptions(long subscriptions) { this.subscriptions = subscriptions; }
    public long getSlowConsumers() { return slowConsumers; }
    public void setSlowConsumers(long slowConsumers) { this.slowConsumers = slowConsumers; }
    public long getInMsgs() { return inMsgs; }
    public void setInMsgs(long inMsgs) { this.inMsgs = inMsgs; }
    public long getOutMsgs() { return outMsgs; }
    public void setOutMsgs(long outMsgs) { this.outMsgs = outMsgs; }
    public long getInBytes() { return inBytes; }
    public void setInBytes(long inBytes) { this.inBytes = inBytes; }
    public long getOutBytes() { return outBytes; }
    public void setOutBytes(long outBytes) { this.outBytes = outBytes; }
    public int getRoutes() { return routes; }
    public void setRoutes(int routes) { this.routes = routes; }
    public int getLeafnodes() { return leafnodes; }
    public void setLeafnodes(int leafnodes) { this.leafnodes = leafnodes; }
    public int getStreams() { return streams; }
    public void setStreams(int streams) { this.streams = streams; }
    public int getConsumers() { return consumers; }
    public void setConsumers(int consumers) { this.consumers = consumers; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NatsMetricSnapshot snapshot)) return false;
        return id != null && Objects.equals(id, snapshot.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}

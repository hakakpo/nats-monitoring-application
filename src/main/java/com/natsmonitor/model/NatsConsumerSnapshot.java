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
@Table(name = "nats_consumer_snapshots")
public class NatsConsumerSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime capturedAt = LocalDateTime.now();

    private String streamName;
    private String consumerName;
    private String durableName;
    private String filterSubject;
    private long lag;
    private long pendingMessages;
    private long ackPending;
    private long redelivered;
    private long waiting;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
    public String getStreamName() { return streamName; }
    public void setStreamName(String streamName) { this.streamName = streamName; }
    public String getConsumerName() { return consumerName; }
    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
    public String getDurableName() { return durableName; }
    public void setDurableName(String durableName) { this.durableName = durableName; }
    public String getFilterSubject() { return filterSubject; }
    public void setFilterSubject(String filterSubject) { this.filterSubject = filterSubject; }
    public long getLag() { return lag; }
    public void setLag(long lag) { this.lag = lag; }
    public long getPendingMessages() { return pendingMessages; }
    public void setPendingMessages(long pendingMessages) { this.pendingMessages = pendingMessages; }
    public long getAckPending() { return ackPending; }
    public void setAckPending(long ackPending) { this.ackPending = ackPending; }
    public long getRedelivered() { return redelivered; }
    public void setRedelivered(long redelivered) { this.redelivered = redelivered; }
    public long getWaiting() { return waiting; }
    public void setWaiting(long waiting) { this.waiting = waiting; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NatsConsumerSnapshot snapshot)) return false;
        return id != null && Objects.equals(id, snapshot.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}

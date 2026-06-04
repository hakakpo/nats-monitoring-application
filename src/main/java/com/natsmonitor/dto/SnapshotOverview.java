package com.natsmonitor.dto;

import com.natsmonitor.model.NatsConsumerSnapshot;
import com.natsmonitor.model.NatsMetricSnapshot;

import java.util.List;

public record SnapshotOverview(
        List<NatsMetricSnapshot> serverSnapshots,
        List<NatsConsumerSnapshot> consumerSnapshots
) {
}

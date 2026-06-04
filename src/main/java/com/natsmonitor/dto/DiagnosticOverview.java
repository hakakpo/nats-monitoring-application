package com.natsmonitor.dto;

import com.natsmonitor.model.Incident;
import com.natsmonitor.model.NatsEvent;
import com.natsmonitor.model.NatsMetricSnapshot;

import java.util.List;

public record DiagnosticOverview(
        HealthDiagnostic health,
        List<Incident> openIncidents,
        List<NatsEvent> recentEvents,
        List<NatsMetricSnapshot> recentSnapshots
) {
}

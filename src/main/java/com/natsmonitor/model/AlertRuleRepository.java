package com.natsmonitor.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByEnabledTrue();
    List<AlertRule> findByStreamName(String streamName);
    List<AlertRule> findByType(AlertRule.AlertType type);
}

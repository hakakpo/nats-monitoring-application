package com.natsmonitor.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
    Page<AlertHistory> findAllByOrderByTriggeredAtDesc(Pageable pageable);
    List<AlertHistory> findByTriggeredAtAfter(LocalDateTime after);
    long countByTriggeredAtAfter(LocalDateTime after);
}

package com.natsmonitor.repository;

import com.natsmonitor.model.NatsMetricSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NatsMetricSnapshotRepository extends JpaRepository<NatsMetricSnapshot, Long> {
    Page<NatsMetricSnapshot> findAllByOrderByCapturedAtDesc(Pageable pageable);
    long deleteByCapturedAtBefore(LocalDateTime before);
}

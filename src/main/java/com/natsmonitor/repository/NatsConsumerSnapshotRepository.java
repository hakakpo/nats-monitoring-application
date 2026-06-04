package com.natsmonitor.repository;

import com.natsmonitor.model.NatsConsumerSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NatsConsumerSnapshotRepository extends JpaRepository<NatsConsumerSnapshot, Long> {
    Page<NatsConsumerSnapshot> findAllByOrderByCapturedAtDesc(Pageable pageable);
    long deleteByCapturedAtBefore(LocalDateTime before);
}

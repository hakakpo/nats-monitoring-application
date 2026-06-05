package com.natsmonitor.repository;

import com.natsmonitor.model.NatsEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NatsEventRepository extends JpaRepository<NatsEvent, Long> {
    Page<NatsEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);
    long deleteByReceivedAtBefore(LocalDateTime before);
}

package com.natsmonitor.repository;

import com.natsmonitor.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Page<Incident> findAllByOrderByLastSeenDesc(Pageable pageable);
    Page<Incident> findByStatusOrderByLastSeenDesc(Incident.Status status, Pageable pageable);
    long deleteByStatusAndResolvedAtBefore(Incident.Status status, LocalDateTime before);
    Optional<Incident> findFirstByStatusAndTypeAndResourceOrderByLastSeenDesc(
            Incident.Status status,
            String type,
            String resource
    );
}

package com.vega.techtest.adapter.in.messaging.kafka.repository;

import com.vega.techtest.adapter.in.messaging.kafka.entity.ProcessedKafkaEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEventEntity, String> {

    Optional<ProcessedKafkaEventEntity> findByEventId(String eventId);

    void deleteByCreatedAtBefore(Instant cutoffDate);
}

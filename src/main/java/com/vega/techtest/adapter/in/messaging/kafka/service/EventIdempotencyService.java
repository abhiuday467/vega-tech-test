package com.vega.techtest.adapter.in.messaging.kafka.service;

import com.vega.techtest.adapter.in.messaging.kafka.entity.ProcessedKafkaEventEntity;
import com.vega.techtest.adapter.in.messaging.kafka.repository.ProcessedKafkaEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service responsible for ensuring event processing idempotency.
 * Uses database-level locking to prevent duplicate processing across multiple instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventIdempotencyService {

    private final ProcessedKafkaEventRepository repository;

    /**
     * Attempts to acquire a processing lock for the given event.
     * Uses a new transaction to ensure the lock is committed immediately.
     *
     * @param eventId   The unique event identifier
     * @param eventType The type of the event
     * @return true if lock acquired successfully, false if event already exists (duplicate)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquireProcessingLock(String eventId, String eventType) {
        try {
            // Check if event already exists
            Optional<ProcessedKafkaEventEntity> existing = repository.findByEventId(eventId);
            if (existing.isPresent()) {
                log.info("Event {} already exists with status: {}", eventId, existing.get().getStatus());
                return false;
            }

            // Create new processing record
            ProcessedKafkaEventEntity entity = new ProcessedKafkaEventEntity();
            entity.setEventId(eventId);
            entity.setEventType(eventType);
            entity.setStatus(ProcessedKafkaEventEntity.EventStatus.PROCESSING);
            entity.setCreatedAt(Instant.now());

            repository.save(entity);
            log.debug("Processing lock acquired for event: {}", eventId);
            return true;

        } catch (DataIntegrityViolationException e) {
            // Race condition: another instance inserted the record first
            log.info("Duplicate event detected (race condition): {}", eventId);
            return false;
        }
    }

    /**
     * Marks an event as successfully completed.
     *
     * @param eventId The event identifier to mark as completed
     */
    @Transactional
    public void markAsCompleted(String eventId) {
        Optional<ProcessedKafkaEventEntity> entityOpt = repository.findByEventId(eventId);
        if (entityOpt.isPresent()) {
            ProcessedKafkaEventEntity entity = entityOpt.get();
            entity.setStatus(ProcessedKafkaEventEntity.EventStatus.COMPLETED);
            entity.setProcessedAt(Instant.now());
            repository.save(entity);
            log.debug("Event {} marked as COMPLETED", eventId);
        } else {
            log.warn("Attempted to mark non-existent event {} as completed", eventId);
        }
    }

    /**
     * Marks an event as failed with error details.
     *
     * @param eventId      The event identifier to mark as failed
     * @param errorMessage The error message describing the failure
     */
    @Transactional
    public void markAsFailed(String eventId, String errorMessage) {
        Optional<ProcessedKafkaEventEntity> entityOpt = repository.findByEventId(eventId);
        if (entityOpt.isPresent()) {
            ProcessedKafkaEventEntity entity = entityOpt.get();
            entity.setStatus(ProcessedKafkaEventEntity.EventStatus.FAILED);
            entity.setFailedAt(Instant.now());
            entity.setErrorMessage(errorMessage);
            repository.save(entity);
            log.debug("Event {} marked as FAILED: {}", eventId, errorMessage);
        } else {
            log.warn("Attempted to mark non-existent event {} as failed", eventId);
        }
    }

    /**
     * Deletes events older than the specified cutoff date.
     * Used for cleanup to prevent unbounded table growth.
     *
     * @param cutoffDate The date before which events should be deleted
     */
    @Transactional
    public void deleteOldEvents(Instant cutoffDate) {
        repository.deleteByCreatedAtBefore(cutoffDate);
        log.info("Deleted processed events older than {}", cutoffDate);
    }
}

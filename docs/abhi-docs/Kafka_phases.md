# Kafka Consumer Implementation Plan

## Package Structure
All implementation in: `adapter.in.messaging.kafka`

---

## Phase 1: Basic Kafka Setup & Message Printing

**Goal**: Receive Kafka messages and print them to console

**Files to Create**:
1. `config/KafkaConsumerConfig.java`
   - Configure ConsumerFactory with JsonDeserializer
   - Configure KafkaListenerContainerFactory
   - Manual acknowledgment mode

2. `consumer/TransactionKafkaConsumer.java`
   - @KafkaListener method
   - Receive `KafkaTransactionEvent`
   - Print entire event to log
   - Manual acknowledge

3. `application.yml` (update)
   - Kafka broker URL
   - Topic name: `transaction-events`
   - Consumer group ID
   - Auto-commit: false

**Testing**:
- Start application
- Send test message to Kafka topic
- Verify message printed in logs
- Verify message acknowledged (offset committed)

---

## Phase 2: Validate, Translate, Process & Acknowledge

**Goal**: Full processing pipeline without idempotency

**Files to Create**:
1. `validator/KafkaEventValidator.java`
   - Validate eventId, eventType, data not null
   - Validate required fields in data map
   - Throw ValidationException if invalid

2. `mapper/KafkaEventMapper.java`
   - Extract fields from Map<String, Object>
   - Convert types: String→Instant, Number→BigDecimal
   - Map to CreateTransactionCommand
   - Map nested items list

3. `service/KafkaMessageProcessor.java`
   - Orchestrate: validate → map → process
   - Call TransactionApplicationService
   - Return success/failure result

4. `application/transaction/service/TransactionApplicationService.java`
   - Bridge to domain TransactionService
   - @Transactional wrapper

5. `exception/EventProcessingException.java`
   - Wrapper for processing errors

**Update**:
6. `consumer/TransactionKafkaConsumer.java`
   - Inject processor
   - Call processor.process()
   - Acknowledge only on success
   - Log errors, don't ack on failure

**Testing**:
- Send valid message → verify transaction created in DB
- Send invalid message → verify rejected, not acknowledged
- Send message causing business error → verify not acknowledged, will retry

---

## Phase 3: Database Idempotency

**Goal**: Prevent duplicate processing across multiple instances

**Files to Create**:
1. `db/changelog/003-processed-kafka-events-schema.yaml`
   - Table: `processed_kafka_events`
   - Columns: event_id (PK), event_type, status, created_at, processed_at, failed_at, error_message
   - Status: PROCESSING, COMPLETED, FAILED
   - Unique constraint on event_id

2. `entity/ProcessedKafkaEventEntity.java`
   - JPA entity with status enum
   - Timestamps for tracking

3. `repository/ProcessedKafkaEventRepository.java`
   - JPA repository
   - findByEventId()
   - deleteByCreatedAtBefore()

4. `service/EventIdempotencyService.java`
   - tryAcquireProcessingLock(eventId) → INSERT with PROCESSING status
   - Use @Transactional(propagation = REQUIRES_NEW)
   - Catch DataIntegrityViolationException → return false (duplicate)
   - markAsCompleted(eventId) → UPDATE status to COMPLETED
   - markAsFailed(eventId, error) → UPDATE status to FAILED

5. `exception/DuplicateEventException.java`
   - Thrown when duplicate detected

**Update**:
6. `service/KafkaMessageProcessor.java`
   - Add idempotency check at start
   - If duplicate → log, return duplicate result
   - On success → mark completed
   - On failure → mark failed

**Testing**:
- Send same message twice → verify only processed once
- Send message, restart app, send again → verify duplicate blocked
- Simulate 2 instances → verify only 1 processes

---

## Phase 4: Dead Letter Queue

**Goal**: Route failed messages to DLQ after retries exhausted

**Files to Create**:
1. `config/KafkaDLQConfig.java`
   - Configure DeadLetterPublishingRecoverer
   - Configure DefaultErrorHandler with FixedBackOff(1000, 3) → 3 retries
   - DLQ topic: `transaction-events.DLQ`

2. `application.yml` (update)
   - Add DLQ topic name
   - Configure retry policy (3 attempts, 1 second delay)

**Update**:
3. `config/KafkaConsumerConfig.java`
   - Wire DefaultErrorHandler into listener container factory

4. `consumer/TransactionKafkaConsumer.java`
   - Throw exception on processing failure (let error handler retry)
   - Don't catch retryable exceptions

**Error Flow**:
- Validation errors → Non-retryable → DLQ immediately
- Processing errors → Retryable → Kafka retries 3x → DLQ

**Testing**:
- Send message causing validation error → verify in DLQ immediately
- Send message causing processing error → verify 3 retries → DLQ
- Check DLQ topic for failed messages

---

## Phase 5: Automated Tests

**Goal**: Comprehensive test coverage

**Files to Create**:
1. `mapper/KafkaEventMapperTest.java`
   - Test field mapping
   - Test type conversions
   - Test null handling
   - Test nested items

2. `validator/KafkaEventValidatorTest.java`
   - Test valid event passes
   - Test missing fields rejected
   - Test invalid types rejected

3. `service/EventIdempotencyServiceTest.java`
   - Test lock acquisition
   - Test duplicate detection
   - Test status transitions
   - Mock repository

4. `service/KafkaMessageProcessorTest.java`
   - Test success flow
   - Test duplicate handling
   - Test validation errors
   - Test processing errors
   - Mock all dependencies

5. `integration/KafkaConsumerIntegrationTest.java`
   - Use @EmbeddedKafka
   - Test end-to-end: send message → verify in DB
   - Test duplicate detection
   - Test DLQ routing
   - Test retry mechanism

**Run All Tests**:
- `mvn test`
- Verify all pass

---

## Phase 6 (Optional): Scheduled Cleanup

**Goal**: Delete events older than 4 days

**Files to Create**:
1. `scheduler/EventCleanupScheduler.java`
   - @Scheduled(cron = "0 0 2 * * ?") → Daily at 2 AM
   - Delete events where created_at < NOW() - 4 days
   - Log deletion count
   - @ConditionalOnProperty for easy enable/disable

2. `application.yml` (update)
   - Add cleanup configuration
   - Retention days: 4
   - Cron expression
   - Enabled flag

**Testing**:
- Insert old test records (5 days old)
- Trigger scheduler manually
- Verify old records deleted
- Verify recent records kept

---

## File Structure Summary

```
adapter.in.messaging.kafka/
├── config/
│   ├── KafkaConsumerConfig.java          [Phase 1, 4]
│   └── KafkaDLQConfig.java               [Phase 4]
├── consumer/
│   └── TransactionKafkaConsumer.java     [Phase 1, 2, 4]
├── dto/
│   └── KafkaTransactionEvent.java        [EXISTING]
├── mapper/
│   └── KafkaEventMapper.java             [Phase 2]
├── validator/
│   └── KafkaEventValidator.java          [Phase 2]
├── service/
│   ├── EventIdempotencyService.java      [Phase 3]
│   └── KafkaMessageProcessor.java        [Phase 2, 3]
├── entity/
│   └── ProcessedKafkaEventEntity.java    [Phase 3]
├── repository/
│   └── ProcessedKafkaEventRepository.java [Phase 3]
├── scheduler/
│   └── EventCleanupScheduler.java        [Phase 6]
└── exception/
    ├── DuplicateEventException.java      [Phase 3]
    └── EventProcessingException.java     [Phase 2]

application/transaction/service/
└── TransactionApplicationService.java     [Phase 2]

db/changelog/
└── 003-processed-kafka-events-schema.yaml [Phase 3]
```

---

## Dependencies to Add (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Testing After Each Phase

**Phase 1**: Use Kafka CLI or kafka-console-producer
```bash
kafka-console-producer --broker-list localhost:9092 --topic transaction-events
# Paste JSON message
```

**Phase 2**: Send valid/invalid messages, check DB for transactions

**Phase 3**: Send same message twice, verify duplicate blocked

**Phase 4**: Send message causing error, check DLQ topic

**Phase 5**: Run `mvn test`

**Phase 6**: Insert old records, trigger scheduler, verify deletion

---

## Estimated Time

- Phase 1: 1 hour
- Phase 2: 2 hours
- Phase 3: 2 hours
- Phase 4: 1.5 hours
- Phase 5: 3 hours
- Phase 6: 0.5 hour

**Total: ~10 hours**

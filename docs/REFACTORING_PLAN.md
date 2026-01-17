# Transaction Service Layer Refactoring Plan

## Executive Summary

This document outlines the refactoring of the transaction processing system to implement **Hexagonal Architecture** (Ports and Adapters pattern). The goal is to decouple the service layer from input sources (REST API, future Kafka consumer) by introducing service-layer DTOs that serve as a contract between adapters and business logic.

### Objectives
1. Decouple service layer from API/infrastructure concerns
2. Prepare for Kafka consumer implementation
3. Create reusable business logic accessible from multiple sources
4. Maintain backward compatibility with existing REST API

### Architecture Pattern
**Hexagonal Architecture** - Service layer defines "ports" (contracts), adapters translate external formats to/from these contracts.

---

## Current Architecture

```
┌─────────────────────────────────────────────────────┐
│                 REST Controller                      │
│                                                      │
│  TransactionRequest (API DTO)                       │
│         ↓                                            │
│  TransactionValidator                               │
└─────────────────┬───────────────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────────────┐
│              TransactionService                      │
│                                                      │
│  processTransaction(TransactionRequest)             │
│         ↓                                            │
│  Service tightly coupled to API DTO                 │
└─────────────────┬───────────────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────────────┐
│           TransactionEntityMapper                    │
│                                                      │
│  TransactionRequest → TransactionEntity             │
│  TransactionEntity → TransactionResponse            │
└─────────────────┬───────────────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────────────┐
│            TransactionRepository                     │
└─────────────────────────────────────────────────────┘
```

### Problem
- Service layer depends on API DTO (`TransactionRequest`)
- When adding Kafka consumer, we'd need service to accept multiple DTO types
- Business logic coupled to infrastructure concerns

---

## Target Architecture

```
┌──────────────────────────────┐  ┌──────────────────────────────┐
│      REST Controller         │  │    Kafka Consumer (Future)   │
│                              │  │                              │
│   TransactionRequest         │  │   KafkaTransactionEvent      │
│          ↓                   │  │          ↓                   │
│   @Valid Validation          │  │   KafkaEventValidator        │
│          ↓                   │  │          ↓                   │
│   TransactionRequestMapper   │  │   KafkaEventMapper           │
└──────────────┬───────────────┘  └──────────────┬───────────────┘
               │                                  │
               └──────────────┬───────────────────┘
                              ↓
        ┌─────────────────────────────────────────────────┐
        │         SERVICE LAYER (Port/Contract)           │
        │                                                 │
        │  Input:  CreateTransactionCommand               │
        │          ↓                                      │
        │  processTransaction(CreateTransactionCommand)   │
        │          ↓                                      │
        │  Output: TransactionResult                      │
        └─────────────────┬───────────────────────────────┘
                          │
                          ↓
        ┌─────────────────────────────────────────────────┐
        │         TransactionEntityMapper                 │
        │                                                 │
        │  CreateTransactionCommand → TransactionEntity   │
        │  TransactionEntity → TransactionResult          │
        └─────────────────┬───────────────────────────────┘
                          │
                          ↓
        ┌─────────────────────────────────────────────────┐
        │         TransactionRepository                   │
        └─────────────────────────────────────────────────┘
```

### Benefits
- ✅ Service layer independent of input source
- ✅ Single business logic implementation for REST + Kafka
- ✅ Clear separation of concerns
- ✅ Easy to add new input sources (gRPC, GraphQL, etc.)
- ✅ Service layer DTOs optimized for business logic
- ✅ Improved testability

---

## Files to Create

### 1. CreateTransactionCommand.java
**Location:** `src/main/java/com/vega/techtest/service/command/CreateTransactionCommand.java`

**Purpose:** Service layer input DTO representing a transaction creation request

**Characteristics:**
- Immutable (Lombok `@Value` or record)
- Optional `transactionId` (null = generate new, provided = use it)
- No validation annotations (validated at adapter layer)
- `@EqualsAndHashCode(of = {"timestamp", "storeId", "tillId"})` for duplicate detection
- Contains nested list of transaction items

**Fields:**
```java
- String transactionId         // OPTIONAL - can be null
- String customerId
- String storeId              // Required for business logic
- String tillId               // Required for business logic
- String paymentMethod
- BigDecimal totalAmount
- String currency
- ZonedDateTime timestamp     // Required for business logic
- List<TransactionItem> items
```

**Why optional transactionId?**
- REST API: doesn't provide ID → service generates one
- Kafka: may provide ID from source system → service uses it
- Preserves traceability with external systems (till receipts, etc.)

---

### 2. TransactionResult.java
**Location:** `src/main/java/com/vega/techtest/service/command/TransactionResult.java`

**Purpose:** Service layer output DTO representing the result of transaction creation

**Characteristics:**
- Immutable
- Always contains `transactionId` (guaranteed after processing)
- `@EqualsAndHashCode(of = {"timestamp", "storeId", "tillId"})`
- Contains all transaction details including generated/assigned ID

**Fields:**
```java
- String transactionId        // REQUIRED - always present after processing
- String customerId
- String storeId
- String tillId
- String paymentMethod
- BigDecimal totalAmount
- String currency
- ZonedDateTime transactionTimestamp
- ZonedDateTime createdAt
- String status
- List<TransactionItemResult> items
```

---

### 3. TransactionItem.java
**Location:** `src/main/java/com/vega/techtest/service/command/TransactionItem.java`

**Purpose:** Service layer DTO for transaction line items (input)

**Characteristics:**
- Immutable
- Minimal fields needed for business logic

**Fields:**
```java
- String productName
- String productCode
- BigDecimal unitPrice
- Integer quantity
- String category
```

---

### 4. TransactionItemResult.java
**Location:** `src/main/java/com/vega/techtest/service/command/TransactionItemResult.java`

**Purpose:** Service layer DTO for transaction line items (output)

**Fields:**
```java
- String productName
- String productCode
- BigDecimal unitPrice
- Integer quantity
- BigDecimal totalPrice    // Calculated: unitPrice * quantity
- String category
```

---

### 5. TransactionRequestMapper.java
**Location:** `src/main/java/com/vega/techtest/mapper/TransactionRequestMapper.java`

**Purpose:** Maps REST API DTO to service layer command

**Type:** MapStruct interface

**Mappings:**
```java
@Mapper(componentModel = "spring")
public interface TransactionRequestMapper {

    // API → Service Command
    CreateTransactionCommand toCommand(TransactionRequest request);

    // Nested items mapping
    TransactionItem toCommandItem(TransactionItemRequest item);
    List<TransactionItem> toCommandItems(List<TransactionItemRequest> items);
}
```

**Notes:**
- Simple direct mapping (field names match)
- No complex transformations needed
- MapStruct generates implementation at compile time

---

### 6. KafkaEventMapper.java (Future Implementation)
**Location:** `src/main/java/com/vega/techtest/mapper/KafkaEventMapper.java`

**Purpose:** Maps Kafka event DTO to service layer command

**Type:** MapStruct interface with custom methods

**Mappings:**
```java
@Mapper(componentModel = "spring")
public interface KafkaEventMapper {

    @Mapping(target = "transactionId", source = "data.transactionId")
    @Mapping(target = "customerId", source = "data.customerId")
    @Mapping(target = "storeId", source = "data.storeId")
    @Mapping(target = "tillId", source = "data.tillId")
    @Mapping(target = "paymentMethod", source = "data.paymentMethod")
    @Mapping(target = "totalAmount", expression = "java(toBigDecimal(event.getData().get(\"totalAmount\")))")
    @Mapping(target = "currency", source = "data.currency")
    @Mapping(target = "timestamp", expression = "java(parseTimestamp(event.getData().get(\"timestamp\")))")
    CreateTransactionCommand toCommand(KafkaTransactionEvent event);

    // Type conversion helpers
    default ZonedDateTime parseTimestamp(Object timestamp) {
        if (timestamp == null) return null;
        return ZonedDateTime.parse(timestamp.toString());
    }

    default BigDecimal toBigDecimal(Object amount) {
        if (amount == null) return null;
        if (amount instanceof BigDecimal) return (BigDecimal) amount;
        if (amount instanceof Number) {
            return BigDecimal.valueOf(((Number) amount).doubleValue());
        }
        return new BigDecimal(amount.toString());
    }
}
```

**Challenges:**
- Kafka `data` field is `Map<String, Object>` (generic)
- Need type conversions: String → ZonedDateTime, Double → BigDecimal
- Need null safety checks

---

### 7. TransactionKafkaConsumer.java (Future Implementation)
**Location:** `src/main/java/com/vega/techtest/consumer/TransactionKafkaConsumer.java`

**Purpose:** Kafka consumer adapter that receives events and delegates to service

**Implementation:**
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionKafkaConsumer {

    private final KafkaEventMapper kafkaEventMapper;
    private final TransactionService transactionService;
    private final KafkaEventValidator kafkaEventValidator;

    @KafkaListener(
        topics = "${kafka.topic.transactions}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void consume(KafkaTransactionEvent event) {
        log.info("Received Kafka event: eventId={}, eventType={}",
                 event.getEventId(), event.getEventType());

        try {
            // 1. Validate Kafka event structure
            kafkaEventValidator.validate(event);

            // 2. Map to service command
            CreateTransactionCommand command = kafkaEventMapper.toCommand(event);

            // 3. Process transaction (same method as REST!)
            TransactionResult result = transactionService.processTransaction(command);

            // 4. Log success
            log.info("Processed Kafka transaction: transactionId={}, eventId={}",
                     result.getTransactionId(), event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process Kafka event: eventId={}",
                      event.getEventId(), e);
            // TODO: Dead letter queue handling
            throw e;
        }
    }
}
```

---

## Files to Modify

### 1. TransactionService.java

**Location:** `src/main/java/com/vega/techtest/service/TransactionService.java`

**Changes:**

#### Method Signature Change
```java
// BEFORE
public TransactionResponse processTransaction(TransactionRequest request)

// AFTER
public TransactionResult processTransaction(CreateTransactionCommand command)
```

#### Updated Implementation
```java
@Transactional
public TransactionResult processTransaction(CreateTransactionCommand command) {
    // Validation removed - happens at adapter layer

    // Use provided transactionId or generate new one
    String transactionId = (command.getTransactionId() != null && !command.getTransactionId().isEmpty())
        ? command.getTransactionId()
        : "TXN-" + UUID.randomUUID().toString();

    // Map command to entity
    TransactionEntity entity = transactionEntityMapper.toEntity(command);
    entity.setTransactionId(transactionId);

    // Map and set items if present
    if (command.getItems() != null && !command.getItems().isEmpty()) {
        List<TransactionItemEntity> itemEntities =
            transactionEntityMapper.toItemEntityList(command.getItems());
        entity.setItems(itemEntities);
        itemEntities.forEach(item -> item.setTransaction(entity));
    }

    // Save to database with duplicate handling
    try {
        entity = transactionRepository.save(entity);
        logger.info("Transaction saved: {}", transactionId);
        transactionMetricsService.recordTransactionCreated(entity.getStoreId());
    } catch (DuplicateKeyException e) {
        logger.warn("Duplicate transaction detected: {}", transactionId);
        entity = transactionRepository.findByStoreIdAndTillIdAndTransactionTimestamp(
            command.getStoreId(),
            command.getTillId(),
            command.getTimestamp()
        );
    }

    // Map entity to result
    return transactionEntityMapper.toResult(entity);
}
```

**Other Methods:**
- No changes needed to query methods (`getTransactionById`, `getTransactionsByStore`, etc.)
- They can continue returning `TransactionResponse` or be updated to return `TransactionResult`

---

### 2. TransactionController.java

**Location:** `src/main/java/com/vega/techtest/controller/TransactionController.java`

**Changes:**

```java
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRequestMapper transactionRequestMapper;  // NEW
    private final TransactionMetricsService transactionMetricsService;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTransaction(
            @Valid @RequestBody TransactionRequest request) {

        logger.info("Received transaction request for store: {}", request.getStoreId());

        // NEW: Map API DTO to service command
        CreateTransactionCommand command = transactionRequestMapper.toCommand(request);

        // Call service with command (not request)
        TransactionResult result = transactionService.processTransaction(command);

        // Record metrics
        transactionMetricsService.recordTransactionSubmission(result.getStoreId());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Transaction processed successfully");
        response.put("transactionId", result.getTransactionId());
        response.put("timestamp", result.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    // Other methods remain unchanged
}
```

**Key Points:**
- Validation stays at controller level using `@Valid` on `TransactionRequest`
- New dependency: `TransactionRequestMapper`
- Map before calling service
- Response building logic unchanged

---

### 3. TransactionEntityMapper.java

**Location:** `src/main/java/com/vega/techtest/mapper/TransactionEntityMapper.java`

**Changes:**

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionEntityMapper {

    // BEFORE: TransactionRequest → TransactionEntity
    // AFTER: CreateTransactionCommand → TransactionEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionId", ignore = true)  // Set manually in service
    @Mapping(target = "createdAt", expression = "java(java.time.ZonedDateTime.now())")
    @Mapping(target = "transactionTimestamp", source = "timestamp")
    @Mapping(target = "items", ignore = true)  // Set manually with bidirectional relationship
    TransactionEntity toEntity(CreateTransactionCommand command);

    // Item mapping (command → entity)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "totalPrice", expression = "java(calculateTotalPrice(item.getUnitPrice(), item.getQuantity()))")
    TransactionItemEntity toItemEntity(TransactionItem item);

    List<TransactionItemEntity> toItemEntityList(List<TransactionItem> items);

    // BEFORE: TransactionEntity → TransactionResponse
    // AFTER: TransactionEntity → TransactionResult

    TransactionResult toResult(TransactionEntity entity);

    List<TransactionResult> toResultList(List<TransactionEntity> entities);

    // Item mapping (entity → result)
    TransactionItemResult toItemResult(TransactionItemEntity entity);

    // Utility methods
    default BigDecimal calculateTotalPrice(BigDecimal unitPrice, Integer quantity) {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // DEPRECATED: Keep for backward compatibility during migration
    @Deprecated
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.ZonedDateTime.now())")
    @Mapping(target = "transactionTimestamp", source = "timestamp")
    @Mapping(target = "items", ignore = true)
    TransactionEntity toEntity(TransactionRequest request);

    @Deprecated
    TransactionResponse toResponse(TransactionEntity entity);

    @Deprecated
    List<TransactionResponse> toResponseList(List<TransactionEntity> entities);
}
```

**Migration Strategy:**
- Add new methods alongside old ones
- Mark old methods as `@Deprecated`
- Allows gradual migration
- Remove deprecated methods after full migration

---

### 4. TransactionValidator.java

**Location:** `src/main/java/com/vega/techtest/validators/TransactionValidator.java`

**Decision:** Keep validating `TransactionRequest` at API boundary

**Rationale:**
- Validation is infrastructure concern (HTTP input validation)
- Different sources may have different validation rules
- Kafka events might have different validation requirements
- Service layer assumes command is valid

**No changes needed** - validator stays as-is, called from controller before mapping

**Future:** Create `KafkaEventValidator` for Kafka-specific validation

---

## Test Files to Update

### 1. TransactionServiceTest.java

**Changes:**
- Replace `TransactionRequest` with `CreateTransactionCommand` in test setup
- Replace `TransactionResponse` with `TransactionResult` in assertions
- Update mocked mapper calls

**Example:**

```java
@Test
@DisplayName("Should generate transaction id and persist transaction")
void processTransaction_generatesIdAndSaves() {
    ZonedDateTime timestamp = ZonedDateTime.now();

    // BEFORE: TransactionRequest request = new TransactionRequest();
    // AFTER:
    CreateTransactionCommand command = CreateTransactionCommand.builder()
        .customerId("CUST-1")
        .storeId("STORE-1")
        .tillId("TILL-1")
        .paymentMethod("card")
        .totalAmount(new BigDecimal("12.50"))
        .currency("GBP")
        .timestamp(timestamp)
        .build();

    TransactionEntity mappedEntity = new TransactionEntity(
        null, "CUST-1", "STORE-1", "TILL-1", "card", new BigDecimal("12.50")
    );
    mappedEntity.setCurrency("GBP");
    mappedEntity.setTransactionTimestamp(timestamp);

    // BEFORE: when(mapper.toEntity(request))
    // AFTER:
    when(mapper.toEntity(command)).thenReturn(mappedEntity);
    when(transactionRepository.save(any(TransactionEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    // BEFORE: when(mapper.toResponse(any()))
    // AFTER:
    when(mapper.toResult(any(TransactionEntity.class)))
        .thenAnswer(invocation -> createTransactionResult(invocation.getArgument(0)));

    // BEFORE: TransactionResponse response = ...
    // AFTER:
    TransactionResult result = transactionService.processTransaction(command);

    assertThat(result.getTransactionId()).startsWith("TXN-");
    assertThat(result.getStoreId()).isEqualTo("STORE-1");
    assertThat(result.getPaymentMethod()).isEqualTo("card");
    assertThat(result.getTotalAmount()).isEqualByComparingTo("12.50");
}

// Helper method updated
private TransactionResult createTransactionResult(TransactionEntity entity) {
    return TransactionResult.builder()
        .transactionId(entity.getTransactionId())
        .customerId(entity.getCustomerId())
        .storeId(entity.getStoreId())
        .tillId(entity.getTillId())
        .paymentMethod(entity.getPaymentMethod())
        .totalAmount(entity.getTotalAmount())
        .currency(entity.getCurrency())
        .transactionTimestamp(entity.getTransactionTimestamp())
        .status(entity.getStatus())
        .build();
}
```

**All test methods need similar updates** (8+ tests total)

---

### 2. TransactionControllerTest.java

**Changes:**
- Mock `TransactionRequestMapper`
- Update service method expectations

**Example:**

```java
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private TransactionRequestMapper transactionRequestMapper;  // NEW

    @MockBean
    private TransactionMetricsService transactionMetricsService;

    @Test
    void submitTransaction_success() throws Exception {
        // Given
        TransactionRequest request = createValidRequest();
        CreateTransactionCommand command = createCommandFromRequest(request);
        TransactionResult result = createSuccessResult();

        // Mock mapper
        when(transactionRequestMapper.toCommand(any(TransactionRequest.class)))
            .thenReturn(command);

        // Mock service
        when(transactionService.processTransaction(command))
            .thenReturn(result);

        // When/Then
        mockMvc.perform(post("/api/transactions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.transactionId").value(result.getTransactionId()));

        verify(transactionRequestMapper).toCommand(any(TransactionRequest.class));
        verify(transactionService).processTransaction(command);
    }
}
```

---

### 3. New Test Files

#### TransactionRequestMapperTest.java
```java
@SpringBootTest
class TransactionRequestMapperTest {

    @Autowired
    private TransactionRequestMapper mapper;

    @Test
    void shouldMapTransactionRequestToCommand() {
        // Given
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-123");
        request.setStoreId("STORE-001");
        request.setTillId("TILL-1");
        request.setPaymentMethod("card");
        request.setTotalAmount(new BigDecimal("100.00"));
        request.setTimestamp(ZonedDateTime.now());

        // When
        CreateTransactionCommand command = mapper.toCommand(request);

        // Then
        assertThat(command.getTransactionId()).isEqualTo("TXN-123");
        assertThat(command.getStoreId()).isEqualTo("STORE-001");
        assertThat(command.getTillId()).isEqualTo("TILL-1");
        assertThat(command.getPaymentMethod()).isEqualTo("card");
        assertThat(command.getTotalAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldMapNullTransactionId() {
        // Given
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId(null);  // No ID provided
        request.setStoreId("STORE-001");

        // When
        CreateTransactionCommand command = mapper.toCommand(request);

        // Then
        assertThat(command.getTransactionId()).isNull();
    }
}
```

#### KafkaEventMapperTest.java (Future)
```java
@SpringBootTest
class KafkaEventMapperTest {

    @Autowired
    private KafkaEventMapper mapper;

    @Test
    void shouldMapKafkaEventToCommand() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", "TXN-KAFKA-123");
        data.put("storeId", "STORE-001");
        data.put("tillId", "TILL-1");
        data.put("paymentMethod", "cash");
        data.put("totalAmount", 25.50);  // Double
        data.put("currency", "GBP");
        data.put("timestamp", "2025-06-27T12:00:00.000Z");

        KafkaTransactionEvent event = new KafkaTransactionEvent(
            "event-123",
            "TRANSACTION_CREATED",
            "2025-06-27T12:00:00.000Z",
            "till-system",
            "1.0",
            data
        );

        // When
        CreateTransactionCommand command = mapper.toCommand(event);

        // Then
        assertThat(command.getTransactionId()).isEqualTo("TXN-KAFKA-123");
        assertThat(command.getStoreId()).isEqualTo("STORE-001");
        assertThat(command.getTotalAmount()).isEqualByComparingTo("25.50");
        assertThat(command.getTimestamp()).isNotNull();
    }

    @Test
    void shouldHandleTypeConversions() {
        // Test Double → BigDecimal
        // Test String → ZonedDateTime
        // Test null values
    }
}
```

---

## Implementation Plan

### Phase 1: Create New DTOs (Low Risk)
**Estimated effort:** 2-3 hours

1. Create `CreateTransactionCommand.java`
2. Create `TransactionResult.java`
3. Create `TransactionItem.java`
4. Create `TransactionItemResult.java`
5. Verify compilation
6. Write unit tests for DTO construction

**Acceptance Criteria:**
- [ ] All DTOs compile without errors
- [ ] Lombok/Builder annotations work correctly
- [ ] Equals/HashCode based on specified fields
- [ ] DTOs are immutable

---

### Phase 2: Create Mappers (Low Risk)
**Estimated effort:** 2-3 hours

1. Create `TransactionRequestMapper.java`
2. Build project to generate MapStruct implementation
3. Verify generated mapper in `target/generated-sources`
4. Write `TransactionRequestMapperTest.java`
5. Test all field mappings
6. Test null handling

**Acceptance Criteria:**
- [ ] Mapper compiles and generates implementation
- [ ] All fields map correctly
- [ ] Null values handled safely
- [ ] Tests pass with 100% coverage

---

### Phase 3: Update Entity Mapper (Medium Risk)
**Estimated effort:** 2-3 hours

1. Add new methods to `TransactionEntityMapper.java`
   - `toEntity(CreateTransactionCommand)`
   - `toResult(TransactionEntity)`
2. Keep old methods marked as `@Deprecated`
3. Build and verify generated implementation
4. Write tests for new mapper methods
5. Verify backward compatibility

**Acceptance Criteria:**
- [ ] New mapper methods work correctly
- [ ] Old methods still work (deprecated but functional)
- [ ] MapStruct generates correct implementation
- [ ] Tests pass

---

### Phase 4: Update Service Layer (High Risk)
**Estimated effort:** 3-4 hours

1. Update `TransactionService.processTransaction()` signature
2. Remove validation logic (moved to controller)
3. Update mapper calls
4. Update all service tests (`TransactionServiceTest.java`)
5. Run all service tests
6. Verify duplicate detection still works

**Acceptance Criteria:**
- [ ] Service method accepts `CreateTransactionCommand`
- [ ] Service returns `TransactionResult`
- [ ] TransactionId handling (optional) works correctly
- [ ] Duplicate detection unchanged
- [ ] All service tests pass

**Risk Mitigation:**
- Keep old method temporarily for rollback
- Test extensively before moving to Phase 5

---

### Phase 5: Update Controller Layer (Medium Risk)
**Estimated effort:** 2-3 hours

1. Inject `TransactionRequestMapper` into controller
2. Map `TransactionRequest` → `CreateTransactionCommand`
3. Update response handling
4. Update controller tests
5. Run integration tests

**Acceptance Criteria:**
- [ ] Controller uses new mapper
- [ ] Validation still works at API boundary
- [ ] Response format unchanged
- [ ] All controller tests pass
- [ ] Integration tests pass

---

### Phase 6: Update All Tests (Medium Risk)
**Estimated effort:** 4-5 hours

1. Update `TransactionServiceTest.java` (8+ tests)
2. Update `TransactionControllerTest.java`
3. Update `TransactionEntityMapperTest.java`
4. Update integration tests if any
5. Run full test suite
6. Fix any failing tests

**Acceptance Criteria:**
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Test coverage maintained or improved
- [ ] No flaky tests

---

### Phase 7: Cleanup (Low Risk)
**Estimated effort:** 1-2 hours

1. Remove `@Deprecated` methods from `TransactionEntityMapper`
2. Remove old test helper methods
3. Update documentation
4. Code review
5. Final testing

**Acceptance Criteria:**
- [ ] No deprecated code remains
- [ ] Documentation updated
- [ ] Code review approved
- [ ] All tests pass

---

## Future: Kafka Consumer Implementation

### Phase 8: Kafka Setup (Future)
**Estimated effort:** 4-6 hours

1. Add Kafka dependencies to `pom.xml`
2. Configure Kafka properties
3. Create `KafkaEventMapper.java`
4. Write `KafkaEventMapperTest.java`
5. Create `KafkaEventValidator.java`
6. Test type conversions (Map → typed objects)

**Acceptance Criteria:**
- [ ] Kafka dependencies added
- [ ] Mapper handles Map<String, Object> correctly
- [ ] Type conversions work (String→ZonedDateTime, Double→BigDecimal)
- [ ] Tests pass

---

### Phase 9: Kafka Consumer (Future)
**Estimated effort:** 3-4 hours

1. Create `TransactionKafkaConsumer.java`
2. Implement `@KafkaListener` method
3. Wire up mapper and service
4. Add error handling
5. Add logging and metrics
6. Write consumer tests

**Acceptance Criteria:**
- [ ] Consumer receives Kafka messages
- [ ] Maps to `CreateTransactionCommand` correctly
- [ ] Calls same service method as REST
- [ ] Error handling works
- [ ] Dead letter queue configured

---

### Phase 10: End-to-End Testing (Future)
**Estimated effort:** 3-4 hours

1. Set up local Kafka broker (Docker)
2. Produce test messages
3. Verify consumer processes them
4. Verify database persistence
5. Test duplicate handling
6. Test error scenarios

**Acceptance Criteria:**
- [ ] Messages consumed successfully
- [ ] Transactions persisted correctly
- [ ] Duplicates detected (idempotency works)
- [ ] Errors logged and handled
- [ ] Metrics recorded

---

## Package Structure

```
com.vega.techtest
├── controller
│   └── TransactionController.java (MODIFIED)
├── service
│   ├── command (NEW PACKAGE)
│   │   ├── CreateTransactionCommand.java (NEW)
│   │   ├── TransactionResult.java (NEW)
│   │   ├── TransactionItem.java (NEW)
│   │   └── TransactionItemResult.java (NEW)
│   ├── TransactionService.java (MODIFIED)
│   └── TransactionMetricsService.java (unchanged)
├── mapper
│   ├── TransactionRequestMapper.java (NEW)
│   ├── KafkaEventMapper.java (NEW - Future)
│   └── TransactionEntityMapper.java (MODIFIED)
├── dto
│   ├── TransactionRequest.java (unchanged)
│   ├── TransactionResponse.java (unchanged)
│   ├── KafkaTransactionEvent.java (unchanged)
│   └── ... (other DTOs unchanged)
├── entity
│   ├── TransactionEntity.java (unchanged)
│   └── TransactionItemEntity.java (unchanged)
├── repository
│   └── TransactionRepository.java (unchanged)
├── validators
│   ├── TransactionValidator.java (unchanged)
│   └── KafkaEventValidator.java (NEW - Future)
└── consumer (NEW PACKAGE - Future)
    └── TransactionKafkaConsumer.java (NEW - Future)
```

---

## Detailed Class Implementations

### CreateTransactionCommand.java

```java
package com.vega.techtest.service.command;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service layer command for creating a new transaction.
 *
 * This class represents the contract between adapters (REST, Kafka, etc.)
 * and the service layer. It is source-agnostic and contains only the data
 * needed for transaction processing.
 *
 * The transactionId field is optional:
 * - If null: service will generate a new UUID-based ID
 * - If provided: service will use the provided ID (from external systems like till)
 *
 * Equality is based on the natural key (timestamp, storeId, tillId) which
 * is used for duplicate detection via database unique constraint.
 */
@Value
@Builder
@EqualsAndHashCode(of = {"timestamp", "storeId", "tillId"})
public class CreateTransactionCommand {

    /**
     * Transaction identifier. Optional.
     * - null: service generates new ID
     * - provided: service uses this ID (for external system traceability)
     */
    String transactionId;

    /**
     * Customer identifier. Optional.
     */
    String customerId;

    /**
     * Store identifier. Required.
     * Part of natural key for duplicate detection.
     */
    String storeId;

    /**
     * Till/POS identifier. Required.
     * Part of natural key for duplicate detection.
     */
    String tillId;

    /**
     * Payment method (e.g., "card", "cash"). Required.
     */
    String paymentMethod;

    /**
     * Total transaction amount. Required.
     */
    BigDecimal totalAmount;

    /**
     * Currency code (e.g., "GBP", "USD"). Defaults to "GBP".
     */
    @Builder.Default
    String currency = "GBP";

    /**
     * Transaction timestamp. Required.
     * Part of natural key for duplicate detection.
     */
    ZonedDateTime timestamp;

    /**
     * Line items for this transaction. Optional but typically present.
     */
    List<TransactionItem> items;
}
```

---

### TransactionResult.java

```java
package com.vega.techtest.service.command;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service layer result representing a successfully processed transaction.
 *
 * This class is returned by the service layer after transaction processing.
 * It always contains a transactionId (either generated or provided).
 *
 * Equality is based on the natural key (timestamp, storeId, tillId) for
 * consistency with CreateTransactionCommand and database constraints.
 */
@Value
@Builder
@EqualsAndHashCode(of = {"transactionTimestamp", "storeId", "tillId"})
public class TransactionResult {

    /**
     * Transaction identifier. Always present after processing.
     */
    String transactionId;

    /**
     * Customer identifier.
     */
    String customerId;

    /**
     * Store identifier.
     */
    String storeId;

    /**
     * Till/POS identifier.
     */
    String tillId;

    /**
     * Payment method.
     */
    String paymentMethod;

    /**
     * Total transaction amount.
     */
    BigDecimal totalAmount;

    /**
     * Currency code.
     */
    String currency;

    /**
     * Transaction timestamp (from original request/event).
     */
    ZonedDateTime transactionTimestamp;

    /**
     * Database creation timestamp (when record was created).
     */
    ZonedDateTime createdAt;

    /**
     * Transaction status (e.g., "COMPLETED", "PENDING").
     */
    String status;

    /**
     * Line items for this transaction.
     */
    List<TransactionItemResult> items;
}
```

---

### TransactionItem.java

```java
package com.vega.techtest.service.command;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Service layer DTO for transaction line item (input).
 */
@Value
@Builder
public class TransactionItem {

    /**
     * Product name.
     */
    String productName;

    /**
     * Product code/SKU.
     */
    String productCode;

    /**
     * Unit price of the product.
     */
    BigDecimal unitPrice;

    /**
     * Quantity purchased.
     */
    Integer quantity;

    /**
     * Product category.
     */
    String category;
}
```

---

### TransactionItemResult.java

```java
package com.vega.techtest.service.command;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Service layer DTO for transaction line item (output).
 */
@Value
@Builder
public class TransactionItemResult {

    /**
     * Product name.
     */
    String productName;

    /**
     * Product code/SKU.
     */
    String productCode;

    /**
     * Unit price of the product.
     */
    BigDecimal unitPrice;

    /**
     * Quantity purchased.
     */
    Integer quantity;

    /**
     * Total price for this line item (unitPrice * quantity).
     * Calculated by the service/mapper.
     */
    BigDecimal totalPrice;

    /**
     * Product category.
     */
    String category;
}
```

---

## Testing Strategy

### Unit Tests
- **Mapper Tests:** Verify field mappings, null handling, type conversions
- **Service Tests:** Verify business logic with mocked dependencies
- **Validator Tests:** Verify validation rules (if adding service-layer validation)

### Integration Tests
- **Controller Tests:** Verify REST API with mocked service
- **Repository Tests:** Verify database operations
- **End-to-End Tests:** Full flow from REST → Service → Database

### Future Kafka Tests
- **Consumer Tests:** Verify message consumption and processing
- **Mapper Tests:** Verify Kafka event → command mapping
- **Integration Tests:** Kafka → Service → Database flow

---

## Rollback Plan

If issues are discovered:

### Phase 4 or Earlier
- Revert commits
- Restore from version control
- Low risk as service not yet changed

### Phase 5 or Later
- Keep deprecated methods in `TransactionEntityMapper`
- Add backward-compatible overload in `TransactionService`:
  ```java
  @Deprecated
  public TransactionResponse processTransaction(TransactionRequest request) {
      CreateTransactionCommand command = requestMapper.toCommand(request);
      TransactionResult result = processTransaction(command);
      return legacyMapper.toResponse(result);
  }
  ```
- Controller can temporarily use old method
- Allows time to fix issues

---

## Success Criteria

### Technical Success
- [ ] All tests pass (unit + integration)
- [ ] No regression in existing functionality
- [ ] Code compiles without warnings
- [ ] MapStruct generates all implementations correctly
- [ ] Performance unchanged or improved

### Architectural Success
- [ ] Service layer decoupled from API DTOs
- [ ] Clear separation of concerns
- [ ] Service layer reusable from multiple sources
- [ ] Easy to add Kafka consumer in future

### Business Success
- [ ] Duplicate detection still works
- [ ] Transaction IDs handled correctly (generated + provided)
- [ ] Validation still prevents invalid data
- [ ] Metrics and logging unchanged

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing REST API | Low | High | Keep deprecated methods, extensive testing |
| MapStruct configuration errors | Medium | Medium | Incremental implementation, verify generated code |
| Test failures | Medium | Medium | Update tests phase-by-phase, run frequently |
| Performance degradation | Low | Medium | Benchmark before/after, profile if needed |
| Merge conflicts | Low | Low | Communicate with team, small PRs |
| Kafka type conversion bugs | Medium | Medium | Comprehensive mapper tests, type safety checks |

---

## Post-Implementation Tasks

1. **Documentation:**
   - Update README with new architecture
   - Document service layer contracts
   - Add sequence diagrams
   - Update API documentation (if needed)

2. **Monitoring:**
   - Verify metrics still recorded
   - Check logs for new patterns
   - Monitor error rates

3. **Performance:**
   - Benchmark transaction processing time
   - Compare before/after metrics
   - Optimize if needed

4. **Code Review:**
   - Peer review all changes
   - Architecture review with tech lead
   - Security review (if needed)

5. **Knowledge Transfer:**
   - Team walkthrough of new architecture
   - Document design decisions
   - Update onboarding materials

---

## Questions & Decisions

### Decided
- ✅ Use Hexagonal Architecture pattern
- ✅ Keep transactionId as optional in `CreateTransactionCommand`
- ✅ Validate at adapter layer (controller/consumer), not service
- ✅ Use `@EqualsAndHashCode(of = {"timestamp", "storeId", "tillId"})`
- ✅ Naming: `CreateTransactionCommand` and `TransactionResult`
- ✅ Package: `com.vega.techtest.service.command`

### Open Questions
- [ ] Should we update query methods to return `TransactionResult` or keep `TransactionResponse`?
- [ ] Do we need a separate DTO for API responses vs service results?
- [ ] Should we add service-layer validation or rely only on adapter validation?
- [ ] Kafka topic names and configuration?
- [ ] Dead letter queue strategy for failed Kafka messages?

---

## Timeline Estimate

| Phase | Effort | Dependencies |
|-------|--------|--------------|
| Phase 1: DTOs | 2-3 hours | None |
| Phase 2: Request Mapper | 2-3 hours | Phase 1 |
| Phase 3: Entity Mapper | 2-3 hours | Phase 1 |
| Phase 4: Service Layer | 3-4 hours | Phase 1-3 |
| Phase 5: Controller | 2-3 hours | Phase 4 |
| Phase 6: All Tests | 4-5 hours | Phase 5 |
| Phase 7: Cleanup | 1-2 hours | Phase 6 |
| **Total (REST refactoring)** | **16-23 hours** | |
| Phase 8: Kafka Mapper | 4-6 hours | Phase 7 |
| Phase 9: Kafka Consumer | 3-4 hours | Phase 8 |
| Phase 10: E2E Kafka Tests | 3-4 hours | Phase 9 |
| **Total (with Kafka)** | **26-37 hours** | |

**Recommended approach:** Complete Phases 1-7 first (REST refactoring), then add Kafka in a separate iteration.

---

## Conclusion

This refactoring implements a clean separation between infrastructure (REST, Kafka) and business logic (service layer) using Hexagonal Architecture. The service layer defines a clear contract (`CreateTransactionCommand` → `TransactionResult`) that is source-agnostic.

**Key Benefits:**
- Service layer reusable from REST API, Kafka, and future sources
- Single implementation of business logic
- Clear separation of concerns
- Easy to test and maintain
- Prepared for future Kafka consumer implementation

**Minimal Risk:**
- Phased implementation with rollback options
- Backward compatibility during migration
- Extensive testing at each phase
- No changes to external API contracts

The architecture follows industry best practices and sets up the codebase for scalable, maintainable growth.

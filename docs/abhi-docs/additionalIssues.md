# Additional Issues

This file documents issues and bugs discovered during the take-home test.



## Issue #4: Unused Database Indexes

**File:** `src/main/resources/db/changelog/001-transaction-schema.yaml`
**Severity:** Low (Performance/Maintenance)

### Problem

Two indexes exist but are never used by any query in the codebase:

1. **`idx_transaction_items_product_code`** on `transaction_items.product_code`
   - No query searches by `product_code`

2. **`idx_transactions_payment_method`** on `transactions.payment_method`
   - The repository defines `findByPaymentMethodOrderByTransactionTimestampDesc()` but this method is **never called** in TransactionService or TransactionController

These unused indexes:
- Consume disk space unnecessarily
- Slow down INSERT/UPDATE operations
- Add maintenance overhead

### Fix

Remove the unused indexes via a new Liquibase migration:
```yaml
- changeSet:
    id: drop-unused-indexes
    author: developer
    changes:
      - dropIndex:
          indexName: idx_transaction_items_product_code
          tableName: transaction_items
      - dropIndex:
          indexName: idx_transactions_payment_method
          tableName: transactions
```

Also consider removing the unused repository method `findByPaymentMethodOrderByTransactionTimestampDesc()` or implementing an endpoint that uses it.

---

## Issue #5: Suboptimal Indexes for Sorted Queries

**File:** `src/main/resources/db/changelog/001-transaction-schema.yaml`
**Severity:** Low (Performance)

### Problem

Several repository methods filter by one column and sort by `transaction_timestamp`:
- `findByStoreIdOrderByTransactionTimestampDesc`
- `findByCustomerIdOrderByTransactionTimestampDesc`
- `findByTillIdOrderByTransactionTimestampDesc`
- `findByPaymentMethodOrderByTransactionTimestampDesc`

Currently, there are separate single-column indexes for each filter column and for `transaction_timestamp`. This forces the database to:
1. Use the filter index to find matching rows
2. Perform a separate sort operation on the results

### Fix

Create composite indexes that cover both the filter and sort columns:
```sql
CREATE INDEX idx_store_timestamp ON transactions(store_id, transaction_timestamp DESC);
CREATE INDEX idx_customer_timestamp ON transactions(customer_id, transaction_timestamp DESC);
CREATE INDEX idx_till_timestamp ON transactions(till_id, transaction_timestamp DESC);
CREATE INDEX idx_payment_timestamp ON transactions(payment_method, transaction_timestamp DESC);
```

These composite indexes allow the database to return results in the correct order without a separate sort step, improving query performance especially as the table grows.

**Note:** After adding composite indexes, the original single-column indexes (`idx_transactions_store_id`, etc.) may become redundant and can potentially be removed.

---

## Issue #6: Item Total Mismatch Only Logs Warning

**File:** `src/main/java/com/vega/techtest/service/TransactionService.java`
**Lines:** 138-146
**Severity:** Medium (Data Integrity)

### Problem

When the calculated item total doesn't match the provided `totalAmount`, the system only logs a warning and proceeds:

```java
if (calculatedTotal.compareTo(request.getTotalAmount()) != 0) {
    logger.warn("Calculated total ({}) doesn't match provided total ({})",
            calculatedTotal, request.getTotalAmount());
    // Transaction proceeds anyway!
}
```

This allows transactions with incorrect totals to be stored, leading to:
- Data integrity issues
- Potential fraud (submitting lower total than item sum)
- Accounting discrepancies

### Fix

Either reject the transaction or auto-correct:

**Option A - Reject:**
```java
if (calculatedTotal.compareTo(request.getTotalAmount()) != 0) {
    throw new IllegalArgumentException(
        "Total amount mismatch: provided " + request.getTotalAmount() +
        " but items sum to " + calculatedTotal);
}
```

**Option B - Auto-correct with warning:**
```java
if (calculatedTotal.compareTo(request.getTotalAmount()) != 0) {
    logger.warn("Auto-correcting total from {} to {}", request.getTotalAmount(), calculatedTotal);
    request.setTotalAmount(calculatedTotal);
}
```

---

## Issue #7: Insufficient Input Validation

**File:** `src/main/java/com/vega/techtest/service/TransactionService.java`
**Lines:** 125-147
**Severity:** Medium (Data Quality)

### Problem

The `validateTransactionRequest` method only validates 3 fields:
- `storeId` - required
- `paymentMethod` - required
- `totalAmount` - required, must be > 0

**Not validated:**
| Field | Current State | Risk |
|-------|---------------|------|
| `customerId` | Nullable, no format check | Orphaned transactions, invalid references |
| `tillId` | Nullable, no format check | Cannot trace transaction source |
| `currency` | Defaults to "GBP", accepts anything | Invalid currency codes stored |
| `items` | No validation on individual items | Null prices, negative quantities possible |

### Fix

Add comprehensive validation:

```java
private void validateTransactionRequest(TransactionRequest request) {
    // Existing validations...

    // Add currency validation
    if (request.getCurrency() != null && !VALID_CURRENCIES.contains(request.getCurrency())) {
        throw new IllegalArgumentException("Invalid currency: " + request.getCurrency());
    }

    // Add item-level validation
    if (request.getItems() != null) {
        for (TransactionItemRequest item : request.getItems()) {
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Invalid unit price for item: " + item.getProductName());
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for item: " + item.getProductName());
            }
        }
    }
}
```

---

## Issue #8: No Idempotency Key Support

**File:** `src/main/java/com/vega/techtest/service/TransactionService.java`
**Severity:** High (Data Integrity / Financial Safety)

### Problem

The `/submit` endpoint has only basic duplicate detection via `transactionId`:

```java
if (transactionRepository.existsByTransactionId(transactionId)) {
    throw new IllegalArgumentException("Transaction ID already exists: " + transactionId);
}
```

**Issues:**
1. If client doesn't provide `transactionId`, a new UUID is generated → retries create duplicates
2. No standard idempotency key header support (`X-Idempotency-Key`)
3. Duplicate detection throws error instead of returning cached response
4. No time-bounded idempotency (same key should work for retry window, then expire)

**Risk:** Network timeouts or client retries can create duplicate financial transactions.

### Fix

Implement proper idempotency:

```java
@PostMapping("/submit")
public ResponseEntity<Map<String, Object>> submitTransaction(
        @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody TransactionRequest request) {

    // Check idempotency cache first
    if (idempotencyKey != null) {
        Optional<CachedResponse> cached = idempotencyCache.get(idempotencyKey);
        if (cached.isPresent()) {
            return cached.get().toResponse(); // Return same response
        }
    }

    // Process transaction...
    TransactionResponse response = transactionService.processTransaction(request);

    // Cache response for idempotency
    if (idempotencyKey != null) {
        idempotencyCache.put(idempotencyKey, response, Duration.ofHours(24));
    }

    return ResponseEntity.ok(...);
}
```

---

# Security Issues

## Security #1: No Authentication/Authorization

**Severity:** 🔴 Critical
**OWASP:** A01:2021 Broken Access Control

### Problem

No Spring Security dependency exists. All endpoints are publicly accessible without any authentication.

**IDOR (Insecure Direct Object Reference) Vulnerabilities:**
| Endpoint | Risk |
|----------|------|
| `/customer/{customerId}` | Anyone can view any customer's transaction history |
| `/store/{storeId}` | Anyone can view any store's financial data |
| `/till/{tillId}` | Anyone can view any till's transactions |
| `/{transactionId}` | Anyone can access any transaction by guessing/enumerating IDs |
| `/submit` | Anyone can submit fraudulent transactions |

### Fix

Add Spring Security with proper authentication:

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
```

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/transactions/health").permitAll()
                .requestMatchers("/api/transactions/customer/**").hasRole("CUSTOMER_SERVICE")
                .requestMatchers("/api/transactions/store/**").hasRole("STORE_MANAGER")
                .requestMatchers("/api/transactions/submit").hasRole("TILL_SYSTEM")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

---

## Security #2: Hardcoded Database Credentials

**File:** `src/main/resources/application.yml`
**Severity:** 🔴 Critical
**OWASP:** A07:2021 Identification and Authentication Failures

### Problem

```yaml
datasource:
  username: postgres
  password: postgres
```

Database credentials are hardcoded in source code. If the repository is exposed, the database is immediately compromised.

### Fix

Use environment variables or secrets management:

```yaml
datasource:
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}
```

Or use Spring Cloud Vault / AWS Secrets Manager / HashiCorp Vault for production.

---

## Security #3: No Rate Limiting

**Severity:** 🔴 Critical
**OWASP:** A04:2021 Insecure Design

### Problem

All endpoints can be called unlimited times:
- `POST /submit` - unlimited transaction creation (financial fraud, resource exhaustion)
- `POST /sample` - unlimited test data generation
- `GET /customer/{id}` - brute-force ID enumeration
- All endpoints - DoS vulnerability

### Fix

Add rate limiting with Bucket4j or Spring Cloud Gateway:

```java
@Bean
public RateLimiter rateLimiter() {
    return RateLimiter.of("api", RateLimiterConfig.custom()
        .limitForPeriod(100)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ofSeconds(1))
        .build());
}
```

Or use API Gateway (Kong, AWS API Gateway) with rate limiting policies.

---

## Security #4: Exposed Actuator Endpoints Without Authentication

**File:** `src/main/resources/application.yml`
**Severity:** 🟠 High
**OWASP:** A01:2021 Broken Access Control

### Problem

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always  # Exposes internal details!
```

Actuator endpoints expose sensitive information:
- `/actuator/health` - database connection status, disk space
- `/actuator/metrics` - internal application metrics
- `/actuator/prometheus` - full metrics for scraping

### Fix

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  endpoint:
    health:
      show-details: when_authorized  # Only for authenticated users
```

Add security for actuator:
```java
.requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")
```

---

## Security #5: Bean Validation Not Used

**File:** `src/main/java/com/vega/techtest/dto/TransactionRequest.java`
**Severity:** 🟠 High
**OWASP:** A03:2021 Injection

### Problem

The project has `spring-boot-starter-validation` dependency but doesn't use it:

```java
// Controller - No @Valid annotation
public ResponseEntity<...> submitTransaction(@RequestBody TransactionRequest request)

// DTO - No validation annotations
private String storeId;        // Should be @NotBlank @Size(max=50)
private BigDecimal totalAmount; // Should be @NotNull @Positive @Digits
```

### Fix

```java
// DTO
public class TransactionRequest {
    @NotBlank(message = "Store ID is required")
    @Size(max = 50)
    @Pattern(regexp = "^STORE-\\d+$")
    private String storeId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;

    @Size(max = 1000, message = "Too many items")
    @Valid  // Validate nested items
    private List<TransactionItemRequest> items;
}

// Controller
public ResponseEntity<...> submitTransaction(@Valid @RequestBody TransactionRequest request)
```

---

## Security #6: Metric Tag Injection (Cardinality Bomb)

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Lines:** 102-118
**Severity:** 🟠 High
**OWASP:** A04:2021 Insecure Design

### Problem

User input is used directly in metric tags:

```java
Counter.builder("transaction_submissions_by_store")
    .tag("store_id", request.getStoreId())  // Attacker controls this!
    .register(meterRegistry)
    .increment();
```

An attacker can send millions of unique `storeId` values, causing:
- Memory exhaustion (each unique tag creates new metric)
- Prometheus/metrics DB bloat
- Monitoring system DoS

### Fix

Validate and sanitize before using in tags:

```java
private static final Set<String> VALID_STORES = Set.of("STORE-001", "STORE-002", ...);

String storeId = VALID_STORES.contains(request.getStoreId())
    ? request.getStoreId()
    : "UNKNOWN";

Counter.builder("transaction_submissions_by_store")
    .tag("store_id", storeId)
    .register(meterRegistry)
    .increment();
```

---

## Security #7: Test Endpoint in Production

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Line:** 290
**Severity:** 🟠 High
**OWASP:** A04:2021 Insecure Design

### Problem

```java
@PostMapping("/sample")
public ResponseEntity<Map<String, Object>> createSampleTransaction()
```

This test endpoint:
- Creates real data in production database
- Has no environment check
- Is publicly accessible

### Fix

Remove or protect with profile:

```java
@Profile("!production")  // Only available in non-prod
@PostMapping("/sample")
public ResponseEntity<...> createSampleTransaction()
```

Or remove entirely from production builds.

---

## Security #8: Error Message Information Disclosure

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Severity:** 🟡 Medium
**OWASP:** A04:2021 Insecure Design

### Problem

```java
// Line 137
return ResponseEntity.badRequest().body(Map.of(
    "error", e.getMessage()  // Exposes internal error details
));

// Line 334
"error", e.getMessage()  // Same issue
```

Exception messages may contain:
- SQL syntax (reveals database structure)
- Stack traces
- Internal class names
- File paths

### Fix

```java
return ResponseEntity.badRequest().body(Map.of(
    "error", "Invalid request data"  // Generic message
    // Log the actual error server-side only
));
```

---

## Security #9: Log Injection

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Severity:** 🟡 Medium
**OWASP:** A09:2021 Security Logging and Monitoring Failures

### Problem

```java
// Line 92
logger.info("Received transaction submission from till: {}", request.getTillId());
```

If `tillId` contains newlines or log format characters, attacker can:
- Inject fake log entries
- Forge audit trails
- Hide malicious activity

Example attack: `tillId = "TILL-1\n2024-01-01 INFO - Admin password reset successful"`

### Fix

Sanitize user input before logging:

```java
String safeTillId = request.getTillId().replaceAll("[\n\r\t]", "_");
logger.info("Received transaction submission from till: {}", safeTillId);
```

Or use structured logging (JSON format) which escapes special characters.

---

## Security #10: No Request Size Limits

**File:** `src/main/java/com/vega/techtest/dto/TransactionRequest.java`
**Severity:** 🟡 Medium
**OWASP:** A04:2021 Insecure Design

### Problem

```java
private List<TransactionItemRequest> items;  // No size limit!
```

Attacker can send request with millions of items → memory exhaustion, OOM crash.

### Fix

```yaml
# application.yml
spring:
  servlet:
    multipart:
      max-request-size: 1MB
      max-file-size: 1MB
server:
  tomcat:
    max-http-post-size: 1MB
```

```java
// DTO validation
@Size(max = 1000, message = "Maximum 1000 items allowed")
private List<TransactionItemRequest> items;
```

---

## Security #11: Missing Security Headers

**Severity:** 🟡 Medium
**OWASP:** A05:2021 Security Misconfiguration

### Problem

No security headers configured:
- `Content-Security-Policy` - prevents XSS
- `X-Content-Type-Options: nosniff` - prevents MIME sniffing
- `X-Frame-Options: DENY` - prevents clickjacking
- `Strict-Transport-Security` - enforces HTTPS

### Fix

```java
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response,
                                   Object handler, ModelAndView modelAndView) {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("X-XSS-Protection", "1; mode=block");
                response.setHeader("Content-Security-Policy", "default-src 'self'");
            }
        });
    }
}
```

---

## Security #12: No HTTPS Enforcement

**File:** `src/main/resources/application.yml`
**Severity:** 🟡 Medium
**OWASP:** A02:2021 Cryptographic Failures

### Problem

```yaml
server:
  port: 8080  # HTTP only
```

Financial transaction data transmitted in cleartext. Vulnerable to:
- Man-in-the-middle attacks
- Network sniffing
- Data interception

### Fix

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12

# Force HTTPS redirect
security:
  require-ssl: true
```

---

## Summary

| Issue | Location | Impact |
|-------|----------|--------|
| Pre-increment `++count` | Calculator.java:9 | Average divided by count+1 |
| Wrong identity value | TransactionController.java:367 | Total always +1 |
| Misleading method name | Calculator.java:8 | Code readability |
| Unused indexes | transaction_items.product_code, transactions.payment_method | Wasted storage & slower writes |
| Suboptimal indexes | transactions table | Inefficient sorted queries |
| Total mismatch warning only | TransactionService.java:143 | Bad data accepted |
| Insufficient validation | TransactionService.java:125 | Invalid data stored |
| No idempotency key | TransactionController/Service | Duplicate transactions on retry |

### Security Issues Summary

| # | Issue | Severity | OWASP Category |
|---|-------|----------|----------------|
| S1 | No Authentication/Authorization | 🔴 Critical | A01:2021 Broken Access Control |
| S2 | Hardcoded Database Credentials | 🔴 Critical | A07:2021 Auth Failures |
| S3 | No Rate Limiting | 🔴 Critical | A04:2021 Insecure Design |
| S4 | Exposed Actuator Endpoints | 🟠 High | A01:2021 Broken Access Control |
| S5 | Bean Validation Not Used | 🟠 High | A03:2021 Injection |
| S6 | Metric Tag Injection | 🟠 High | A04:2021 Insecure Design |
| S7 | Test Endpoint in Production | 🟠 High | A04:2021 Insecure Design |
| S8 | Error Info Disclosure | 🟡 Medium | A04:2021 Insecure Design |
| S9 | Log Injection | 🟡 Medium | A09:2021 Logging Failures |
| S10 | No Request Size Limit | 🟡 Medium | A04:2021 Insecure Design |
| S11 | Missing Security Headers | 🟡 Medium | A05:2021 Misconfiguration |
| S12 | No HTTPS Enforcement | 🟡 Medium | A02:2021 Crypto Failures |

**Critical Issues (#1, #2, #3):** Affect `/api/transactions/stats/{storeId}` endpoint, causing incorrect statistics.

**Data Integrity Issues (#6, #7, #8):** Allow invalid or duplicate data into the system.

**Performance Issues (#4, #5):** Database optimization opportunities.

**Security Issues (S1-S12):** Multiple OWASP Top 10 vulnerabilities requiring immediate attention.

## Data Correction Note

Fixing the calculator/statistics logic does not change existing stored data. If any derived totals or aggregates have
been persisted or exported based on the buggy calculations, we will need a migration/backfill script to recompute and
correct those records.

---

# Clean Code Issues

## Clean Code #1: Controller Violates Single Responsibility Principle

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Severity:** High

### Problem

The `TransactionController` handles **6+ responsibilities** instead of just HTTP routing:

```java
@RestController
public class TransactionController {
    // Responsibility 1: HTTP request handling (correct - its job)
    // Responsibility 2: Metrics initialization (lines 56-80)
    // Responsibility 3: Metrics recording (scattered in every method)
    // Responsibility 4: Business logic - statistics calculation (lines 368-374)
    // Responsibility 5: Sample data generation (lines 295-309)
    // Responsibility 6: Error response formatting (repeated 9 times)
    // Responsibility 7: Logging (scattered throughout)
}
```

A controller should only:
- Accept HTTP requests
- Delegate to services
- Return HTTP responses

### Fix

Split into focused classes:

```java
// TransactionController - HTTP handling only
@RestController
public class TransactionController {
    private final TransactionService transactionService;
    private final StatisticsService statisticsService;

    @GetMapping("/stats/{storeId}")
    public ResponseEntity<StoreStatistics> getStats(@PathVariable String storeId) {
        return ResponseEntity.ok(statisticsService.calculateForStore(storeId));
    }
}

// StatisticsService - business logic
@Service
public class StatisticsService {
    public StoreStatistics calculateForStore(String storeId) {
        // Statistics calculation logic here
    }
}

// MetricsService or use AOP - metrics recording
// SampleDataService - test data generation
```

---

## Clean Code #2: Service Has Multiple Concerns

**File:** `src/main/java/com/vega/techtest/service/TransactionService.java`
**Severity:** Medium

### Problem

`TransactionService` handles multiple concerns:

```java
public class TransactionService {
    // Concern 1: Transaction processing (correct - its job)
    // Concern 2: Validation (lines 125-148) - should be separate
    // Concern 3: Entity-DTO conversion (lines 150-181) - should be mapper
    // Concern 4: ID generation (lines 121-123) - could be separate
}
```

### Fix

```java
// Separate validator
@Component
public class TransactionValidator {
    public void validate(TransactionRequest request) {
        // All validation logic
    }
}

// Separate mapper (or use MapStruct)
@Component
public class TransactionMapper {
    public TransactionEntity toEntity(TransactionRequest request) { ... }
    public TransactionResponse toResponse(TransactionEntity entity) { ... }
}

// Clean service
@Service
public class TransactionService {
    private final TransactionRepository repository;
    private final TransactionValidator validator;
    private final TransactionMapper mapper;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        validator.validate(request);
        TransactionEntity entity = mapper.toEntity(request);
        TransactionEntity saved = repository.save(entity);
        return mapper.toResponse(saved);
    }
}
```

---

## Clean Code #3: Business Logic in Controller

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Lines:** 348-400
**Severity:** High

### Problem

Statistics calculation logic is in the controller instead of service layer:

```java
@GetMapping("/stats/{storeId}")
public ResponseEntity<Map<String, Object>> getTransactionStats(@PathVariable String storeId) {
    // ❌ Business logic in controller!
    List<TransactionResponse> transactions = transactionService.getTransactionsByStore(storeId);

    int totalTransactions = transactions.size();
    BigDecimal totalAmount = transactions.stream()
            .map(TransactionResponse::getTotalAmount)
            .reduce(BigDecimal.ONE, BigDecimal::add);  // Also has bug

    BigDecimal averageAmount = calculateTotalAmount(totalAmount, totalTransactions);
    // ...
}
```

### Fix

Move to service layer:

```java
// Controller - thin, just routing
@GetMapping("/stats/{storeId}")
public ResponseEntity<StoreStatistics> getTransactionStats(@PathVariable String storeId) {
    return ResponseEntity.ok(statisticsService.getStoreStatistics(storeId));
}

// Service - contains business logic
@Service
public class StatisticsService {
    public StoreStatistics getStoreStatistics(String storeId) {
        List<TransactionEntity> transactions = transactionRepository.findByStoreId(storeId);

        BigDecimal totalAmount = transactions.stream()
                .map(TransactionEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalAmount.divide(
                BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);

        return new StoreStatistics(storeId, transactions.size(), totalAmount, averageAmount);
    }
}
```

---

## Clean Code #4: No Global Exception Handler

**File:** Missing `GlobalExceptionHandler.java`
**Severity:** High

### Problem

No `@ControllerAdvice` exists. Each controller method has its own try-catch block, repeated **9 times**:

```java
// This pattern appears 9 times in TransactionController:
try {
    // business logic
} catch (IllegalArgumentException e) {
    logger.warn("Invalid request: {}", e.getMessage());
    transactionErrorCounter.increment();
    return ResponseEntity.badRequest().body(Map.of(
            "status", "error",
            "message", "Invalid transaction data",
            "error", e.getMessage()
    ));
} catch (Exception e) {
    logger.error("Error processing", e);
    transactionErrorCounter.increment();
    return ResponseEntity.internalServerError().body(Map.of(
            "status", "error",
            "message", "Failed to process",
            "error", "Internal server error"
    ));
}
```

### Fix

Create centralized exception handler:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleValidationError(IllegalArgumentException e) {
        logger.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiError("error", "Invalid request data", e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericError(Exception e) {
        logger.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(new ApiError("error", "Internal server error", null));
    }
}

// Clean controller methods become:
@GetMapping("/{transactionId}")
public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
    return transactionService.getTransactionById(transactionId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
}
```

---

## Clean Code #5: DRY Violation - Repeated Error Handling

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Severity:** High

### Problem

The same error handling pattern is repeated **9 times** across different methods:

**Locations:**
- Lines 129-149 (`submitTransaction`)
- Lines 166-176 (`getTransaction`)
- Lines 193-203 (`getTransactionsByStore`)
- Lines 220-230 (`getTransactionsByCustomer`)
- Lines 247-257 (`getTransactionsByTill`)
- Lines 277-287 (`getTransactionsByDateRange`)
- Lines 326-335 (`createSampleTransaction`)
- Lines 389-398 (`getTransactionStats`)

Each block has:
```java
} catch (Exception e) {
    logger.error("Error ...", e);
    transactionErrorCounter.increment();
    sample.stop(timer);
    return ResponseEntity.internalServerError().body(Map.of(
            "status", "error",
            "message", "Failed to ...",
            "error", "Internal server error"
    ));
}
```

### Fix

Use `@ControllerAdvice` (see Clean Code #4) to eliminate all 9 repetitions.

---

## Clean Code #6: DRY Violation - Repeated Metrics Pattern

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Severity:** High

### Problem

Same metrics boilerplate repeated in every method:

```java
// START - appears 9 times
Timer.Sample sample = Timer.start(meterRegistry);

// END - appears 9 times (in try and catch blocks)
transactionRetrievalCounter.increment();
sample.stop(transactionRetrievalTimer);
```

Total: ~50+ lines of repeated metrics code polluting business logic.

### Fix

Use AOP or Micrometer's `@Timed` annotation:

```java
@Aspect
@Component
public class MetricsAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(timed)")
    public Object recordMetrics(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Object result = joinPoint.proceed();
            Counter.builder(timed.value() + ".success").register(meterRegistry).increment();
            return result;
        } catch (Exception e) {
            Counter.builder(timed.value() + ".error").register(meterRegistry).increment();
            throw e;
        } finally {
            sample.stop(Timer.builder(timed.value() + ".duration").register(meterRegistry));
        }
    }
}

// Controller becomes clean:
@Timed("transaction.retrieval")
@GetMapping("/{transactionId}")
public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
    return transactionService.getTransactionById(transactionId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new EntityNotFoundException("Not found"));
}
```

---

## Clean Code #7: DRY Violation - Duplicated Transaction ID Generation

**File:** `TransactionController.java` and `TransactionService.java`
**Severity:** Low

### Problem

Transaction ID generation logic is duplicated:

```java
// In TransactionService (line 122):
private String generateTransactionId() {
    return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}

// In TransactionController /sample endpoint (line 296):
request.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
```

### Fix

Single source of truth:

```java
// Use the service method
@PostMapping("/sample")
public ResponseEntity<...> createSampleTransaction() {
    TransactionRequest request = sampleDataService.createSampleRequest();
    // Let the service generate the ID
    return ResponseEntity.ok(transactionService.processTransaction(request));
}
```

---

## Clean Code #8: Severely Lacking Test Coverage

**Directory:** `src/test/java/`
**Severity:** Critical

### Problem

Only **2 test files** exist for the entire application:

```
src/test/java/com/vega/techtest/
├── KafkaMessageDeserializationTest.java  // Only tests DTO deserialization
└── TechTestApplicationTests.java          // Only tests context loads
```

**Missing Tests:**

| Component | Test Status | Risk |
|-----------|-------------|------|
| `TransactionController` | ❌ No tests | Endpoints untested |
| `TransactionService` | ❌ No tests | Business logic untested |
| `TransactionRepository` | ❌ No tests | Queries untested |
| `Calculator` | ❌ No tests | **Has known bugs!** |
| `TransactionValidator` | ❌ No tests | Validation untested |
| Integration tests | ❌ None | End-to-end untested |

### The Calculator Bug Would Be Caught Immediately

```java
// This simple test would catch both bugs in Calculator:
@Test
void calculateAverage_shouldDivideCorrectly() {
    BigDecimal result = Calculator.calculateTotalAmount(new BigDecimal("100"), 4);

    // Expected: 100 / 4 = 25.00
    // Actual:   100 / 5 = 20.00 (due to ++count bug)
    assertEquals(new BigDecimal("25.00"), result);  // FAILS!
}

@Test
void calculateAverage_withSingleTransaction() {
    BigDecimal result = Calculator.calculateTotalAmount(new BigDecimal("50"), 1);

    // Expected: 50 / 1 = 50.00
    // Actual:   50 / 2 = 25.00 (due to ++count bug)
    assertEquals(new BigDecimal("50.00"), result);  // FAILS!
}
```

### Fix

Add comprehensive tests:

```java
// Controller tests
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean TransactionService transactionService;

    @Test
    void submitTransaction_shouldReturn200() { ... }

    @Test
    void submitTransaction_withInvalidData_shouldReturn400() { ... }

    @Test
    void getTransaction_notFound_shouldReturn404() { ... }
}

// Service tests
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock TransactionRepository repository;
    @InjectMocks TransactionService service;

    @Test
    void processTransaction_shouldSaveAndReturnResponse() { ... }

    @Test
    void processTransaction_withDuplicateId_shouldThrow() { ... }
}

// Repository tests
@DataJpaTest
class TransactionRepositoryTest {
    @Autowired TransactionRepository repository;

    @Test
    void findByStoreId_shouldReturnTransactions() { ... }
}

// Integration tests
@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void fullTransactionFlow() { ... }
}
```

---

## Clean Code #9: No Logging Aspect

**Severity:** Medium

### Problem

Logging is manually scattered throughout the codebase:

```java
// TransactionController - manual logging everywhere
logger.info("Received transaction submission from till: {}", request.getTillId());
logger.info("Successfully processed transaction: {}", response.getTransactionId());
logger.error("Error retrieving transaction: {}", transactionId, e);
logger.warn("Invalid transaction request: {}", e.getMessage());

// TransactionService - more manual logging
logger.info("Processing transaction request: {}", request.getTransactionId());
logger.info("Successfully saved transaction: {}", transactionId);
logger.warn("Calculated total ({}) doesn't match provided total ({})");
```

### Fix

Use AOP for consistent logging:

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.vega.techtest.controller.*.*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("Entering {}.{} with args: {}",
                joinPoint.getTarget().getClass().getSimpleName(), methodName, args);

        try {
            Object result = joinPoint.proceed();
            log.info("Exiting {}.{} with result: {}",
                    joinPoint.getTarget().getClass().getSimpleName(), methodName, result);
            return result;
        } catch (Exception e) {
            log.error("Exception in {}.{}: {}",
                    joinPoint.getTarget().getClass().getSimpleName(), methodName, e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.vega.techtest.service.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        // Similar logging for service layer
    }
}
```

---

## Clean Code #10: No Metrics Aspect

**Severity:** High

### Problem

Metrics code pollutes every controller method (~30+ lines per method):

```java
@PostMapping("/submit")
public ResponseEntity<...> submitTransaction(@RequestBody TransactionRequest request) {
    Timer.Sample sample = Timer.start(meterRegistry);  // Boilerplate

    try {
        // Actual business logic (few lines)
        TransactionResponse response = transactionService.processTransaction(request);

        // Metrics boilerplate (15+ lines)
        transactionSubmissionCounter.increment();
        sample.stop(transactionSubmissionTimer);
        transactionAmountSummary.record(response.getTotalAmount().doubleValue());
        transactionItemCountSummary.record(response.getItems().size());
        Counter.builder("transaction_submissions_by_store")
                .tag("store_id", request.getStoreId())
                .register(meterRegistry).increment();
        // ... more metrics code ...

    } catch (Exception e) {
        transactionErrorCounter.increment();  // More metrics
        sample.stop(transactionSubmissionTimer);  // More metrics
        // ...
    }
}
```

### Fix

Use AOP or Micrometer's built-in `@Timed`:

```java
// Option 1: Use Micrometer's @Timed
@Timed(value = "transaction.submission", description = "Time to process submissions")
@Counted(value = "transaction.submissions.total", description = "Total submissions")
@PostMapping("/submit")
public ResponseEntity<TransactionResponse> submitTransaction(@RequestBody TransactionRequest request) {
    return ResponseEntity.ok(transactionService.processTransaction(request));
}

// Option 2: Custom metrics aspect
@Aspect
@Component
public class TransactionMetricsAspect {

    @AfterReturning(pointcut = "execution(* com.vega.techtest.domain.transaction.service.TransactionService.processTransaction(..))",
                    returning = "response")
    public void recordTransactionMetrics(TransactionResponse response) {
        transactionAmountSummary.record(response.getTotalAmount().doubleValue());
        transactionItemCountSummary.record(response.getItems().size());
    }
}
```

---

## Clean Code Summary

| # | Issue | Category | Severity |
|---|-------|----------|----------|
| CC1 | Controller has 6+ responsibilities | SRP | High |
| CC2 | Service does validation + mapping + processing | SRP | Medium |
| CC3 | Statistics logic in controller | Separation of Concerns | High |
| CC4 | No global exception handler | Separation of Concerns | High |
| CC5 | Error handling repeated 9 times | DRY | High |
| CC6 | Metrics pattern repeated 9 times | DRY | High |
| CC7 | Transaction ID generation duplicated | DRY | Low |
| CC8 | Only 2 test files, no unit/integration tests | Testing | Critical |
| CC9 | Manual logging scattered everywhere | AOP | Medium |
| CC10 | Metrics code pollutes business logic | AOP | High |

### Impact

**Without these clean code practices:**
- Controller methods are 50-100 lines instead of 5-10 lines
- Same bugs can exist in multiple places (DRY violations)
- Bugs go undetected (no tests)
- Changes require modifications in multiple places
- Code is hard to read and maintain
- New developers struggle to understand the codebase

# Step 1: Controller Safety Net (Integration Testing)

## Objective
Create a comprehensive integration test suite for `TransactionController` to preserve behavior, HTTP contracts, and Micrometer metrics before refactoring.

## Core Directives
* **Framework:** Use `@WebMvcTest(TransactionController.class)`.
* **Isolation:** Use `@MockBean` for `TransactionService` to avoid database side effects.
* **Assertions:** Use **AssertJ** for state checks and **MockMvc** for HTTP checks.

## Required Test Cases
1. **Submission Success:** Call `/api/transactions/submit` with valid data. Verify `200 OK`, JSON structure, and that `transaction_submissions_total` increments by 1 in the `MeterRegistry`.
2. **Submission Error:** Mock the service to throw `IllegalArgumentException`. Verify `400 Bad Request` and that `transaction_errors_total` increments.
3. **Metrics Tags:** Verify that successful submissions record tags for `store_id`, `till_id`, and `payment_method`.
4. **Stats Verification:** Call `/api/transactions/stats/{storeId}`. Verify it returns the correct average by mocking the service response.

## Error Mapping Coverage
Add tests that lock down status codes and error payloads for each endpoint:
1. **POST `/api/transactions/submit`:** `IllegalArgumentException` -> `400`; any other exception -> `500`.
2. **GET `/api/transactions/{transactionId}`:** empty result -> `404`; any exception -> `500`.
3. **GET `/api/transactions/store/{storeId}`:** any exception -> `500`.
4. **GET `/api/transactions/customer/{customerId}`:** any exception -> `500`.
5. **GET `/api/transactions/till/{tillId}`:** any exception -> `500`.
6. **GET `/api/transactions/date-range`:** any exception -> `500`; invalid date params should yield `400` from Spring MVC before controller logic.
7. **POST `/api/transactions/sample`:** any exception -> `500`.
8. **GET `/api/transactions/health`:** always `200` unless the framework fails.
9. **GET `/api/transactions/stats/{storeId}`:** any exception -> `500`; empty list -> `200` with zeroed totals.

# Step 2: Controller Refactoring (Cleanup & Delegation)

## Phase A: Cross-Cutting Concerns
* **Global Exception Handling:** Create a `@ControllerAdvice` class. Move all `try-catch` blocks from the Controller to `@ExceptionHandler` methods.
* **AOP Metrics:** Replace manual `Timer.Sample` code with the `@Timed` annotation on all relevant Controller methods.
* **Centralized Logging:** Ensure the `@ControllerAdvice` handles logging of errors to keep the Controller methods clean.

## Phase B: Logic Delegation
* **Stats Refactor:** Move the calculation logic (averages and sums) from `getTransactionStats` into `TransactionService`.
* **New DTO:** The Controller should receive a calculated statistics object from the Service and simply return it.

## Phase C: Transactional Mapping
* **Mapper Implementation:** Create a dedicated `TransactionMapper` (Component). Move the logic from `convertToResponse` into this mapper.
* **Streamline Retrieval:** Refactor endpoints (Store/Till/Customer) to call the Service and return results without manual stream mapping in the Controller.
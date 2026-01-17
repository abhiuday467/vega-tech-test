# Development Plan - Sequential Execution

This document outlines the sequential execution steps for implementing the Hexagonal Architecture refactoring as defined in REFACTORING_PLAN.md.

## Phase 1: Create New DTOs (2-3 hours)

### Steps:
1. Create directory: `src/main/java/com/vega/techtest/service/command/`
2. Create `CreateTransactionCommand.java` with Lombok annotations, optional transactionId
3. Create `TransactionResult.java` with all required fields
4. Create `TransactionItem.java` for input items
5. Create `TransactionItemResult.java` for output items
6. Run `mvn clean compile` to verify compilation
7. Create basic tests for DTO construction

### Acceptance Criteria:
- [ ] All DTOs compile without errors
- [ ] Lombok/Builder annotations work correctly
- [ ] Equals/HashCode based on specified fields
- [ ] DTOs are immutable

---

## Phase 2: Create Request Mapper (2-3 hours)

### Steps:
1. Create `TransactionRequestMapper.java` in `src/main/java/com/vega/techtest/mapper/`
2. Define MapStruct interface with `toCommand()` method
3. Run `mvn clean compile` to generate MapStruct implementation
4. Verify generated code in `target/generated-sources/annotations/`
5. Create `TransactionRequestMapperTest.java`
6. Test field mappings and null handling
7. Run tests: `mvn test -Dtest=TransactionRequestMapperTest`

### Acceptance Criteria:
- [ ] Mapper compiles and generates implementation
- [ ] All fields map correctly
- [ ] Null values handled safely
- [ ] Tests pass with 100% coverage

---

## Phase 3: Update Entity Mapper (2-3 hours)

### Steps:
1. Open `TransactionEntityMapper.java`
2. Add new methods: `toEntity(CreateTransactionCommand)`, `toResult(TransactionEntity)`, item mappings
3. Mark old methods as `@Deprecated` (keep them for backward compatibility)
4. Run `mvn clean compile` to regenerate MapStruct implementation
5. Update `TransactionEntityMapperTest.java` with new test cases
6. Run tests to verify both old and new methods work

### Acceptance Criteria:
- [ ] New mapper methods work correctly
- [ ] Old methods still work (deprecated but functional)
- [ ] MapStruct generates correct implementation
- [ ] Tests pass

---

## Phase 4: Update Service Layer (3-4 hours) ⚠️ High Risk

### Steps:
1. Open `TransactionService.java`
2. Change method signature: `processTransaction(CreateTransactionCommand command)`
3. Update return type to `TransactionResult`
4. Update logic to handle optional transactionId (generate if null, use if provided)
5. Update mapper calls from old to new methods
6. Open `TransactionServiceTest.java`
7. Replace all `TransactionRequest` with `CreateTransactionCommand` in tests
8. Replace all `TransactionResponse` with `TransactionResult` assertions
9. Update all mock configurations for new mapper methods
10. Run all service tests: `mvn test -Dtest=TransactionServiceTest`
11. Fix any failing tests

### Acceptance Criteria:
- [ ] Service method accepts `CreateTransactionCommand`
- [ ] Service returns `TransactionResult`
- [ ] TransactionId handling (optional) works correctly
- [ ] Duplicate detection unchanged
- [ ] All service tests pass

---

## Phase 5: Update Controller Layer (2-3 hours)

### Steps:
1. Open `TransactionController.java`
2. Inject `TransactionRequestMapper` dependency
3. Add mapping line: `CreateTransactionCommand command = transactionRequestMapper.toCommand(request);`
4. Update service call to pass `command` instead of `request`
5. Update response handling to use `TransactionResult`
6. Open `TransactionControllerTest.java`
7. Add `@MockBean` for `TransactionRequestMapper`
8. Update all test mocks to include mapper behavior
9. Update service method expectations
10. Run controller tests: `mvn test -Dtest=TransactionControllerTest`

### Acceptance Criteria:
- [ ] Controller uses new mapper
- [ ] Validation still works at API boundary
- [ ] Response format unchanged
- [ ] All controller tests pass
- [ ] Integration tests pass

---

## Phase 6: Run Full Test Suite (4-5 hours)

### Steps:
1. Run complete test suite: `mvn clean test`
2. Identify all failing tests
3. Update integration tests if any exist
4. Fix remaining test failures one by one
5. Verify test coverage maintained
6. Run final verification: `mvn clean verify`

### Acceptance Criteria:
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Test coverage maintained or improved
- [ ] No flaky tests

---

## Phase 7: Cleanup (1-2 hours)

### Steps:
1. Remove `@Deprecated` methods from `TransactionEntityMapper.java`
2. Remove any old test helper methods no longer needed
3. Update JavaDocs and comments
4. Run final test suite: `mvn clean test`
5. Review all changes

### Acceptance Criteria:
- [ ] No deprecated code remains
- [ ] Documentation updated
- [ ] Code review approved
- [ ] All tests pass

---

## After Each Phase:

- ✅ Verify compilation succeeds
- ✅ Run relevant tests
- ✅ Commit changes with descriptive message
- ✅ Mark phase as complete before moving to next

---

## Safety Measures:

- Git commit after each successful phase for easy rollback
- Keep deprecated methods during Phases 3-5 for safety
- Run tests frequently to catch issues early
- If any phase fails critically, can rollback to previous commit

---

## Progress Tracking:

- [ ] Phase 1: Create New DTOs
- [ ] Phase 2: Create Request Mapper
- [ ] Phase 3: Update Entity Mapper
- [ ] Phase 4: Update Service Layer
- [ ] Phase 5: Update Controller Layer
- [ ] Phase 6: Run Full Test Suite
- [ ] Phase 7: Cleanup

---

## Notes:

- Total estimated time: 16-23 hours
- Phases 8-10 (Kafka implementation) will be addressed in future iteration
- Each phase builds on the previous one
- No phase should be skipped

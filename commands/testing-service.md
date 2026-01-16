# Repository Layer Testing Instructions
**Context:** [ARGUMENT: Pass specific method names or new query logic here]

## Core Directives
* **Framework:** Use `@DataJpaTest` with `@ActiveProfiles("test")` for slice testing.
* **Assertions:** Use **AssertJ** `isEqualByComparingTo()` for all BigDecimal comparisons to avoid scale mismatches.
* **Mandatory Scenarios:**
    * **Aggregation:** Verify that `GROUP BY` logic (e.g., store totals) correctly isolates data without cross-contamination.
    * **Boundaries:** Test `ZonedDateTime` ranges to ensure they are inclusive of start/end timestamps.
    * **Empty States:** Ensure custom `@Query` methods return empty collections or zeroed results instead of `null` when no data exists.
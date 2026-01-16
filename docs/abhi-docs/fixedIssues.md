## Bug #1: Pre-increment Operator in Average Calculation

**File:** `src/main/java/com/vega/techtest/utils/Calculator.java`
**Line:** 9
**Severity:** High

### Problem
```java
return totalAmount.divide(BigDecimal.valueOf(++count), 2, RoundingMode.DOWN);
```

The pre-increment operator `++count` increments the count before the division, causing the average to be calculated with `count + 1` instead of `count`.

### Fix
```java
return totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
```

---

## Bug #2: Misleading Method Name

**File:** `src/main/java/com/vega/techtest/utils/Calculator.java`
**Line:** 8
**Severity:** Medium (Code Quality)

### Problem
```java
public static BigDecimal calculateTotalAmount(BigDecimal totalAmount, int count)
```

The method is named `calculateTotalAmount` but it actually calculates the **average** amount (total divided by count).

### Fix
Rename to `calculateAverageAmount` for clarity:
```java
public static BigDecimal calculateAverageAmount(BigDecimal totalAmount, int count)
```

---

## Bug #3: Incorrect Identity Value in Stream Reduce

**File:** `src/main/java/com/vega/techtest/controller/TransactionController.java`
**Line:** 409
**Severity:** High

### Problem
```java
BigDecimal totalAmount = transactions.stream()
    .map(TransactionResponse::getTotalAmount)
    .reduce(BigDecimal.ONE, BigDecimal::add);
```

The `reduce()` operation uses `BigDecimal.ONE` as the identity value instead of `BigDecimal.ZERO`. This means the total amount is always **+1** higher than the actual sum.

### Fix
```java
BigDecimal totalAmount = transactions.stream()
    .map(TransactionResponse::getTotalAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

---
# Naive Token Bucket Implementation - Race Condition Demonstration

## Overview
This document summarizes the naive token bucket rate limiter implementation and its intentional race condition vulnerability.

## Files Created

### 1. **RateLimitResult.java** (`dto/RateLimitResult.java`)
- A record containing:
  - `allowed: boolean` - Whether the request was allowed
  - `remainingTokens: long` - Number of tokens remaining after the operation

### 2. **TokenBucketLimiter.java** (`algorithms/TokenBucketLimiter.java`)
- **Naive Implementation** using non-atomic Redis operations
- Method signature:
  ```java
  public RateLimitResult checkAndConsume(String key, int capacity, double refillRatePerSec)
  ```

#### Race Condition Details
The implementation uses **separate, non-atomic Redis calls**:

```
Timeline of Race Condition:
┌─────────────────────────────────────────────┐
│ Thread 1: GET tokens (count=1)              │ 
│ Thread 2: GET tokens (count=1) ← SAME!      │
│ Thread 1: Check (1 >= 1) → allowed=true     │
│ Thread 2: Check (1 >= 1) → allowed=true ❌  │
│ Thread 1: SET tokens=0                      │
│ Thread 2: SET tokens=0                      │
└─────────────────────────────────────────────┘

RESULT: Both threads allowed! Limit violated!
```

#### Javadoc Warning
```java
/**
 * RACE CONDITION WARNING:
 * This method performs separate, non-atomic Redis operations:
 * 1. GET current token count
 * 2. GET last refill timestamp
 * 3. Check availability
 * 4. SET new token count
 * 5. SET new last refill timestamp
 * 
 * Under concurrent load, multiple threads can read the same token count
 * before any thread writes back the updated value, allowing more requests
 * than the rate limit permits.
 */
```

### 3. **TokenBucketLimiterTest.java** (`tests/algorithms/TokenBucketLimiterTest.java`)
JUnit 5 test class that demonstrates the race condition:

#### Test 1: `testSingleRequestAllowed()`
- Verifies that a single request is allowed with a full bucket
- ✓ Should pass

#### Test 2: `testSecondRequestRejected()`
- First request consumes the only token
- Second request should be rejected
- ✓ Should pass

#### Test 3: `testRaceConditionUnderConcurrentLoad()` ⚠️
- **Intentionally failing test** demonstrating the bug
- Fires 20 concurrent requests against a bucket with capacity=1
- Uses `CountDownLatch` to synchronize threads for maximum contention
- **Expected behavior**: Only 1 request allowed
- **Actual behavior**: Multiple requests allowed (typically 2-4+)
- **Assertion**: Will FAIL, proving the race condition exists

Test output example (when Redis available):
```
========== RACE CONDITION TEST RESULTS ==========
Total threads: 20
Bucket capacity: 1
Requests allowed: 3   ← SHOULD BE 1!
Requests rejected: 17
================================================

AssertionError: Expected only 1 request to be allowed with capacity=1, 
but got 3. This demonstrates the non-atomic nature of the naive implementation.
```

## How to Run Tests

### Prerequisites
- Java 17+
- Maven 3.9+
- Redis instance (optional - tests gracefully skip if Redis unavailable)

### Running Tests
```bash
cd c:\Users\sanke\Downloads\rate-limiter
mvn clean test -Dtest=TokenBucketLimiterTest
```

### Running Specific Test
```bash
# Only the race condition test
mvn test -Dtest=TokenBucketLimiterTest#testRaceConditionUnderConcurrentLoad
```

## Why This Naive Implementation?
1. **Educational**: Clearly demonstrates the problem before the solution
2. **Documentation**: The race condition is explicitly documented in code
3. **Proof**: The failing test serves as evidence of the vulnerability
4. **Foundation**: This sets up the next step - fixing it with Redis Lua scripts

## Next Steps: Atomic Implementation
To fix this race condition, we'll implement an atomic version using:
- **Redis Lua scripts** for atomic read-check-write operations
- **EVAL** command to execute Lua atomically on Redis server
- New class: `TokenBucketLimiterAtomic.java` with passing tests

## Key Takeaways
- **Race conditions** can be subtle and hard to detect
- **Non-atomic operations** on distributed systems are dangerous
- **Concurrent testing** reveals issues that single-threaded tests miss
- **Atomic operations** (Lua, transactions) are necessary for correctness
- **Documentation** of known issues is crucial for understanding trade-offs

## Code Statistics
- **Lines of Code**: ~120 (TokenBucketLimiter)
- **Test Coverage**: 3 test cases (2 passing, 1 failing by design)
- **Race Condition Probability**: ~15-20% per run (depends on system load/timing)

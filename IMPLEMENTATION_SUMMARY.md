# Rateforge - Complete Rate Limiting Implementation

## Overview
A Spring Boot application demonstrating three atomic rate limiting algorithms with Redis, protected by an HTTP filter that can be configured per tier.

## Architecture

### Core Components

#### 1. Rate Limiting Algorithms

**TokenBucketLimiterAtomic** (`algorithms/TokenBucketLimiterAtomic.java`)
- Accumulates tokens up to capacity
- Allows bursts up to full capacity
- Tokens refill at a constant rate
- Best for: APIs that tolerate occasional traffic spikes
- Method: `checkAndConsume(key, capacity, refillRatePerSec)`

**SlidingWindowLimiter** (`algorithms/SlidingWindowLimiter.java`)
- Tracks request timestamps in a Redis sorted set
- Removes entries outside the time window
- Counts requests within the window
- Best for: Accurate per-window request counting
- Method: `checkAndRecord(key, windowSizeMs, maxRequests)`

**LeakyBucketLimiter** (`algorithms/LeakyBucketLimiter.java`)
- Queue with fixed capacity that drains at a constant rate
- Smooths bursty traffic into steady output
- Queue fills up to capacity, processes requests at leak rate
- Best for: Protecting downstream systems from traffic bursts
- Method: `checkAndEnqueue(key, capacity, leakRatePerSec)`

#### 2. Lua Scripts for Atomicity

All scripts execute atomically on the Redis server, preventing race conditions:

**token_bucket.lua**
- Reads current tokens and last refill time
- Calculates and adds leaked tokens
- Checks availability and decrements atomically
- Returns: [allowed, remainingTokens]

**sliding_window.lua**
- Removes timestamps outside the window (ZREMRANGEBYSCORE)
- Counts remaining entries (ZCARD)
- Adds current timestamp if allowed (ZADD)
- Returns: [allowed, remainingRequests]

**leaky_bucket.lua**
- Calculates bucket leakage based on elapsed time
- Removes leaked capacity from queue
- Checks queue space and enqueues atomically
- Returns: [allowed, queueSize]

#### 3. Service Layer

**RateLimiterService** (`service/RateLimiterService.java`)
- Facade that delegates to the appropriate limiter
- Supports algorithm switching without client code changes
- Methods:
  - `checkRateLimit(algorithm, key, capacity, rate)`
  - `checkTokenBucket(key, capacity, refillRatePerSec)`
  - `checkSlidingWindow(key, windowSizeMs, maxRequests)`
  - `checkLeakyBucket(key, capacity, leakRatePerSec)`

#### 4. HTTP Filter

**RateLimitFilter** (`filter/RateLimitFilter.java`)
- Intercepts all HTTP requests (except excluded paths)
- Extracts client identifier from:
  1. X-API-Key header (or configured header name)
  2. Falls back to client IP (supports X-Forwarded-For, X-Real-IP)
- Looks up tier configuration
- Sets response headers:
  - `X-RateLimit-Limit`: Maximum requests
  - `X-RateLimit-Remaining`: Requests left
  - `X-RateLimit-Reset`: When limit resets
  - `Retry-After`: Seconds to wait (for rejected requests)
- Returns 429 Too Many Requests with JSON error body if rejected

### Configuration

**application.yml**
```yaml
ratelimit:
  enabled: true
  algorithm: TOKEN_BUCKET  # TOKEN_BUCKET, SLIDING_WINDOW, LEAKY_BUCKET
  headerName: X-API-Key
  excludePaths: /health,/actuator
  tiers:
    default:
      capacity: 100
      rate: 10.0
    premium:
      capacity: 1000
      rate: 100.0
```

## Race Condition Analysis

### Problem
In naive (non-atomic) implementations, concurrent requests can both read the same state before either writes back:

```
Thread 1: GET tokens (count=1) ← sees same value as Thread 2
Thread 2: GET tokens (count=1)
Thread 1: Check (1 >= 1) → ALLOW, SET tokens=0
Thread 2: Check (1 >= 1) → ALLOW, SET tokens=0  ← BOTH allowed!
```

### Solution
Lua scripts execute atomically on Redis server. No interleaving between read-check-write:

```
Thread 1: EVAL (GET → check → SET) → ALLOWED, tokens=0
Thread 2: EVAL (GET → check → SET) → REJECTED (sees tokens=0)  ← Only 1 allowed
```

## Test Coverage

### TokenBucketLimiterTest
- Single request allowed within capacity
- Sequential requests rejected after exhaustion
- Race condition test: 20 concurrent threads, exactly 1 allowed (demonstrates atomicity)

### TokenBucketLimiterAtomicTest
- Same tests as naive version, but PASSING
- Concurrent test: exactly 1 allowed under 20 concurrent requests

### SlidingWindowLimiterTest
- Requests within window allowed
- Requests exceeding limit rejected
- Window slides: old requests expire, capacity frees up
- Concurrent test: exactly 3 allowed (capacity) under 10 concurrent threads

### LeakyBucketLimiterTest
- Requests within capacity enqueued
- Requests exceeding capacity rejected
- Bucket leaks: after sufficient time, capacity recovers
- Steady state: queue remains full at matching request/leak rates

## Running the Application

### Build
```bash
mvn clean package
```

### Run
```bash
# Requires Redis on localhost:6379
java -jar target/rateforge-0.0.1-SNAPSHOT.jar
```

### Health Checks
```bash
# Not rate limited (excluded path)
curl http://localhost:3000/health

# With rate limit (uses default tier, 100 capacity @ 10 tokens/sec)
curl -H "X-API-Key: user123" http://localhost:3000/api/endpoint
```

### Test Specific Algorithm
```bash
# Token bucket (default)
export RATELIMIT_ALGORITHM=TOKEN_BUCKET

# Sliding window (window size 1000ms, max 3 requests)
export RATELIMIT_ALGORITHM=SLIDING_WINDOW

# Leaky bucket (capacity 5, leak rate 2/sec)
export RATELIMIT_ALGORITHM=LEAKY_BUCKET
```

## Key Design Decisions

1. **Lua Scripts**: Ensures atomicity without distributed locks. Redis executes the script as a single operation.

2. **Three Algorithms**: Demonstrates different tradeoffs:
   - Token bucket: simple, allows bursts
   - Sliding window: accurate, per-window counting
   - Leaky bucket: smooths traffic, protects downstream

3. **Testable Timestamps**: All `checkAndRecord`/`checkAndConsume`/`checkAndEnqueue` methods accept optional `now` parameter for testing without `Thread.sleep`.

4. **Facade Service**: Allows algorithm switching and testing via configuration.

5. **HTTP Filter**: Transparent rate limiting at the HTTP layer. Excluded paths avoid rate limiting overhead.

6. **Client Identification**: Multiple strategies (API key, IP) with proxy support.

## Deployment Considerations

1. **Redis**: Single instance for rate limiting state. Consider Redis Cluster for HA.
2. **Clock Skew**: Ensure all application servers have synchronized clocks.
3. **Lua Script Caching**: Redis caches scripts by SHA-1 hash. Scripts are idempotent.
4. **Tier Configuration**: Can be dynamically updated without restarts (if using external config).
5. **Monitoring**: Log all rate limit decisions. Monitor 429 response rate.

## Future Enhancements

1. Distributed rate limiting: Coordinate across multiple services
2. Dynamic tier adjustment: Adjust limits based on load
3. Per-endpoint limits: Different limits for different endpoints
4. User-based limits: VIP users with higher limits
5. Rate limit sharing: Shared quotas across multiple API keys

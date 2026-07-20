# RateForge Interview Demo

Complete demonstration of the RateForge rate limiting system with all three algorithms, Redis integration, and atomic Lua script execution.

## Quick Start

### Prerequisites

Ensure Redis and the backend are running before starting the demo.

### Step 1: Start Redis

**Option A: Docker (Recommended)**
```bash
docker run -d -p 6379:6379 redis:latest
```

**Option B: Docker Compose**
```bash
docker-compose up
```

**Option C: Local Redis**
```bash
redis-server
```

### Step 2: Start Backend

**Option A: Maven**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=demo'
```

**Option B: IDE**
- Open `RateLimiterApplication.java` in your IDE
- Run with Spring profiles: `demo`

**Option C: Pre-built JAR**
```bash
java -jar target/rateforge-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

### Step 3: Run Demo

```bash
cd demo
demo.bat
```

The script will automatically:
1. ✓ Check if backend is running
2. ✓ Verify Redis connectivity
3. ✓ Run all algorithm demonstrations
4. ✓ Display Redis state
5. ✓ Show summary

## What the Demo Shows

### 1. Token Bucket Algorithm
- **Duration**: ~2 seconds
- **What it demonstrates**:
  - Makes 10 successful requests (capacity = 10)
  - Each request decrements X-RateLimit-Remaining
  - Request 11 returns HTTP 429 (rate limited)
  - Waits 2 seconds for token refill
  - Request 12 succeeds (tokens refilled)
- **Key insight**: Allows bursts while refilling over time

### 2. Sliding Window Algorithm
- **Duration**: ~15 seconds
- **What it demonstrates**:
  - Makes 10 successful requests (window capacity = 10)
  - X-RateLimit-Remaining decreases
  - Request 11 returns HTTP 429
  - Waits 3 seconds for window to expire
  - Request 12 succeeds (new window)
- **Key insight**: Accurate per-window request counting

### 3. Leaky Bucket Algorithm
- **Duration**: ~5 seconds
- **What it demonstrates**:
  - Makes 10 successful requests (queue capacity = 10)
  - X-RateLimit-Remaining (capacity - queue size) decreases
  - Request 11 returns HTTP 429 (queue full)
  - Waits 3 seconds for queue to drain
  - Request 12 succeeds (queue has space)
- **Key insight**: Smooths traffic bursts into steady output

### 4. Redis State Inspection
- Shows keys stored in Redis
- Demonstrates atomic execution
- Illustrates Lua script integration

## Demo Profile Configuration

The demo uses a special `demo` Spring profile with reduced capacities for quick demonstration:

**Demo Tier Configuration** (`application-demo.yml`):
```yaml
ratelimit:
  tiers:
    free:
      capacity: 10        # Demo: 10 requests (prod: 100/hour)
      algorithm: TOKEN_BUCKET
    pro:
      capacity: 20        # Demo: 20 requests (prod: 1000/hour)
      algorithm: SLIDING_WINDOW
    enterprise:
      capacity: 10        # Demo: 10 requests (prod: unlimited)
      algorithm: LEAKY_BUCKET
```

This allows all algorithms to visibly reach rate limits within seconds.

## Response Headers Explained

Each response includes rate limiting headers:

```
X-RateLimit-Limit: 10
  └─ Maximum requests in this period

X-RateLimit-Remaining: 7
  └─ Requests still available

X-RateLimit-Reset: 1626153575000
  └─ Unix timestamp when limit resets

Retry-After: 5
  └─ Seconds to wait before retrying (only on 429)
```

## HTTP Status Codes

- **200 OK**: Request allowed, header show remaining capacity
- **429 Too Many Requests**: Rate limit exceeded
  - Includes `Retry-After` header
  - Body contains error details

## Architecture Overview

```
┌──────────────────┐
│   HTTP Request   │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────┐
│  RateLimitFilter         │
│  (Intercepts all routes) │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  Get API Key & Tier from Request         │
│  Determine Algorithm (Token/Window/Leak) │
└────────┬─────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  Algorithm Implementation                │
│  (TokenBucketLimiter, SlidingWindow, etc)│
└────────┬─────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  Lua Script Execution (ATOMIC)           │
│  ✓ No race conditions                    │
│  ✓ Thread-safe                           │
│  ✓ Zero downtime                         │
└────────┬─────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  Redis (Key-Value Store)                 │
│  - Token count                           │
│  - Last refill/leak time                 │
│  - Sliding window sorted set             │
└──────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│  Return Result                           │
│  ✓ allowed/rejected                      │
│  ✓ X-RateLimit-* headers                 │
│  ✓ HTTP 200 or 429                       │
└──────────────────────────────────────────┘
         │
         ▼
┌──────────────────┐
│  HTTP Response   │
└──────────────────┘
```

## Key Technologies

- **Spring Boot 4.1**: Web framework and dependency injection
- **Spring Data Redis**: Official Redis integration
- **Lettuce**: Redis driver (non-blocking)
- **Redis**: In-memory data store
- **Lua Scripts**: Atomic script execution on Redis server

## Three Rate Limiting Algorithms

### Token Bucket
- Tokens accumulate over time
- Each request consumes 1 token
- Refills at configured rate
- Allows bursts up to capacity
- **Use case**: APIs allowing traffic spikes

### Sliding Window
- Tracks request timestamps in a window
- Counts requests within time window
- Rejects when count exceeds limit
- Window slides forward in time
- **Use case**: Precise request counting per time period

### Leaky Bucket
- Queue model with constant drain rate
- Requests queued up to capacity
- Queue drains at fixed rate
- Smooths bursts to steady output
- **Use case**: Protecting downstream services

## Lua Script Advantages

All rate limiting logic executes atomically in Redis via Lua scripts:

**Benefits**:
1. ✓ **No race conditions**: Entire operation is atomic
2. ✓ **No data inconsistency**: Check-then-act cannot interleave
3. ✓ **No round-trips**: Single Redis call instead of multiple
4. ✓ **Server-side logic**: Reduces client-server traffic
5. ✓ **Guaranteed execution**: Script runs exactly once

**Example Race Condition (Without Lua)**:
```
Thread 1: GET counter → 9
Thread 2: GET counter → 9 (same!)
Thread 1: SET counter → 8
Thread 2: SET counter → 8 (lost update! Should be 7)
```

**With Lua (Atomic)**:
```
Thread 1: EVAL script → returns (allowed=1, remaining=9)
Thread 2: EVAL script → returns (allowed=1, remaining=8)
         ↑ Thread 2 sees Thread 1's change immediately
```

## Exploring the Code

### Core Rate Limiter Files
```
src/main/java/com/backend/rate_limiter/
├── algorithms/
│   ├── TokenBucketLimiterAtomic.java
│   ├── SlidingWindowLimiter.java
│   ├── LeakyBucketLimiter.java
│   └── RateLimitAlgorithm.java
├── filter/
│   └── RateLimitFilter.java (intercepts requests)
├── service/
│   └── RateLimiterService.java (orchestrates algorithms)
├── config/
│   ├── TierConfig.java (tier definitions)
│   ├── RateLimitProperties.java (configuration)
│   └── RedisConfig.java (Redis setup)
└── controller/
    └── DemoController.java (demo endpoints)
```

### Lua Scripts
```
src/main/resources/scripts/
├── token_bucket.lua       (Atomic token bucket logic)
├── sliding_window.lua     (Atomic window tracking)
└── leaky_bucket.lua       (Atomic queue drain logic)
```

Each script performs its entire operation atomically, ensuring consistency.

## Configuration

Create `application-demo.yml` in `src/main/resources/` with demo capacities:

```yaml
ratelimit:
  enabled: true
  algorithm: TOKEN_BUCKET
  header-name: X-API-Key
  exclude-paths: /health,/swagger-ui.html,/v3/api-docs
  
  tiers:
    free:
      capacity: 10
      algorithm: TOKEN_BUCKET
      window-size-ms: 10000
    pro:
      capacity: 20
      algorithm: SLIDING_WINDOW
      window-size-ms: 10000
    enterprise:
      capacity: 10
      algorithm: LEAKY_BUCKET
      window-size-ms: 10000

spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

## Troubleshooting

### Backend not starting
```
Error: "No RateLimiterApplication found"
Solution: 
  - Ensure Spring profiles are set: --spring.profiles.active=demo
  - Check that port 8080 is available
  - Verify Java 21+ is installed
```

### Redis connection failed
```
Error: "Cannot connect to Redis"
Solution:
  - Start Redis: docker run -d -p 6379:6379 redis:latest
  - Verify connection: redis-cli ping → should respond PONG
```

### Requests always allowed (no 429)
```
Possible causes:
  - Backend not using demo profile (reduce capacities)
  - Redis not connected properly
  - Rate limit filter not active
Solution: Check application-demo.yml exists and is loaded
```

## Performance Characteristics

- **Latency**: ~5-10ms per request (Lua script + Redis round-trip)
- **Throughput**: ~1000+ RPS (depends on Redis hardware)
- **Memory**: ~100 bytes per unique client/key
- **CPU**: Minimal (Lua script is simple arithmetic)

## Next Steps After Demo

1. **Read the code**: Inspect `TokenBucketLimiterAtomic.java` for implementation details
2. **Review Lua scripts**: Understand atomic execution patterns
3. **Test with custom keys**: Use `X-API-Key: demo-pro` for different limits
4. **Scale to production**: Update capacities in `application.yml`
5. **Deploy**: Use Docker or Kubernetes for scalable deployment

## Questions This Demo Answers

- ✓ How do you implement rate limiting in Spring Boot?
- ✓ How do you eliminate race conditions in distributed systems?
- ✓ Why use Lua scripts in Redis?
- ✓ How to design multiple rate limiting algorithms?
- ✓ How to handle failure gracefully (fail-open policy)?
- ✓ How to integrate Redis with Spring Data?
- ✓ How to enforce rate limits in a web filter?
- ✓ How to return proper HTTP status codes and headers?

## Duration

- Total demo runtime: **~25-30 seconds**
- Perfect for interviews, technical discussions, or architecture reviews
- Demonstrates both theoretical knowledge and practical implementation

---

**Ready for interview? Run: `demo.bat`**

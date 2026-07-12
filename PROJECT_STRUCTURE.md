# Rateforge Project Structure

## Directory Layout

```
rate-limiter/
├── src/
│   ├── main/
│   │   ├── java/com/backend/rate_limiter/
│   │   │   ├── RateLimiterApplication.java          # Spring Boot entry point
│   │   │   │
│   │   │   ├── algorithms/
│   │   │   │   ├── RateLimitAlgorithm.java          # Enum of algorithms
│   │   │   │   ├── TokenBucketLimiter.java          # DEPRECATED naive implementation
│   │   │   │   ├── TokenBucketLimiterAtomic.java    # Atomic token bucket with Lua
│   │   │   │   ├── SlidingWindowLimiter.java        # Sliding window with sorted sets
│   │   │   │   └── LeakyBucketLimiter.java          # Leaky bucket queue
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── RedisConfig.java                 # Redis beans, Lua script registration
│   │   │   │   ├── RedisProperties.java             # Redis connection config
│   │   │   │   ├── RateLimitProperties.java         # Rate limit tier configuration
│   │   │   │   └── WebConfig.java                   # Filter registration
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   └── HealthController.java            # GET /health endpoint
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── HealthResponse.java              # Health endpoint DTO
│   │   │   │   ├── RedisHealthResponse.java         # Redis health DTO
│   │   │   │   └── RateLimitResult.java             # Rate limit decision result
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   └── (reserved for custom exceptions)
│   │   │   │
│   │   │   ├── filter/
│   │   │   │   └── RateLimitFilter.java             # HTTP servlet filter
│   │   │   │
│   │   │   └── service/
│   │   │       └── RateLimiterService.java          # Rate limiting facade
│   │   │
│   │   └── resources/
│   │       ├── application.yml                      # Main config (with Upstash Redis)
│   │       └── scripts/
│   │           ├── token_bucket.lua                 # Token bucket Lua script
│   │           ├── sliding_window.lua               # Sliding window Lua script
│   │           └── leaky_bucket.lua                 # Leaky bucket Lua script
│   │
│   └── test/
│       ├── java/com/backend/rate_limiter/algorithms/
│       │   ├── TokenBucketLimiterTest.java          # Naive implementation test (shows race condition)
│       │   ├── TokenBucketLimiterAtomicTest.java    # Atomic implementation test (fixed)
│       │   ├── SlidingWindowLimiterTest.java        # Sliding window tests
│       │   └── LeakyBucketLimiterTest.java          # Leaky bucket tests
│       │
│       └── resources/
│           └── application-test.yml                 # Test config (localhost Redis)
│
├── pom.xml                                           # Maven configuration
├── mvnw / mvnw.cmd                                  # Maven wrapper
│
├── HELP.md                                          # Spring Initializr help
├── NAIVE_IMPLEMENTATION_SUMMARY.md                  # Naive vs atomic analysis
├── IMPLEMENTATION_SUMMARY.md                        # Complete architecture guide
└── PROJECT_STRUCTURE.md                             # This file

```

## File Descriptions

### Core Algorithm Files

| File | Purpose | Key Method |
|------|---------|-----------|
| `TokenBucketLimiter.java` | Naive implementation (deprecated) | `checkAndConsume(key, capacity, rate)` |
| `TokenBucketLimiterAtomic.java` | Atomic token bucket using Lua | `checkAndConsume(key, capacity, rate)` |
| `SlidingWindowLimiter.java` | Redis sorted set based | `checkAndRecord(key, windowMs, maxReqs)` |
| `LeakyBucketLimiter.java` | Queue-based constant rate | `checkAndEnqueue(key, capacity, leakRate)` |

### Configuration Files

| File | Purpose |
|------|---------|
| `RedisConfig.java` | Redis connection, Lua script beans |
| `RedisProperties.java` | Redis credentials via @ConfigurationProperties |
| `RateLimitProperties.java` | Rate limit tiers and algorithm selection |
| `WebConfig.java` | Servlet filter registration |

### Lua Scripts

| Script | Algorithm | Operations |
|--------|-----------|-----------|
| `token_bucket.lua` | Token Bucket | GET, calculate refill, SET |
| `sliding_window.lua` | Sliding Window | ZREMRANGEBYSCORE, ZCARD, ZADD |
| `leaky_bucket.lua` | Leaky Bucket | GET, calculate leak, SET |

### Test Files

| Test | Coverage | Assertions |
|------|----------|-----------|
| `TokenBucketLimiterTest.java` | Naive race condition | Intentionally fails to show bug |
| `TokenBucketLimiterAtomicTest.java` | Atomic correctness | Passes with Lua atomicity |
| `SlidingWindowLimiterTest.java` | Window expiration | Sliding and concurrent tests |
| `LeakyBucketLimiterTest.java` | Queue drainage | Leakage and steady-state tests |

## Dependencies

### Core Dependencies (from Spring Initializr)
- spring-boot-starter-web: HTTP layer
- spring-boot-starter-data-redis: Redis integration
- spring-boot-starter-actuator: Health endpoints

### Additional Dependencies (Added)
- io.lettuce:lettuce-core: Redis client
- com.fasterxml.jackson.core:jackson-databind: JSON serialization (for error responses)

## Configuration Files

### application.yml
Location: `src/main/resources/application.yml`
- Redis connection: Upstash (cloud Redis)
- Rate limit tiers: default, premium, basic
- Algorithm: TOKEN_BUCKET (configurable)

### application-test.yml
Location: `src/test/resources/application-test.yml`
- Redis connection: localhost:6379
- Used by tests via @ActiveProfiles("test")

### pom.xml
Location: Root directory
- Spring Boot 4.1.0, Java 17
- Maven plugins for build, test, package

## Build & Run

### Build
```bash
mvn clean package
```

### Run (requires Redis)
```bash
java -jar target/rateforge-0.0.1-SNAPSHOT.jar
```

### Tests
```bash
# All tests
mvn test

# Specific test
mvn test -Dtest=TokenBucketLimiterAtomicTest

# Skip tests during build
mvn package -DskipTests
```

## Key Classes at a Glance

### Rate Limiting Algorithms
- **RateLimitAlgorithm.java**: Enum with TOKEN_BUCKET, SLIDING_WINDOW, LEAKY_BUCKET
- **RateLimiterService.java**: Facade delegating to appropriate limiter

### HTTP Layer
- **HealthController.java**: GET /health (not rate limited)
- **RateLimitFilter.java**: Servlet filter applying rate limits
- **RateLimitProperties.java**: Configuration for limits and tiers

### Redis Integration
- **RedisConfig.java**: Registers StringRedisTemplate and script beans
- **RedisProperties.java**: Credentials and connection settings

### Data Transfer
- **RateLimitResult.java**: Record with (allowed, remainingTokens)
- **HealthResponse.java**: Simple status response

## Extension Points

1. **Add New Algorithm**: 
   - Create `NewAlgorithmLimiter.java`
   - Add Lua script in `scripts/`
   - Register bean in `RedisConfig.java`
   - Add to `RateLimitAlgorithm` enum
   - Add case in `RateLimiterService.checkRateLimit()`

2. **Add New Tier**:
   - Update `application.yml` ratelimit.tiers

3. **Custom Client Identification**:
   - Modify `extractClientIdentifier()` in `RateLimitFilter.java`

4. **Per-Endpoint Limits**:
   - Enhance `RateLimitFilter.java` to check URL patterns
   - Map URLs to tier configurations

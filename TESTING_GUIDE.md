# Rateforge Testing Guide

Complete guide for testing the rate limiting system locally and with Docker.

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose (for containerized testing)
- Redis (for local testing without Docker)
- curl or Postman (for API testing)

---

## Option 1: Local Testing (Without Docker)

### 1.1 Start Redis Locally

#### Using Docker (Redis only)
```bash
docker run -d -p 6379:6379 --name redis-test redis:7-alpine
```

#### Or install Redis locally
- Windows: Use Windows Subsystem for Linux (WSL) or Redis for Windows
- macOS: `brew install redis`
- Linux: `apt-get install redis-server`

### 1.2 Run the Application

```bash
# Build the project
mvn clean package -DskipTests

# Run the application
java -jar target/rateforge-0.0.1-SNAPSHOT.jar \
  --spring.data.redis.host=localhost \
  --spring.data.redis.port=6379
```

The app will start on `http://localhost:3000`

### 1.3 Test Health Endpoint

```bash
# Health check (not rate limited)
curl http://localhost:3000/health
```

Expected response:
```json
{"status":"ok"}
```

---

## Option 2: Docker Compose Stack

### 2.1 Start the Full Stack

```bash
cd c:\Users\sanke\Downloads\rate-limiter

# Build and start services
docker compose up --build

# Or in background
docker compose up -d --build
```

Wait for both services to report healthy (watch the logs or check status).

### 2.2 Verify Services are Running

```bash
# Check service status
docker compose ps

# View logs
docker compose logs app      # Application logs
docker compose logs redis    # Redis logs
docker compose logs -f       # Follow logs (real-time)
```

### 2.3 Stop the Stack

```bash
docker compose down

# Remove volumes too
docker compose down -v
```

---

## Testing the Rate Limiting APIs

### Demo Endpoints

The application provides three demo endpoints, each using a different rate limiting algorithm:

| Endpoint | Algorithm | Path |
|----------|-----------|------|
| Token Bucket | Accumulates tokens, allows bursts | `/demo/token-bucket` |
| Sliding Window | Counts requests in a time window | `/demo/sliding-window` |
| Leaky Bucket | Queue that drains at constant rate | `/demo/leaky-bucket` |

### Tier Limits

```
FREE:       100 requests/hour (api key: demo-free)
PRO:        1000 requests/hour (api key: demo-pro)
ENTERPRISE: 1,000,000 requests/hour (api key: demo-enterprise)
```

### 3.1 Get Endpoint Info

```bash
# Get info about all endpoints and tiers
curl http://localhost:3000/demo/info | jq
```

### 3.2 Test Token Bucket Algorithm

#### Allowed Request (within limit)
```bash
# Test with PRO tier (1000 req/hour = 0.278 req/sec)
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket | jq
```

Expected response headers:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: <timestamp>
```

#### Rapid Requests (testing burst capacity)
```bash
# Send 5 rapid requests with FREE tier (100 req/hour)
for i in {1..5}; do
  echo "Request $i:"
  curl -H "X-API-Key: demo-free" http://localhost:3000/demo/token-bucket -i 2>/dev/null | grep -E "HTTP|X-RateLimit"
  echo ""
done
```

#### Rate Limited Response (429)
```bash
# Send many requests quickly to hit the limit
for i in {1..150}; do
  curl -H "X-API-Key: demo-free" http://localhost:3000/demo/token-bucket -s > /dev/null
  echo "Request $i sent"
done

# Next request should get 429
curl -H "X-API-Key: demo-free" http://localhost:3000/demo/token-bucket -v
```

Expected response:
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded for tier: free",
  "tier": "free",
  "remaining": 0,
  "limit": 100
}
```

### 3.3 Test Sliding Window Algorithm

```bash
# Send 5 requests with FREE tier
for i in {1..5}; do
  echo "Request $i:"
  curl -H "X-API-Key: demo-free" http://localhost:3000/demo/sliding-window -s | jq '.endpoint, .algorithm'
done

# This should work - each endpoint has its own rate limit
# So even though /demo/token-bucket used 5, /demo/sliding-window resets
```

### 3.4 Test Leaky Bucket Algorithm

```bash
# Test leaky bucket - queue fills and drains at constant rate
for i in {1..10}; do
  echo "Request $i:"
  result=$(curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/leaky-bucket -s -w "\nStatus: %{http_code}")
  echo "$result"
  echo ""
done
```

### 3.5 Test Different Tiers

```bash
# FREE tier (100 req/hour = very restrictive in testing)
curl -H "X-API-Key: demo-free" http://localhost:3000/demo/token-bucket -s | jq

# PRO tier (1000 req/hour)
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket -s | jq

# ENTERPRISE tier (1,000,000 req/hour = practically unlimited)
curl -H "X-API-Key: demo-enterprise" http://localhost:3000/demo/token-bucket -s | jq
```

---

## Automated Testing

### 4.1 Unit Tests

Run the algorithm tests:

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=TokenBucketLimiterAtomicTest
mvn test -Dtest=SlidingWindowLimiterTest
mvn test -Dtest=LeakyBucketLimiterTest

# Specific test method
mvn test -Dtest=TokenBucketLimiterAtomicTest#testAtomicConcurrentLoad
```

### 4.2 Load Testing with Apache Bench

```bash
# Install ab (Apache Bench)
# macOS: brew install httpd
# Linux: apt-get install apache2-utils
# Windows: Download from Apache website

# Test token bucket with 100 concurrent requests
ab -n 100 -c 10 -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket

# Output shows success vs failures (429s)
```

### 4.3 Load Testing with Apache JMeter

```bash
# Download from https://jmeter.apache.org/
# GUI: jmeter
# Create test plan with:
# - HTTP Request Sampler pointing to /demo/token-bucket
# - Thread Group with 50 threads
# - Response Assertion checking for 200 OK
# - View Results Tree to see responses
```

### 4.4 Load Testing with Artillery

```bash
# Install artillery
npm install -g artillery

# Create load-test.yml:
config:
  target: "http://localhost:3000"
  phases:
    - duration: 10
      arrivalRate: 50
      name: "Warm up"
    - duration: 30
      arrivalRate: 100
      name: "High load"

scenarios:
  - name: "Rate Limit Test"
    requests:
      - url: "/demo/token-bucket"
        headers:
          X-API-Key: "demo-pro"

# Run test
artillery run load-test.yml
```

---

## Integration Tests

### 5.1 Test with Different Algorithms

Create a script to test algorithm switching:

```bash
#!/bin/bash

echo "Testing Token Bucket..."
for i in {1..3}; do
  curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket -s | jq '.algorithm'
done

echo "Testing Sliding Window..."
for i in {1..3}; do
  curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/sliding-window -s | jq '.algorithm'
done

echo "Testing Leaky Bucket..."
for i in {1..3}; do
  curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/leaky-bucket -s | jq '.algorithm'
done
```

### 5.2 Test Race Condition Fix

```bash
# Send concurrent requests to verify atomicity
# Use the atomic test from the unit tests
mvn test -Dtest=TokenBucketLimiterAtomicTest#testAtomicConcurrentLoad

# Should show "exactly X allowed" instead of random numbers
```

### 5.3 Test Tier Isolation

Each tier should have independent rate limits:

```bash
# Use different API keys, each should have separate quota
curl -H "X-API-Key: demo-free" http://localhost:3000/demo/token-bucket -s | jq '.remaining'
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket -s | jq '.remaining'

# Free tier will have lower remaining count due to lower capacity
```

---

## Docker Testing

### 6.1 Test Container Health

```bash
# Check if containers are healthy
docker compose ps

# Watch health status in real-time
watch -n 1 'docker compose ps'

# View detailed health check info
docker inspect --format='{{json .State.Health}}' rateforge-app | jq
docker inspect --format='{{json .State.Health}}' rateforge-redis | jq
```

### 6.2 Test Container Communication

```bash
# Exec into app container and test Redis connection
docker compose exec app sh -c 'wget -O- http://localhost:3000/health'

# Test from outside
curl http://localhost:3000/health
```

### 6.3 Test Persistence

```bash
# Make some requests to generate state
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket

# Stop and restart
docker compose restart app

# Verify Redis state persists (remaining count continues counting down)
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket
```

### 6.4 Container Logs Analysis

```bash
# View all logs
docker compose logs

# Follow app logs
docker compose logs -f app

# View last 50 lines
docker compose logs --tail=50 app

# View logs since last 5 minutes
docker compose logs --since 5m app
```

---

## Performance Testing

### 7.1 Measure Response Time

```bash
# Using curl's time metrics
curl -H "X-API-Key: demo-pro" \
  -w "Time: %{time_total}s\n" \
  http://localhost:3000/demo/token-bucket

# More detailed timing
curl -H "X-API-Key: demo-pro" \
  -w "DNS: %{time_namelookup}, Connect: %{time_connect}, Total: %{time_total}\n" \
  http://localhost:3000/demo/token-bucket
```

### 7.2 Monitor Redis Performance

```bash
# Connect to Redis CLI
docker compose exec redis redis-cli

# Inside redis-cli:
> INFO stats      # Redis stats
> MONITOR         # Watch all commands
> KEYS *          # List all keys
> DBSIZE          # Database size
```

### 7.3 Monitor Container Resources

```bash
# Watch CPU and memory usage
docker stats rateforge-app rateforge-redis --no-stream

# Continuous monitoring
docker stats rateforge-app rateforge-redis
```

---

## Troubleshooting

### 8.1 Connection Issues

```bash
# Test Redis connectivity from app container
docker compose exec app ping redis

# Test from host
redis-cli -h localhost ping

# Check Redis logs
docker compose logs redis
```

### 8.2 Rate Limit Not Working

```bash
# Check if rate limiting is enabled
curl http://localhost:3000/demo/info | jq '.configuration'

# Verify tier mapping
curl http://localhost:3000/demo/info | jq '.tiers'

# Check app logs for errors
docker compose logs app | grep -i error
```

### 8.3 Docker Build Issues

```bash
# Clean build
docker compose down -v
docker system prune -a

# Rebuild with verbose output
docker compose build --no-cache --verbose app

# Check Docker daemon
docker info
```

---

## Expected Behavior Summary

### Successful Rate Limiting:
✓ First 100 requests with demo-free → HTTP 200
✓ Request 101 with demo-free → HTTP 429
✓ X-RateLimit-* headers present
✓ Error body with tier and remaining info

### Algorithm-Specific:
**Token Bucket:**
- Allows burst requests up to capacity
- Refills tokens over time at configured rate

**Sliding Window:**
- Removes old requests outside the window
- Counts remaining requests in current window

**Leaky Bucket:**
- Queue fills to capacity
- Processes requests at constant drain rate

### Atomicity:
✓ Concurrent requests handled safely
✓ No race conditions (verified by tests)
✓ Exact capacity limits respected

---

## Quick Start Commands

```bash
# Option 1: Local with local Redis
redis-server &
mvn package -DskipTests
java -jar target/rateforge-0.0.1-SNAPSHOT.jar

# Option 2: Local with Docker Redis
docker run -d -p 6379:6379 redis:7-alpine
mvn package -DskipTests
java -jar target/rateforge-0.0.1-SNAPSHOT.jar

# Option 3: Full Docker Compose
docker compose up --build

# Test any option
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket
```

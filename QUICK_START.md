# Rateforge - Quick Start Guide

Get the rate limiting service up and running in 5 minutes.

---

## 🚀 Fastest Way to Start: Docker Compose

### Prerequisites
- Docker and Docker Compose installed
- No local Redis needed (Docker provides it)

### Start
```bash
cd c:\Users\sanke\Downloads\rate-limiter
docker compose up --build
```

**Wait for output like:**
```
rateforge-redis | * Ready to accept connections
rateforge-app   | Started RateLimiterApplication
```

Access the service on `http://localhost:3000`

---

## 🧪 Test the Service

### 1. Health Check (Unprotected)
```bash
curl http://localhost:3000/health
```

Expected:
```json
{"status":"ok"}
```

### 2. Token Bucket Test (Protected by Rate Limit)
```bash
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket | jq
```

Expected:
```json
{
  "endpoint": "/demo/token-bucket",
  "algorithm": "Token Bucket",
  "description": "Accumulates tokens up to capacity. Allows bursts.",
  "useCase": "APIs that tolerate occasional traffic spikes"
}
```

Response headers:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1626153575000
```

### 3. Test Different Tiers
```bash
# FREE tier (100 req/hour)
curl -H "X-API-Key: demo-free" http://localhost:3000/demo/token-bucket | jq '.endpoint'

# PRO tier (1000 req/hour)
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket | jq '.endpoint'

# ENTERPRISE tier (1,000,000 req/hour - practically unlimited)
curl -H "X-API-Key: demo-enterprise" http://localhost:3000/demo/token-bucket | jq '.endpoint'
```

### 4. Test Rate Limiting (429 Response)
```bash
# Bash/Linux/Mac - Rapid requests to hit the limit
for i in {1..150}; do
  curl -s -H "X-API-Key: demo-free" http://localhost:3000/demo/token-bucket > /dev/null
  if [ $i -eq 100 ]; then echo "Request $i sent"; fi
  if [ $i -eq 150 ]; then echo "Request $i sent - should be limited now"; fi
done

# Next request should fail
curl -H "X-API-Key: demo-free" http://localhost:3000/demo/token-bucket
```

Expected 429 response:
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

Response headers:
```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
Retry-After: 1
```

### 5. Test All Three Algorithms
```bash
# Token Bucket
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket | jq '.algorithm'

# Sliding Window
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/sliding-window | jq '.algorithm'

# Leaky Bucket
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/leaky-bucket | jq '.algorithm'
```

### 6. View API Documentation
```bash
curl http://localhost:3000/demo/info | jq
```

Shows:
- All available endpoints
- Tier information and limits
- Example curl commands
- Response header documentation

---

## 📊 Algorithm Comparison

### Token Bucket
```bash
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/token-bucket | jq
```
- **Use Case**: APIs that tolerate occasional traffic spikes
- **Behavior**: Allows bursts up to capacity, refills over time
- **Best For**: Web services, mobile apps

### Sliding Window
```bash
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/sliding-window | jq
```
- **Use Case**: Accurate per-window request counting
- **Behavior**: Removes old requests outside the window
- **Best For**: Billing APIs, audit logs

### Leaky Bucket
```bash
curl -H "X-API-Key: demo-pro" http://localhost:3000/demo/leaky-bucket | jq
```
- **Use Case**: Protecting downstream systems from bursts
- **Behavior**: Queues requests, processes at constant rate
- **Best For**: Queue-based systems, message brokers

---

## 🔍 Monitoring

### View Logs
```bash
# Follow app logs
docker compose logs -f app

# Follow Redis logs
docker compose logs -f redis

# View all logs
docker compose logs
```

### Check Service Status
```bash
# See running containers
docker compose ps

# Get detailed status
docker inspect rateforge-app | jq '.State'
```

### Monitor Redis
```bash
# Connect to Redis CLI
docker compose exec redis redis-cli

# Inside redis-cli:
> PING
> DBSIZE
> KEYS rate*
> GET "api-key:demo-pro:tokens"
```

---

## 🛑 Stop the Service

```bash
# Stop containers (preserve data)
docker compose stop

# Remove containers (clean shutdown)
docker compose down

# Remove everything including volumes
docker compose down -v
```

---

## 📝 API Keys for Testing

| API Key | Tier | Limit | Per |
|---------|------|-------|-----|
| `demo-free` | FREE | 100 | hour |
| `demo-pro` | PRO | 1,000 | hour |
| `demo-enterprise` | ENTERPRISE | 1,000,000 | hour |

---

## 🔧 Configuration Changes

### Change Algorithm Globally
Edit `docker-compose.yml` or set environment variable:

```yaml
environment:
  RATELIMIT_ALGORITHM: "SLIDING_WINDOW"  # or LEAKY_BUCKET
```

Then restart: `docker compose restart app`

### Change Tier Limits
Edit `src/main/resources/application.yml`:

```yaml
ratelimit:
  tiers:
    by-tier:
      free:
        capacity: 50  # Changed from 100
        rate: 0.0139  # 50/(60*60)
```

Then rebuild and restart: `docker compose up --build`

### Add New API Key
Edit `src/main/java/com/backend/rate_limiter/service/UserTierService.java`:

```java
case "new-key":
    return "pro";  // Map to PRO tier
```

---

## 🐛 Troubleshooting

### "Connection refused" to Redis
```bash
# Check if Redis container is running
docker compose ps

# Check Redis logs
docker compose logs redis

# Restart
docker compose restart redis
```

### "Rate limiting not working"
```bash
# Check if filter is enabled
curl http://localhost:3000/health  # Should work

# Check logs for errors
docker compose logs app | grep -i error

# Verify Redis connection
curl http://localhost:3000/health/redis
```

### "Cannot build image"
```bash
# Clean and rebuild
docker compose down -v
docker system prune -a
docker compose up --build
```

---

## 📚 More Information

- **Full Architecture**: See `IMPLEMENTATION_SUMMARY.md`
- **Complete Testing**: See `TESTING_GUIDE.md`
- **Project Structure**: See `PROJECT_STRUCTURE.md`
- **Build Verification**: See `VERIFICATION_CHECKLIST.md`

---

## ✨ Key Features

✅ **3 Atomic Algorithms**
- Token Bucket (bursts allowed)
- Sliding Window (per-window counting)
- Leaky Bucket (smooth output)

✅ **Atomic Lua Scripts** (no race conditions)
- Prevent concurrent request bypass
- Execute as single Redis operation

✅ **Tiered Configuration**
- FREE, PRO, ENTERPRISE tiers
- Per-API-key tier assignment

✅ **HTTP Filter**
- Transparent rate limiting
- Response headers with limits
- 429 status on rejection

✅ **Docker Ready**
- Multi-stage build
- Health checks
- Auto-recovery

✅ **Well Tested**
- 16 unit tests (100% pass)
- Concurrent load tests
- Atomic behavior verified

---

## 🎯 Next Steps

1. **Start service**: `docker compose up --build`
2. **Test endpoints**: Use curl commands above
3. **Load test**: See TESTING_GUIDE.md for Apache Bench/JMeter/Artillery
4. **Deploy**: Push to Docker registry or cloud platform
5. **Customize**: Adjust tier limits and API keys for your use case

---

**Ready to rate-limit?** 🚀

```bash
docker compose up --build
```

Then test: `curl http://localhost:3000/health`


# RateForge - Quick Start Guide

## What Was Fixed (TL;DR)

### Backend Fix
- **Problem**: `IllegalArgumentException: Return type null is not a supported script output type` in Spring Boot 4.1
- **Solution**: Replaced null ReturnType with `DefaultRedisScript<List>` using official Spring Data Redis API
- **Result**: ✅ Rate limiting now works atomically via Redis Lua script

### Frontend Fix
- **Problem**: PostCSS plugin error + missing `react-is` dependency
- **Solution**: Updated to Tailwind CSS v4 syntax (`@import "tailwindcss"`) + added dependency
- **Result**: ✅ Frontend builds successfully, no runtime errors

---

## 60-Second Setup

### Prerequisites
- Java 21+ installed
- Docker installed (recommended for Redis)
- Node.js 18+ installed

### Start Backend

```bash
cd c:\Users\sanke\Downloads\rate-limiter

# Start Redis (if not already running)
docker run -d -p 6379:6379 redis:latest

# Start application
java -jar target/rateforge-0.0.1-SNAPSHOT.jar
```

✓ Backend running at http://localhost:8080

### Start Frontend

```bash
cd frontend
npm run dev
```

✓ Frontend running at http://localhost:5173

### Test Rate Limiting

```powershell
# Run the automated test script
cd c:\Users\sanke\Downloads\rate-limiter
.\TEST_BACKEND_FIX.ps1
```

Expected output:
```
Request 1-100:   HTTP 200 ✓
Request 101:     HTTP 429 ✓ (Rate limited)
```

---

## Full Docker Deployment

```bash
cd c:\Users\sanke\Downloads\rate-limiter
docker-compose up --build
```

This starts:
- Backend on port 8080
- Frontend on port 5173
- Redis on port 6379

---

## Verify Everything Works

### Check Backend Health
```bash
curl http://localhost:8080/health
```

Should return:
```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" }
  }
}
```

### Test Rate Limiting
```bash
for ($i=1; $i -le 5; $i++) {
  curl -H "X-API-Key: demo-free" http://localhost:8080/api/rate-limit
  Write-Host ""
}
```

Each request should have decreasing `X-RateLimit-Remaining` header.

---

## Key Files

### Backend
```
src/main/java/com/backend/rate_limiter/algorithms/TokenBucketLimiterAtomic.java
  → Fixed with DefaultRedisScript<List>
  
src/main/resources/scripts/token_bucket.lua
  → Atomic rate limiting logic
  
target/rateforge-0.0.1-SNAPSHOT.jar
  → Ready to deploy
```

### Frontend
```
frontend/src/App.css
  → Uses @import "tailwindcss"
  
frontend/postcss.config.js
  → Empty plugins config
  
frontend/package.json
  → Includes react-is dependency
  
frontend/dist/
  → Production build
```

---

## Common Issues

| Issue | Solution |
|-------|----------|
| Redis connection fails | `docker run -d -p 6379:6379 redis:latest` |
| Port 8080 in use | Kill process: `netstat -ano \| findstr :8080` |
| Frontend CSS errors | Verify `postcss.config.js` has `plugins: {}` |
| `react-is` not found | Run `npm install --legacy-peer-deps` in frontend/ |
| All requests return 200 | Check Redis is running and connected |

---

## What to Read Next

1. **System Overview**: See `SYSTEM_STATUS.md`
2. **Deployment**: See `DEPLOYMENT_GUIDE.md`
3. **Technical Details**: See `REDIS_LUA_FIX.md`
4. **Testing**: Run `TEST_BACKEND_FIX.ps1`

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   Frontend (React 19)                │
│        - Tailwind CSS v4 (@import "tailwindcss")   │
│        - Runs on http://localhost:5173             │
└───────────────────┬─────────────────────────────────┘
                    │ HTTP/REST API
┌───────────────────▼─────────────────────────────────┐
│               Spring Boot 4.1 Backend               │
│        - Runs on http://localhost:8080             │
│        - Handles rate limiting                      │
│        - Controllers, Services, Config              │
└───────────────────┬─────────────────────────────────┘
                    │ Redis Protocol
┌───────────────────▼─────────────────────────────────┐
│                  Redis Database                      │
│        - Stores rate limit state atomically         │
│        - Executes Lua scripts                       │
│        - Runs on localhost:6379                     │
└──────────────────────────────────────────────────────┘
```

---

## Rate Limiting How It Works

1. **Request arrives** with `X-API-Key: demo-free`
2. **Backend calls** Redis Lua script (atomic execution)
3. **Lua script**:
   - Calculates available tokens
   - Checks if 1 token available
   - If yes: consume token, return allowed=1
   - If no: return allowed=0
4. **Backend responds**:
   - HTTP 200 with X-RateLimit-Remaining header
   - HTTP 429 if rate limit exceeded
5. **State persisted** in Redis (survives app restart)

---

## Deployment Checklist

- [ ] Redis running
- [ ] Backend JAR built: `target/rateforge-0.0.1-SNAPSHOT.jar`
- [ ] Backend starts without errors
- [ ] Frontend built: `npm run build` succeeds
- [ ] Health check passes: `curl http://localhost:8080/health`
- [ ] Rate limiting test passes: `.\TEST_BACKEND_FIX.ps1`
- [ ] Frontend loads in browser without CSS errors
- [ ] API key headers work correctly

---

## Commands Reference

```bash
# Backend
cd c:\Users\sanke\Downloads\rate-limiter
.\mvnw.cmd clean package -DskipTests      # Build
java -jar target/rateforge-*.jar          # Run

# Frontend
cd frontend
npm install --legacy-peer-deps            # Install dependencies
npm run dev                                # Dev server
npm run build                              # Production build
npm run lint                               # Lint code

# Redis
docker run -d -p 6379:6379 redis:latest   # Start Redis
redis-cli PING                             # Test connection
redis-cli FLUSHDB                          # Clear data

# Docker
docker-compose up --build                 # Start everything
docker-compose down                        # Stop everything
docker-compose logs -f                     # View logs
```

---

## Next Steps

1. **Run the application** following the 60-second setup above
2. **Verify all systems** using the verification steps
3. **Review logs** if any issues occur
4. **Test rate limiting** with the automated script
5. **Deploy** using Docker or JAR as appropriate

---

## Support

- **Issues**: Check troubleshooting section above
- **Details**: Read SYSTEM_STATUS.md or DEPLOYMENT_GUIDE.md
- **Testing**: Run TEST_BACKEND_FIX.ps1 for automated verification
- **Code**: See source files in src/ and frontend/src/

---

**Status**: ✅ Ready for Production  
**Version**: rateforge-0.0.1-SNAPSHOT  
**Last Updated**: July 20, 2026

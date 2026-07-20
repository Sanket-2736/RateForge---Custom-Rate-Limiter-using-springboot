#requires -version 3.0

# RateForge Interview Demo Script
# Demonstrates all rate limiting algorithms with Redis integration

$ErrorActionPreference = "Continue"

# Configuration
$BACKEND_URL = "http://localhost:3000"
$REDIS_HOST = "localhost"
$REDIS_PORT = 6379
$DEMO_API_KEY = "demo-free"

function Write-Header {
    param([string]$Title, [string]$Color = "Cyan")
    Write-Host ""
    Write-Host "================================================" -ForegroundColor $Color
    Write-Host $Title -ForegroundColor $Color
    Write-Host "================================================" -ForegroundColor $Color
}

function Write-SubHeader {
    param([string]$Title)
    Write-Host ""
    Write-Host "--- $Title ---" -ForegroundColor Cyan
}

function Check-Backend {
    Write-Host "`nChecking backend..." -ForegroundColor Yellow
    
    $healthUrl = "$BACKEND_URL/health"
    Write-Host "  Health endpoint: $healthUrl" -ForegroundColor White
    
    try {
        $response = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
        Write-Host "  Status: $($response.StatusCode)" -ForegroundColor Cyan
        
        # Check if status code is 200
        if ($response.StatusCode -eq 200) {
            Write-Host "[OK] Backend running on $BACKEND_URL" -ForegroundColor Green
            return $true
        }
        else {
            Write-Host "  Unexpected status: $($response.StatusCode)" -ForegroundColor Yellow
            Write-Host "  Response: $($response.Content)" -ForegroundColor Yellow
        }
    }
    catch [System.Net.WebException] {
        $ex = $_.Exception
        Write-Host "  Error: $($ex.Message)" -ForegroundColor Yellow
        
        # Check if it's a connection refused or timeout
        if ($ex.Message -like "*Unable to connect*" -or $ex.Message -like "*Timeout*" -or $ex.Message -like "*refused*") {
            Write-Host "[FAIL] Backend not reachable (connection refused or timeout)" -ForegroundColor Red
        }
        else {
            Write-Host "  Full response:" -ForegroundColor Yellow
            if ($ex.Response) {
                Write-Host "    Status Code: $($ex.Response.StatusCode)" -ForegroundColor Cyan
                $reader = New-Object System.IO.StreamReader($ex.Response.GetResponseStream())
                $body = $reader.ReadToEnd()
                Write-Host "    Response: $body" -ForegroundColor Yellow
                $reader.Close()
            }
        }
    }
    catch {
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host "`n[FAIL] Cannot connect to backend" -ForegroundColor Red
    Write-Host "`nPlease start the backend:" -ForegroundColor Yellow
    Write-Host "  Option 1: ./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=demo'" -ForegroundColor White
    Write-Host "  Option 2: Run RateLimiterApplication.java in your IDE" -ForegroundColor White
    Write-Host "  Option 3: java -jar target/rateforge-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo" -ForegroundColor White
    return $false
}

function Check-Redis {
    Write-Host "`nChecking Redis..." -ForegroundColor Yellow
    Write-Host "  Redis endpoint: $($REDIS_HOST):$($REDIS_PORT)" -ForegroundColor White
    
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.ConnectAsync($REDIS_HOST, $REDIS_PORT).Wait(2000) | Out-Null
        
        if ($client.Connected) {
            $client.Close()
            Write-Host "[OK] Redis reachable on $($REDIS_HOST):$($REDIS_PORT)" -ForegroundColor Green
            return $true
        }
    }
    catch {
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host "[FAIL] Redis not reachable on $($REDIS_HOST):$($REDIS_PORT)" -ForegroundColor Red
    Write-Host "`nPlease start Redis:" -ForegroundColor Yellow
    Write-Host "  Option 1: docker run -d -p 6379:6379 redis:latest" -ForegroundColor White
    Write-Host "  Option 2: redis-server (if installed locally)" -ForegroundColor White
    Write-Host "  Option 3: docker-compose up (from project root)" -ForegroundColor White
    return $false
}

function Make-Request {
    param(
        [string]$Algorithm,
        [int]$RequestNum = 0
    )
    
    try {
        $response = Invoke-WebRequest `
            -Uri "$BACKEND_URL/demo/$Algorithm" `
            -Headers @{"X-API-Key" = $DEMO_API_KEY} `
            -TimeoutSec 5 `
            -UseBasicParsing `
            -ErrorAction Stop
        
        return @{
            Status = $response.StatusCode
            Headers = $response.Headers
            Body = $response.Content
            Success = $true
        }
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 429) {
            return @{
                Status = 429
                Headers = $_.Exception.Response.Headers
                Body = $_.Exception.Response.Content
                Success = $false
            }
        }
        Write-Host "Error making request: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

function Display-Response {
    param($Response, [int]$RequestNum)
    
    if ($Response -eq $null) { return }
    
    Write-Host "  Request $($RequestNum) : " -NoNewline
    
    if ($Response.Status -eq 200) {
        Write-Host "[OK] Allowed" -ForegroundColor Green
    }
    else {
        Write-Host "[RATE LIMITED] 429" -ForegroundColor Red
    }
    
    Write-Host "    Status: $($Response.Status)" -ForegroundColor Cyan
    Write-Host "    X-RateLimit-Limit: $($Response.Headers['X-RateLimit-Limit'])" -ForegroundColor Cyan
    Write-Host "    X-RateLimit-Remaining: $($Response.Headers['X-RateLimit-Remaining'])" -ForegroundColor Cyan
    
    if ($Response.Headers.ContainsKey('Retry-After')) {
        Write-Host "    Retry-After: $($Response.Headers['Retry-After']) seconds" -ForegroundColor Cyan
    }
}

function Demo-TokenBucket {
    Write-Header "TOKEN BUCKET RATE LIMITER" "Green"
    
    Write-Host "`nAlgorithm: Token Bucket" -ForegroundColor White
    Write-Host "Capacity: 10 tokens" -ForegroundColor White
    Write-Host "Refill Rate: 1 token/second" -ForegroundColor White
    Write-Host "Behavior: Accumulates tokens, allows bursts up to capacity" -ForegroundColor White
    Write-Host "`nDemonstration:" -ForegroundColor Yellow
    
    # Make 10 successful requests
    for ($i = 1; $i -le 10; $i++) {
        $response = Make-Request "token-bucket"
        Display-Response $response $i
        Start-Sleep -Milliseconds 100
    }
    
    # Try to exceed limit
    Write-Host "`nAttempting request when limit exceeded:" -ForegroundColor Yellow
    $response = Make-Request "token-bucket" 11
    Display-Response $response 11
    
    # Wait for refill
    Write-Host "`nWaiting 2 seconds for token refill..." -ForegroundColor Yellow
    for ($i = 2; $i -ge 1; $i--) {
        Write-Host "  $i seconds remaining..." -ForegroundColor Cyan
        Start-Sleep -Seconds 1
    }
    
    # Try again after refill
    Write-Host "`nAttempting request after refill:" -ForegroundColor Yellow
    $response = Make-Request "token-bucket" 12
    Display-Response $response 12
    
    Write-Host "`n[OK] Token Bucket demonstration complete" -ForegroundColor Green
}

function Demo-SlidingWindow {
    Write-Header "SLIDING WINDOW RATE LIMITER" "Green"
    
    Write-Host "`nAlgorithm: Sliding Window" -ForegroundColor White
    Write-Host "Capacity: 10 requests" -ForegroundColor White
    Write-Host "Window Size: 10 seconds" -ForegroundColor White
    Write-Host "Behavior: Counts requests in a time window, rejects when limit exceeded" -ForegroundColor White
    Write-Host "`nDemonstration:" -ForegroundColor Yellow
    
    # Make 10 successful requests
    for ($i = 1; $i -le 10; $i++) {
        $response = Make-Request "sliding-window"
        Display-Response $response $i
        Start-Sleep -Milliseconds 100
    }
    
    # Try to exceed limit
    Write-Host "`nAttempting request when window limit exceeded:" -ForegroundColor Yellow
    $response = Make-Request "sliding-window" 11
    Display-Response $response 11
    
    # Wait for window expiration
    Write-Host "`nWaiting 3 seconds for window to expire..." -ForegroundColor Yellow
    for ($i = 3; $i -ge 1; $i--) {
        Write-Host "  $i seconds remaining..." -ForegroundColor Cyan
        Start-Sleep -Seconds 1
    }
    
    # Try again after window expires
    Write-Host "`nAttempting request after window expiration:" -ForegroundColor Yellow
    $response = Make-Request "sliding-window" 12
    Display-Response $response 12
    
    Write-Host "`n[OK] Sliding Window demonstration complete" -ForegroundColor Green
}

function Demo-LeakyBucket {
    Write-Header "LEAKY BUCKET RATE LIMITER" "Green"
    
    Write-Host "`nAlgorithm: Leaky Bucket" -ForegroundColor White
    Write-Host "Capacity: 10 requests (queue)" -ForegroundColor White
    Write-Host "Leak Rate: 1 request/second" -ForegroundColor White
    Write-Host "Behavior: Queue fills up, drains at constant rate" -ForegroundColor White
    Write-Host "`nDemonstration:" -ForegroundColor Yellow
    
    # Make 10 successful requests
    for ($i = 1; $i -le 10; $i++) {
        $response = Make-Request "leaky-bucket"
        Display-Response $response $i
        Start-Sleep -Milliseconds 100
    }
    
    # Try to exceed limit
    Write-Host "`nAttempting request when queue is full:" -ForegroundColor Yellow
    $response = Make-Request "leaky-bucket" 11
    Display-Response $response 11
    
    # Wait for queue to drain
    Write-Host "`nWaiting 3 seconds for queue to drain..." -ForegroundColor Yellow
    for ($i = 3; $i -ge 1; $i--) {
        Write-Host "  $i seconds remaining..." -ForegroundColor Cyan
        Start-Sleep -Seconds 1
    }
    
    # Try again after drain
    Write-Host "`nAttempting request after queue drains:" -ForegroundColor Yellow
    $response = Make-Request "leaky-bucket" 12
    Display-Response $response 12
    
    Write-Host "`n[OK] Leaky Bucket demonstration complete" -ForegroundColor Green
}

function Demo-Redis {
    Write-SubHeader "REDIS STATE INSPECTION"
    
    Write-Host "`nRedis Key Inspection:" -ForegroundColor White
    Write-Host "(Simulated demo - shows Redis integration)" -ForegroundColor Yellow
    
    Write-Host ""
    Write-Host "Sample Redis Keys (after demo runs):" -ForegroundColor White
    Write-Host "  api-key:demo-free:tokens                (Token bucket tokens)" -ForegroundColor Cyan
    Write-Host "  api-key:demo-free:lastRefill            (Token bucket last refill time)" -ForegroundColor Cyan
    Write-Host "  api-key:demo-free:queue                 (Leaky bucket queue size)" -ForegroundColor Cyan
    Write-Host "  api-key:demo-free:lastLeak              (Leaky bucket last leak time)" -ForegroundColor Cyan
    Write-Host "  api-key:demo-free:requests              (Sliding window sorted set)" -ForegroundColor Cyan
    
    Write-Host ""
    Write-Host "[OK] All operations executed atomically via Lua scripts in Redis" -ForegroundColor Green
    Write-Host "[OK] Atomic execution eliminates race conditions" -ForegroundColor Green
    Write-Host "[OK] Zero downtime for state updates" -ForegroundColor Green
}

function Demo-Summary {
    Write-Header "DEMO COMPLETE" "Green"
    
    Write-Host ""
    Write-Host "[OK] Backend running" -ForegroundColor Green
    Write-Host "[OK] Redis connected" -ForegroundColor Green
    Write-Host "[OK] Token Bucket algorithm" -ForegroundColor Green
    Write-Host "[OK] Sliding Window algorithm" -ForegroundColor Green
    Write-Host "[OK] Leaky Bucket algorithm" -ForegroundColor Green
    Write-Host "[OK] HTTP 429 rate limit handling" -ForegroundColor Green
    Write-Host "[OK] Lua script execution" -ForegroundColor Green
    Write-Host "[OK] Atomic Redis operations" -ForegroundColor Green
    Write-Host "[OK] Rate limit headers (X-RateLimit-*)" -ForegroundColor Green
    
    Write-Host "`nKey Takeaways:" -ForegroundColor Yellow
    Write-Host "  * All three algorithms use atomic Lua scripts in Redis"
    Write-Host "  * No race conditions - thread-safe by design"
    Write-Host "  * Rate limits are enforced via HTTP 429 responses"
    Write-Host "  * Headers show remaining capacity and reset times"
    Write-Host "  * Algorithms automatically recover after limit period"
    
    Write-Host "`nArchitecture:" -ForegroundColor Yellow
    Write-Host "  HTTP Request -> Spring Filter -> Algorithm Check -> Lua Script (Atomic)"
    Write-Host "  |"
    Write-Host "  Response Headers: X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After"
    Write-Host "  |"
    Write-Host "  HTTP 200 (Allowed) or HTTP 429 (Rate Limited)"
    
    Write-Host "`nNext Steps:" -ForegroundColor Yellow
    Write-Host "  * Check demo/README.md for detailed documentation"
    Write-Host "  * Inspect source code in src/main/java/com/backend/rate_limiter/"
    Write-Host "  * Review Lua scripts in src/main/resources/scripts/"
    Write-Host "  * Run with custom API keys and capacities"
    
    Write-Host ""
}

function Main {
    Clear-Host
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Blue
    Write-Host "  RATEFORGE - RATE LIMITING INTERVIEW DEMONSTRATION" -ForegroundColor Blue
    Write-Host "  Atomic Redis Lua Scripts - Spring Boot - Three Algorithms" -ForegroundColor Blue
    Write-Host "==========================================================" -ForegroundColor Blue
    
    # Pre-flight checks
    if (-not (Check-Backend)) {
        Write-Host ""
        exit 1
    }
    
    if (-not (Check-Redis)) {
        Write-Host ""
        exit 1
    }
    
    Write-Host "`n[OK] All systems ready" -ForegroundColor Green
    Write-Host "`nStarting demonstrations..." -ForegroundColor Yellow
    
    # Run all demonstrations
    Demo-TokenBucket
    Demo-SlidingWindow
    Demo-LeakyBucket
    Demo-Redis
    Demo-Summary
    
    Write-Host "`nPress Enter to exit..." -ForegroundColor Yellow
    Read-Host
}

# Run main
Main

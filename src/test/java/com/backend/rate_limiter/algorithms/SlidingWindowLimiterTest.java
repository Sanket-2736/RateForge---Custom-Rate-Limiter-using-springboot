package com.backend.rate_limiter.algorithms;

import com.backend.rate_limiter.dto.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SlidingWindowLimiter Tests
 * 
 * Tests for the atomic sliding window rate limiter using Redis sorted sets and Lua scripts.
 * Tests verify:
 * 1. Requests within the limit are allowed
 * 2. Requests exceeding the limit are rejected
 * 3. Old requests fall out of the window and new requests are allowed
 * 
 * All tests use explicit timestamps (not System.currentTimeMillis()) to simulate
 * time passing without actual Thread.sleep delays.
 * 
 * NOTE: This test requires a Redis instance running on localhost:6379
 * If Redis is not available, tests will be skipped or fail appropriately.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SlidingWindowLimiter Tests")
class SlidingWindowLimiterTest {

    @Autowired(required = false)
    private SlidingWindowLimiter slidingWindowLimiter;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private static final String TEST_KEY = "sliding-window-test";
    private static final long WINDOW_SIZE_MS = 1000;  // 1 second window
    private static final int MAX_REQUESTS = 3;

    private boolean redisAvailable = false;
    private long baseTimestamp = 1000000;  // Start at an arbitrary timestamp

    @BeforeEach
    void setUp() {
        if (slidingWindowLimiter == null || stringRedisTemplate == null) {
            redisAvailable = false;
            System.out.println("\n⚠️  Redis is not available. Skipping tests that require Redis connection.");
            return;
        }

        try {
            stringRedisTemplate.delete(TEST_KEY);
            redisAvailable = true;
            System.out.println("\n✓ Redis connection successful. Running sliding window tests...\n");
        } catch (Exception e) {
            redisAvailable = false;
            System.out.println("\n⚠️  Redis connection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 1: Requests within the limit are allowed")
    void testRequestsWithinLimit() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("=== Test 1: Requests Within Limit ===");
        System.out.println("Window: 1000ms, Max: 3 requests");
        
        // All requests at the same timestamp within the window
        long now = baseTimestamp;

        RateLimitResult result1 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        assertTrue(result1.allowed(), "1st request should be allowed");
        assertEquals(1, result1.remainingTokens(), "1 request in window");
        System.out.println("Request 1: ALLOWED ✓ (in window: 1)");

        RateLimitResult result2 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        assertTrue(result2.allowed(), "2nd request should be allowed");
        assertEquals(2, result2.remainingTokens(), "2 requests in window");
        System.out.println("Request 2: ALLOWED ✓ (in window: 2)");

        RateLimitResult result3 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        assertTrue(result3.allowed(), "3rd request should be allowed");
        assertEquals(3, result3.remainingTokens(), "3 requests in window");
        System.out.println("Request 3: ALLOWED ✓ (in window: 3)");
    }

    @Test
    @DisplayName("Test 2: Requests exceeding the limit are rejected")
    void testRequestsExceedingLimit() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("=== Test 2: Requests Exceeding Limit ===");
        System.out.println("Window: 1000ms, Max: 3 requests");
        
        long now = baseTimestamp;

        // Allow 3 requests
        slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        System.out.println("Requests 1-3: ALLOWED ✓");

        // 4th request should be rejected (still within the window)
        RateLimitResult result4 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        assertFalse(result4.allowed(), "4th request should be rejected (limit exceeded)");
        assertEquals(3, result4.remainingTokens(), "Still 3 requests in window (4th not added)");
        System.out.println("Request 4: REJECTED ✓ (limit exceeded)");

        // 5th request should also be rejected
        RateLimitResult result5 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        assertFalse(result5.allowed(), "5th request should be rejected (limit exceeded)");
        assertEquals(3, result5.remainingTokens(), "Still 3 requests in window");
        System.out.println("Request 5: REJECTED ✓ (limit exceeded)");
    }

    @Test
    @DisplayName("Test 3: Old requests fall out of window, new requests are allowed")
    void testWindowSliding() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("=== Test 3: Sliding Window (Time Passing) ===");
        System.out.println("Window: 1000ms, Max: 3 requests");
        
        long now = baseTimestamp;

        // Phase 1: Fill the window with 3 requests at time T
        System.out.println("\nPhase 1: T=" + now + "ms - Fill window");
        slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        RateLimitResult result3a = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        assertTrue(result3a.allowed(), "3rd request should be allowed");
        assertEquals(3, result3a.remainingTokens());
        System.out.println("  Requests 1-3: ALLOWED ✓");

        // Phase 2: Try 4th request at same time (should be rejected)
        System.out.println("\nPhase 2: T=" + now + "ms - Exceed limit");
        RateLimitResult result4 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
        assertFalse(result4.allowed(), "4th request should be rejected");
        assertEquals(3, result4.remainingTokens());
        System.out.println("  Request 4: REJECTED ✓ (limit exceeded)");

        // Phase 3: Advance time just slightly (within window) - should still be rejected
        System.out.println("\nPhase 3: T=" + (now + 500) + "ms - Still in window");
        long now2 = now + 500;  // 500ms later
        RateLimitResult result5 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now2);
        assertFalse(result5.allowed(), "Request should be rejected (still in window)");
        assertEquals(3, result5.remainingTokens());
        System.out.println("  Request 5: REJECTED ✓ (still in window)");

        // Phase 4: Advance time past the window (>1000ms) - oldest requests fall out
        System.out.println("\nPhase 4: T=" + (now + 1001) + "ms - Window slides, old requests expire");
        long now3 = now + 1001;  // 1001ms later - window has moved
        RateLimitResult result6 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now3);
        assertTrue(result6.allowed(), "Request should be allowed (old requests fell out of window)");
        System.out.println("  Request 6: ALLOWED ✓ (old requests expired from window)");
        System.out.println("  Remaining in window: " + result6.remainingTokens());

        // Phase 5: Can add more requests now
        System.out.println("\nPhase 5: T=" + now3 + "ms - Add more requests in new window");
        RateLimitResult result7 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now3);
        assertTrue(result7.allowed(), "7th request should be allowed");
        System.out.println("  Request 7: ALLOWED ✓");

        RateLimitResult result8 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now3);
        assertTrue(result8.allowed(), "8th request should be allowed");
        System.out.println("  Request 8: ALLOWED ✓");

        // Should be at capacity now
        RateLimitResult result9 = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now3);
        assertFalse(result9.allowed(), "9th request should be rejected (new limit reached)");
        System.out.println("  Request 9: REJECTED ✓ (new limit reached)");
    }

    @Test
    @DisplayName("Test 4: Concurrent sliding window requests are atomic")
    void testConcurrentSlidingWindowRequests() throws InterruptedException {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("=== Test 4: Atomic Behavior Under Concurrent Load ===");
        System.out.println("Window: 1000ms, Max: 3 requests, 10 concurrent threads");
        
        int numThreads = 10;
        long now = baseTimestamp;
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch endLatch = new java.util.concurrent.CountDownLatch(numThreads);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
        java.util.concurrent.atomic.AtomicInteger allowedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RateLimitResult result = slidingWindowLimiter.checkAndRecord(TEST_KEY, WINDOW_SIZE_MS, MAX_REQUESTS, now);
                    if (result.allowed()) {
                        allowedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdownNow();

        System.out.println("Requests allowed: " + allowedCount.get());
        System.out.println("Requests rejected: " + (numThreads - allowedCount.get()));
        assertEquals(MAX_REQUESTS, allowedCount.get(), 
            "With atomic operations, exactly " + MAX_REQUESTS + " requests should be allowed, " +
            "not " + allowedCount.get());
        System.out.println("✓ Atomic behavior confirmed: exactly " + MAX_REQUESTS + " allowed");
    }
}

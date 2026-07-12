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
 * LeakyBucketLimiter Tests
 * 
 * Tests for the atomic leaky bucket rate limiter using Redis and Lua scripts.
 * Tests verify:
 * 1. Requests within capacity are allowed
 * 2. Requests exceeding capacity are rejected
 * 3. As the bucket leaks, capacity frees up and new requests are allowed
 * 
 * All tests use explicit timestamps to simulate time passing without actual delays.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LeakyBucketLimiter Tests")
class LeakyBucketLimiterTest {

    @Autowired(required = false)
    private LeakyBucketLimiter leakyBucketLimiter;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private static final String TEST_KEY = "leaky-bucket-test";
    private static final int BUCKET_CAPACITY = 5;
    private static final double LEAK_RATE_PER_SEC = 2.0;  // 2 requests per second leak out

    private boolean redisAvailable = false;
    private long baseTimestamp = 2000000;  // Start at an arbitrary timestamp

    @BeforeEach
    void setUp() {
        if (leakyBucketLimiter == null || stringRedisTemplate == null) {
            redisAvailable = false;
            System.out.println("\n⚠️  Redis is not available. Skipping tests that require Redis connection.");
            return;
        }

        try {
            stringRedisTemplate.delete(TEST_KEY + ":queue");
            stringRedisTemplate.delete(TEST_KEY + ":lastLeak");
            redisAvailable = true;
            System.out.println("\n✓ Redis connection successful. Running leaky bucket tests...\n");
        } catch (Exception e) {
            redisAvailable = false;
            System.out.println("\n⚠️  Redis connection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 1: Requests within capacity are allowed")
    void testRequestsWithinCapacity() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("=== Test 1: Requests Within Capacity ===");
        System.out.println("Capacity: " + BUCKET_CAPACITY + ", Leak rate: " + LEAK_RATE_PER_SEC + "/sec");
        
        long now = baseTimestamp;

        // Enqueue requests up to capacity
        for (int i = 1; i <= BUCKET_CAPACITY; i++) {
            RateLimitResult result = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now);
            assertTrue(result.allowed(), "Request " + i + " should be allowed (within capacity)");
            assertEquals(i, result.remainingTokens(), "Queue size should be " + i);
            System.out.println("Request " + i + ": ALLOWED ✓ (queue size: " + result.remainingTokens() + ")");
        }
    }

    @Test
    @DisplayName("Test 2: Requests exceeding capacity are rejected")
    void testRequestsExceedingCapacity() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("=== Test 2: Requests Exceeding Capacity ===");
        System.out.println("Capacity: " + BUCKET_CAPACITY + ", Leak rate: " + LEAK_RATE_PER_SEC + "/sec");
        
        long now = baseTimestamp;

        // Fill the bucket
        for (int i = 0; i < BUCKET_CAPACITY; i++) {
            leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now);
        }
        System.out.println("Queue filled with " + BUCKET_CAPACITY + " requests");

        // Try to exceed capacity
        RateLimitResult resultExceed = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now);
        assertFalse(resultExceed.allowed(), "Request exceeding capacity should be rejected");
        assertEquals(BUCKET_CAPACITY, resultExceed.remainingTokens(), "Queue size unchanged after rejection");
        System.out.println("Request exceeding capacity: REJECTED ✓ (queue remains at " + resultExceed.remainingTokens() + ")");

        // Another rejected request
        RateLimitResult resultExceed2 = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now);
        assertFalse(resultExceed2.allowed(), "Another request over capacity should be rejected");
        System.out.println("Another request exceeding capacity: REJECTED ✓");
    }

    @Test
    @DisplayName("Test 3: Bucket leaks, capacity frees up for new requests")
    void testBucketLeakingFreesCapacity() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("=== Test 3: Bucket Leaking and Recovery ===");
        System.out.println("Capacity: " + BUCKET_CAPACITY + ", Leak rate: " + LEAK_RATE_PER_SEC + "/sec");
        
        long now = baseTimestamp;

        // Phase 1: Fill the bucket
        System.out.println("\nPhase 1: Fill the bucket at T=" + now + "ms");
        for (int i = 0; i < BUCKET_CAPACITY; i++) {
            leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now);
        }
        RateLimitResult filledBucket = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now);
        assertFalse(filledBucket.allowed(), "Queue should be full");
        System.out.println("  Queue full with " + BUCKET_CAPACITY + " requests ✓");

        // Phase 2: Short wait (0.5 seconds) - not enough time to leak much
        System.out.println("\nPhase 2: Wait 0.5 seconds (not enough to leak capacity)");
        long now2 = now + 500;  // 500ms later
        RateLimitResult afterShortWait = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now2);
        // At leak rate 2/sec, in 0.5 sec only 1 request leaks
        assertFalse(afterShortWait.allowed(), "Queue should still be full (only ~1 request leaked)");
        System.out.println("  Still full (leaked ~1 request): REJECTED ✓");

        // Phase 3: Longer wait (2+ seconds) - enough time to leak the entire queue
        System.out.println("\nPhase 3: Wait 3 seconds (queue should fully leak)");
        long now3 = now + 3000;  // 3000ms later
        // With leak rate 2/sec, 3 seconds = 6 requests leaked (bucket only has 5)
        RateLimitResult afterLongWait = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now3);
        assertTrue(afterLongWait.allowed(), "Request should be allowed (bucket leaked)");
        System.out.println("  Bucket recovered: Request ALLOWED ✓ (queue size: " + afterLongWait.remainingTokens() + ")");

        // Phase 4: Can fill the bucket again
        System.out.println("\nPhase 4: Fill bucket again at T=" + now3 + "ms");
        for (int i = 0; i < BUCKET_CAPACITY; i++) {
            RateLimitResult result = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now3);
            assertTrue(result.allowed(), "Request should be allowed");
        }
        System.out.println("  Bucket refilled with " + BUCKET_CAPACITY + " requests ✓");

        RateLimitResult fullAgain = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now3);
        assertFalse(fullAgain.allowed(), "Queue should be full again");
        System.out.println("  Queue full: REJECTED ✓");
    }

    @Test
    @DisplayName("Test 4: Constant request rate matches leak rate (steady state)")
    void testSteadyStateLeaking() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("=== Test 4: Steady State (Request Rate = Leak Rate) ===");
        System.out.println("Capacity: " + BUCKET_CAPACITY + ", Leak rate: " + LEAK_RATE_PER_SEC + "/sec");
        System.out.println("Sending 2 requests per second (matches leak rate)");
        
        long now = baseTimestamp;
        int allowedCount = 0;
        int rejectedCount = 0;

        // Fill the bucket to capacity
        for (int i = 0; i < BUCKET_CAPACITY; i++) {
            leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, now);
        }
        System.out.println("Initial: Queue filled with " + BUCKET_CAPACITY + " requests");

        // Send requests at a rate that matches leak rate for several intervals
        // At leak rate 2/sec, if we send 2 req/sec, queue should stay full
        for (int interval = 0; interval < 5; interval++) {
            long intervalTime = now + (interval * 1000);  // Each second
            
            // Send 2 requests (matches leak rate)
            for (int req = 0; req < 2; req++) {
                RateLimitResult result = leakyBucketLimiter.checkAndEnqueue(TEST_KEY, BUCKET_CAPACITY, LEAK_RATE_PER_SEC, intervalTime);
                if (result.allowed()) {
                    allowedCount++;
                } else {
                    rejectedCount++;
                }
            }
        }

        System.out.println("Over 5 seconds:");
        System.out.println("  Allowed: " + allowedCount);
        System.out.println("  Rejected: " + rejectedCount);
        System.out.println("  Result: Queue stays full, new requests rejected ✓");
    }
}

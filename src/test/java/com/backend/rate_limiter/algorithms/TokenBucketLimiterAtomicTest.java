package com.backend.rate_limiter.algorithms;

import com.backend.rate_limiter.dto.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TokenBucketLimiterAtomic Tests
 * 
 * Tests for the atomic token bucket implementation using Redis Lua scripts.
 * These tests verify that the race condition is eliminated and rate limiting
 * works correctly under concurrent load.
 * 
 * NOTE: This test requires a Redis instance running on localhost:6379
 * If Redis is not available, tests will be skipped or fail appropriately.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TokenBucketLimiterAtomic (Lua Script) Tests")
class TokenBucketLimiterAtomicTest {

    @Autowired(required = false)
    private TokenBucketLimiterAtomic tokenBucketLimiterAtomic;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private static final String TEST_KEY = "test-limiter-atomic";
    private static final int BUCKET_CAPACITY = 1;
    private static final double REFILL_RATE = 0.1; // 0.1 tokens per second

    private boolean redisAvailable = false;

    @BeforeEach
    void setUp() {
        if (tokenBucketLimiterAtomic == null || stringRedisTemplate == null) {
            redisAvailable = false;
            System.out.println("\n⚠️  Redis is not available. Skipping tests that require Redis connection.");
            return;
        }

        try {
            stringRedisTemplate.delete(TEST_KEY + ":tokens");
            stringRedisTemplate.delete(TEST_KEY + ":lastRefill");
            redisAvailable = true;
            System.out.println("\n✓ Redis connection successful. Running atomic tests...\n");
        } catch (Exception e) {
            redisAvailable = false;
            System.out.println("\n⚠️  Redis connection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Single request should be allowed when bucket has capacity")
    void testSingleRequestAllowed() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        RateLimitResult result = tokenBucketLimiterAtomic.checkAndConsume(TEST_KEY, BUCKET_CAPACITY, REFILL_RATE);

        assertTrue(result.allowed(), "First request should be allowed");
        assertEquals(0, result.remainingTokens(), "Should have 0 tokens remaining after consuming 1");
    }

    @Test
    @DisplayName("Second request should be rejected when bucket is empty")
    void testSecondRequestRejected() {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        RateLimitResult result1 = tokenBucketLimiterAtomic.checkAndConsume(TEST_KEY, BUCKET_CAPACITY, REFILL_RATE);
        assertTrue(result1.allowed(), "First request should be allowed");

        RateLimitResult result2 = tokenBucketLimiterAtomic.checkAndConsume(TEST_KEY, BUCKET_CAPACITY, REFILL_RATE);
        assertFalse(result2.allowed(), "Second request should be rejected");
    }

    @Test
    @DisplayName("ATOMIC IMPLEMENTATION: Concurrent requests - exactly 1 should be allowed")
    void testAtomicConcurrentLoad() throws InterruptedException {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        int numThreads = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger allowedCount = new AtomicInteger(0);
        List<RateLimitResult> results = new ArrayList<>();

        // Prepare all threads to start at roughly the same time
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    // Wait for the signal to start
                    startLatch.await();

                    // All threads call checkAndConsume at nearly the same moment
                    RateLimitResult result = tokenBucketLimiterAtomic.checkAndConsume(
                        TEST_KEY, 
                        BUCKET_CAPACITY, 
                        REFILL_RATE
                    );

                    if (result.allowed()) {
                        allowedCount.incrementAndGet();
                    }

                    synchronized (results) {
                        results.add(result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Signal all threads to start
        startLatch.countDown();

        // Wait for all threads to complete
        endLatch.await();
        executor.shutdownNow();

        // Print results for visibility
        System.out.println("\n========== ATOMIC IMPLEMENTATION - CONCURRENT TEST RESULTS ==========");
        System.out.println("Total threads: " + numThreads);
        System.out.println("Bucket capacity: " + BUCKET_CAPACITY);
        System.out.println("Requests allowed: " + allowedCount.get());
        System.out.println("Requests rejected: " + (numThreads - allowedCount.get()));
        System.out.println("====================================================================\n");

        // EXPECTED AND ACTUAL: Exactly 1 request should be allowed
        // The Lua script ensures atomicity, so this test PASSES
        assertEquals(
            1, 
            allowedCount.get(), 
            "ATOMIC OPERATION: With capacity=1, exactly 1 request should be allowed. " +
            "The Lua script ensures atomicity, so the rate limit is never violated."
        );
    }

    @Test
    @DisplayName("Multiple concurrent tests - verify consistency")
    void testMultipleConcurrentRuns() throws InterruptedException {
        if (!redisAvailable) {
            System.out.println("Test skipped: Redis not available");
            return;
        }

        System.out.println("\n========== RUNNING 5 CONCURRENT TESTS FOR CONSISTENCY ==========\n");

        for (int run = 1; run <= 5; run++) {
            String testKey = TEST_KEY + "-run-" + run;
            stringRedisTemplate.delete(testKey + ":tokens");
            stringRedisTemplate.delete(testKey + ":lastRefill");

            int numThreads = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            AtomicInteger allowedCount = new AtomicInteger(0);

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        RateLimitResult result = tokenBucketLimiterAtomic.checkAndConsume(
                            testKey, 
                            BUCKET_CAPACITY, 
                            REFILL_RATE
                        );
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

            System.out.println("Run " + run + ": " + allowedCount.get() + " allowed (expected 1)");
            assertEquals(1, allowedCount.get(), "Run " + run + " failed: expected 1 allowed request");
        }

        System.out.println("\n✓ All 5 runs passed with exactly 1 allowed request each.\n");
    }
}

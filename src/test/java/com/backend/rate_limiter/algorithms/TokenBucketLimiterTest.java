package com.backend.rate_limiter.algorithms;

import com.backend.rate_limiter.dto.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * TokenBucketLimiter Tests - Naive Implementation
 * 
 * Tests for the naive token bucket implementation using non-atomic Redis operations.
 * These tests demonstrate the race condition vulnerability.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TokenBucketLimiter (Naive) Tests")
class TokenBucketLimiterTest {

    @Autowired(required = false)
    private TokenBucketLimiter tokenBucketLimiter;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private static final String TEST_KEY = "test-limiter-naive";
    private static final int BUCKET_CAPACITY = 1;
    private static final double REFILL_RATE = 0.1;

    private boolean redisAvailable = false;

    @BeforeEach
    void setUp() {
        if (tokenBucketLimiter == null || stringRedisTemplate == null) {
            redisAvailable = false;
            System.out.println("\n⚠️  Redis is not available. Skipping tests that require Redis connection.");
            return;
        }

        try {
            stringRedisTemplate.delete(TEST_KEY + ":tokens");
            stringRedisTemplate.delete(TEST_KEY + ":lastRefill");
            redisAvailable = true;
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

        RateLimitResult result = tokenBucketLimiter.checkAndConsume(TEST_KEY, BUCKET_CAPACITY, REFILL_RATE);

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

        RateLimitResult result1 = tokenBucketLimiter.checkAndConsume(TEST_KEY, BUCKET_CAPACITY, REFILL_RATE);
        assertTrue(result1.allowed(), "First request should be allowed");

        RateLimitResult result2 = tokenBucketLimiter.checkAndConsume(TEST_KEY, BUCKET_CAPACITY, REFILL_RATE);
        assertFalse(result2.allowed(), "Second request should be rejected");
    }

    @Test
    @DisplayName("RACE CONDITION TEST: Concurrent requests demonstrate the bug in naive implementation")
    void testRaceConditionUnderConcurrentLoad() throws InterruptedException {
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

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    RateLimitResult result = tokenBucketLimiter.checkAndConsume(
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

        startLatch.countDown();
        endLatch.await();
        executor.shutdownNow();

        System.out.println("\n========== NAIVE IMPLEMENTATION - RACE CONDITION TEST ==========");
        System.out.println("Total threads: " + numThreads);
        System.out.println("Bucket capacity: " + BUCKET_CAPACITY);
        System.out.println("Requests allowed: " + allowedCount.get());
        System.out.println("Requests rejected: " + (numThreads - allowedCount.get()));
        System.out.println("================================================================\n");

        // This test FAILS intentionally - showing the race condition exists
        assertEquals(
            1, 
            allowedCount.get(), 
            "RACE CONDITION: Expected only 1 request to be allowed with capacity=1, " +
            "but got " + allowedCount.get() + ". This demonstrates the non-atomic nature " +
            "of the naive implementation."
        );
    }
}


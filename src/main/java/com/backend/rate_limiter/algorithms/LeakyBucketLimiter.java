package com.backend.rate_limiter.algorithms;

import com.backend.rate_limiter.dto.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scripting.ScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Leaky Bucket Rate Limiter using Redis and Lua Scripts.
 * 
 * The leaky bucket algorithm models a queue with fixed capacity that drains
 * (leaks) at a constant rate. Requests are added to the queue if there's space;
 * they're rejected if the queue is full.
 * 
 * KEY CHARACTERISTIC: Smooths bursty traffic into a steady, predictable output.
 * 
 * COMPARISON WITH TOKEN BUCKET:
 * 
 * Token Bucket:
 * - Accumulates tokens up to capacity
 * - Allows bursts: if bucket is full, burst of capacity requests go through
 * - Good for: APIs that can handle occasional traffic spikes
 * - Example: 100 req/sec capacity, if idle for 10 sec → 1000 tokens accumulated
 *            → all 1000 requests allowed immediately (burst)
 * 
 * Leaky Bucket:
 * - Queue fills up with requests, drains at fixed rate
 * - Smooth output: requests process at leak rate, no bursts
 * - Good for: systems that need predictable, steady load downstream
 * - Example: 100 req/sec capacity, if idle for 10 sec → queue empty
 *            → requests process at exactly 100 req/sec, no faster
 * 
 * ALGORITHM:
 * 1. On each request, calculate how much the bucket has leaked since last request
 * 2. Remove leaked requests from the queue
 * 3. If queue size < capacity: add request and allow
 * 4. Else: reject request (queue full)
 * 
 * RACE CONDITION PROTECTION:
 * ⚠️ WITHOUT ATOMICITY:
 * Thread 1: Read queue (size=10) at time T1
 * Thread 2: Read queue (size=10) at time T1  ← same value!
 * Thread 1: Calculate leak, check (10 < 100) → allowed, write queue=11
 * Thread 2: Calculate leak, check (10 < 100) → allowed, write queue=11 ← both allowed!
 * Result: Queue size never reaches 12 (second write overwrites first)
 * 
 * ✓ WITH ATOMICITY (Lua script):
 * Thread 1: EVAL (read, calculate, check, write) → allowed, queue=11
 * Thread 2: EVAL (read, calculate, check, write) → allowed, queue=12
 * Result: Perfect serialization, queue grows correctly
 */
@Component
public class LeakyBucketLimiter {
    private static final Logger logger = LoggerFactory.getLogger(LeakyBucketLimiter.class);
    private static final String QUEUE_SIZE_KEY_SUFFIX = ":queue";
    private static final String LAST_LEAK_KEY_SUFFIX = ":lastLeak";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ScriptSource leakyBucketScriptSource;

    /**
     * Check if a request can be enqueued and add it if accepted.
     * 
     * Uses a Lua script to atomically:
     * 1. Calculate how much the bucket has leaked since last request
     * 2. Remove leaked capacity from the queue
     * 3. Check if there's space in the bucket
     * 4. If allowed, increment the queue size
     * 5. Return decision and current queue size
     * 
     * This ensures steady output at leakRatePerSec, smoothing any bursty input.
     * 
     * @param key The rate limit key (e.g., user ID, IP address)
     * @param capacity Maximum queue size (bucket capacity)
     * @param leakRatePerSec Rate at which requests drain from the queue (per second)
     * @param now Current timestamp in milliseconds (for testability)
     * @return RateLimitResult with allowed flag and current queue size
     */
    public RateLimitResult checkAndEnqueue(String key, int capacity, double leakRatePerSec, long now) {
        String queueKey = key + QUEUE_SIZE_KEY_SUFFIX;
        String lastLeakKey = key + LAST_LEAK_KEY_SUFFIX;

        try {
            // Read the Lua script
            String script = leakyBucketScriptSource.getScriptAsString();

            // Execute the Lua script atomically on Redis server
            Object result = stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                Object evalResult = connection.eval(
                    script.getBytes(),
                    null,  // ReturnType - let Redis determine
                    2,     // number of keys
                    queueKey.getBytes(),
                    lastLeakKey.getBytes(),
                    String.valueOf(now).getBytes(),
                    String.valueOf(capacity).getBytes(),
                    String.valueOf(leakRatePerSec).getBytes()
                );
                return evalResult;
            });

            if (result instanceof List<?>) {
                List<?> resultList = (List<?>) result;
                if (resultList.size() >= 2) {
                    long allowed = ((Number) resultList.get(0)).longValue();
                    long queueSize = ((Number) resultList.get(1)).longValue();
                    boolean requestAllowed = allowed == 1;

                    if (requestAllowed) {
                        logger.debug("Request enqueued. Key: {}, Leak rate: {}/sec, Queue size: {}/{}", 
                            key, leakRatePerSec, queueSize, capacity);
                    } else {
                        logger.debug("Request rejected (queue full). Key: {}, Queue size: {}/{}", 
                            key, queueSize, capacity);
                    }

                    return new RateLimitResult(requestAllowed, queueSize);
                }
            }

            logger.warn("Unexpected script result type: {}", result);
            return new RateLimitResult(false, 0);
        } catch (Exception e) {
            logger.error("Error executing leaky bucket script for key: {}", key, e);
            // Fail open: allow the request if script fails (don't drop traffic)
            return new RateLimitResult(true, 0);
        }
    }

    /**
     * Check if a request can be enqueued and add it if accepted.
     * Convenience method that uses the current system time.
     * 
     * @param key The rate limit key
     * @param capacity Maximum queue size
     * @param leakRatePerSec Leak rate per second
     * @return RateLimitResult with allowed flag and current queue size
     */
    public RateLimitResult checkAndEnqueue(String key, int capacity, double leakRatePerSec) {
        return checkAndEnqueue(key, capacity, leakRatePerSec, System.currentTimeMillis());
    }
}

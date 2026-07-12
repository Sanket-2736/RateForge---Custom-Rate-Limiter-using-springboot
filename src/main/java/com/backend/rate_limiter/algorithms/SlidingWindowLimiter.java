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
 * Sliding Window Rate Limiter using Redis Sorted Sets and Lua Scripts.
 * 
 * This implementation uses Redis sorted sets to track request timestamps within
 * a sliding time window. The entire operation (trim old entries, count, check,
 * add) executes atomically on the Redis server.
 * 
 * HOW IT WORKS:
 * 1. Uses a Redis sorted set where the score is the request timestamp
 * 2. On each request:
 *    - Remove all entries with timestamp < (now - windowSize)
 *    - Count remaining entries
 *    - If count < maxRequests: add current timestamp and allow
 *    - Else: reject
 * 
 * RACE CONDITION PROTECTION:
 * ⚠️ WITHOUT ATOMICITY (naive approach):
 * Thread 1: ZCARD (count=2) ← sees count=2
 * Thread 2: ZCARD (count=2) ← sees count=2 (same value!)
 * Thread 1: Check (2 < 3) -> allowed, ZADD
 * Thread 2: Check (2 < 3) -> allowed, ZADD ← BOTH allowed!
 * Result: 4 requests let through instead of 3
 * 
 * ✓ WITH ATOMICITY (Lua script):
 * Thread 1: EVAL (ZCARD + check + ZADD) -> allowed
 * Thread 2: EVAL (ZCARD + check + ZADD) -> rejected (sees Thread 1's ZADD)
 * Result: Exactly 3 requests let through
 * 
 * The Lua script guarantees no interleaving between the count check and add.
 * 
 * ADVANTAGES:
 * - Accurate: no requests slip through when limit is reached
 * - Sliding: old requests automatically drop out of the window
 * - Memory efficient: old entries are automatically pruned
 * - Fair: FIFO-like behavior within the window
 * 
 * DISADVANTAGES:
 * - Memory: stores timestamp for every request (vs token bucket's simple counter)
 * - Overhead: Lua script execution on every request
 * - Precision: requires synchronized clocks for distributed systems
 */
@Component
public class SlidingWindowLimiter {
    private static final Logger logger = LoggerFactory.getLogger(SlidingWindowLimiter.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ScriptSource slidingWindowScriptSource;

    /**
     * Check if a request is allowed and record it if accepted.
     * 
     * Uses a Lua script to atomically:
     * 1. Remove timestamps outside the sliding window
     * 2. Count remaining requests
     * 3. Check if another request is allowed
     * 4. If allowed, add current timestamp
     * 5. Return decision
     * 
     * RACE CONDITION GUARANTEE:
     * The Lua script ensures the count-check-add sequence is atomic.
     * Two concurrent requests cannot both read an old count and then both add.
     * 
     * @param key The rate limit key (e.g., user ID, IP address)
     * @param windowSizeMs The sliding window size in milliseconds
     * @param maxRequests Maximum allowed requests within the window
     * @param now Current timestamp in milliseconds (for testability)
     * @return RateLimitResult with allowed flag and remaining request count
     */
    public RateLimitResult checkAndRecord(String key, long windowSizeMs, int maxRequests, long now) {
        try {
            // Read the Lua script
            String script = slidingWindowScriptSource.getScriptAsString();

            // Execute the Lua script atomically on Redis server
            Object result = stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                Object evalResult = connection.eval(
                    script.getBytes(),
                    null,  // ReturnType - let Redis determine
                    1,     // number of keys
                    key.getBytes(),
                    String.valueOf(now).getBytes(),
                    String.valueOf(windowSizeMs).getBytes(),
                    String.valueOf(maxRequests).getBytes()
                );
                return evalResult;
            });

            if (result instanceof List<?>) {
                List<?> resultList = (List<?>) result;
                if (resultList.size() >= 2) {
                    long allowed = ((Number) resultList.get(0)).longValue();
                    long remainingRequests = ((Number) resultList.get(1)).longValue();
                    boolean requestAllowed = allowed == 1;

                    if (requestAllowed) {
                        logger.debug("Request allowed. Key: {}, Window size: {}ms, Max requests: {}, Requests in window: {}", 
                            key, windowSizeMs, maxRequests, remainingRequests);
                    } else {
                        logger.debug("Request rejected. Key: {}, Window size: {}ms, Max requests: {}, Requests in window: {}", 
                            key, windowSizeMs, maxRequests, remainingRequests);
                    }

                    return new RateLimitResult(requestAllowed, remainingRequests);
                }
            }

            logger.warn("Unexpected script result type: {}", result);
            return new RateLimitResult(false, 0);
        } catch (Exception e) {
            logger.error("Error executing sliding window script for key: {}", key, e);
            // Fail open: allow the request if script fails (don't drop traffic)
            return new RateLimitResult(true, 0);
        }
    }

    /**
     * Check if a request is allowed and record it if accepted.
     * Convenience method that uses the current system time.
     * 
     * @param key The rate limit key
     * @param windowSizeMs The sliding window size in milliseconds
     * @param maxRequests Maximum allowed requests within the window
     * @return RateLimitResult with allowed flag and remaining request count
     */
    public RateLimitResult checkAndRecord(String key, long windowSizeMs, int maxRequests) {
        return checkAndRecord(key, windowSizeMs, maxRequests, System.currentTimeMillis());
    }
}

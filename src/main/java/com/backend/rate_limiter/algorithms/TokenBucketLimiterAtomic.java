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
 * Atomic Token Bucket Rate Limiter using Redis Lua Scripts.
 * 
 * This implementation performs the entire rate limiting check and token consumption
 * as a single atomic operation on the Redis server, eliminating race conditions.
 * 
 * The Lua script (token_bucket.lua) executes atomically, meaning:
 * - No two threads can interleave their reads/writes
 * - The check and consume happen together, preventing over-limit requests
 * - Perfect serialization of rate limit decisions
 * 
 * Key advantage over naive implementation:
 * Timeline with atomic operation:
 * Thread 1: EVAL (atomic GET, check, SET) -> allowed
 * Thread 2: EVAL (atomic GET, check, SET) -> rejected (sees Thread 1's update)
 * ✓ Exactly 1 allowed, as expected!
 */
@Component
public class TokenBucketLimiterAtomic {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucketLimiterAtomic.class);
    private static final String TOKEN_KEY_SUFFIX = ":tokens";
    private static final String LAST_REFILL_KEY_SUFFIX = ":lastRefill";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ScriptSource tokenBucketScriptSource;

    /**
     * Check if a request can be allowed and consume a token if available.
     * 
     * Uses a Lua script executed atomically on the Redis server to ensure
     * thread-safe rate limiting under concurrent load.
     * 
     * The script performs:
     * 1. GET current token count and last refill time
     * 2. Calculate tokens to add based on elapsed time
     * 3. Check if tokens >= 1
     * 4. If allowed: decrement tokens and update timestamp atomically
     * 5. Return {allowed, remainingTokens} as a single atomic operation
     * 
     * @param key The rate limit key (e.g., user ID, IP address)
     * @param capacity Maximum tokens in the bucket
     * @param refillRatePerSec Token refill rate per second
     * @return RateLimitResult with allowed flag and remaining token count
     */
    public RateLimitResult checkAndConsume(String key, int capacity, double refillRatePerSec) {
        long now = System.currentTimeMillis();
        String tokenKey = key + TOKEN_KEY_SUFFIX;
        String lastRefillKey = key + LAST_REFILL_KEY_SUFFIX;

        try {
            // Read the Lua script
            String script = tokenBucketScriptSource.getScriptAsString();

            // Execute the Lua script atomically on Redis server
            Object result = stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                Object evalResult = connection.eval(
                    script.getBytes(),
                    null,  // ReturnType - let Redis determine
                    2,     // number of keys
                    tokenKey.getBytes(),
                    lastRefillKey.getBytes(),
                    String.valueOf(now).getBytes(),
                    String.valueOf(capacity).getBytes(),
                    String.valueOf(refillRatePerSec).getBytes()
                );
                return evalResult;
            });

            if (result instanceof List<?>) {
                List<?> resultList = (List<?>) result;
                if (resultList.size() >= 2) {
                    long allowed = ((Number) resultList.get(0)).longValue();
                    long remainingTokens = ((Number) resultList.get(1)).longValue();
                    boolean requestAllowed = allowed == 1;

                    if (requestAllowed) {
                        logger.debug("Request allowed. Key: {}, Tokens remaining: {}", key, remainingTokens);
                    } else {
                        logger.debug("Request rejected. Key: {}, Remaining tokens: {}", key, remainingTokens);
                    }

                    return new RateLimitResult(requestAllowed, remainingTokens);
                }
            }

            logger.warn("Unexpected script result type: {}", result);
            return new RateLimitResult(false, 0);
        } catch (Exception e) {
            logger.error("Error executing rate limit script for key: {}", key, e);
            // Fail open: allow the request if script fails (don't drop traffic)
            return new RateLimitResult(true, 0);
        }
    }
}

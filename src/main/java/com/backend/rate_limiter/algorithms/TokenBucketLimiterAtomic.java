package com.backend.rate_limiter.algorithms;

import com.backend.rate_limiter.dto.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.ScriptSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Atomic Token Bucket Rate Limiter using Redis Lua Scripts.
 * 
 * This implementation performs the entire rate limiting check and token consumption
 * as a single atomic operation on the Redis server, eliminating race conditions.
 * 
 * Uses Spring Data Redis official API with RedisScript<List> for framework-agnostic
 * Lua script execution.
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
     * Check if a request is allowed and consume a token if permitted.
     * 
     * Executes atomically on Redis server using Lua script.
     * 
     * @param key The rate limit key (e.g., api-key:demo-free)
     * @param capacity Maximum tokens in the bucket
     * @param refillRatePerSec Tokens refilled per second
     * @return RateLimitResult with allowed flag and remaining tokens
     */
    public RateLimitResult checkAndConsume(String key, int capacity, double refillRatePerSec) {
        long now = System.currentTimeMillis();
        String tokenKey = key + TOKEN_KEY_SUFFIX;
        String lastRefillKey = key + LAST_REFILL_KEY_SUFFIX;

        logger.debug("[TokenBucket] Checking rate limit for key: {}, capacity: {}, refillRate: {}/sec", 
            key, capacity, refillRatePerSec);

        try {
            // Load the Lua script from classpath
            String scriptContent = tokenBucketScriptSource.getScriptAsString();
            logger.debug("[TokenBucket] Lua script loaded, length: {} bytes", scriptContent.length());

            // Create a RedisScript with List return type
            // This tells Spring Data Redis the script returns an array: {allowed, remainingTokens}
            DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(scriptContent);
            redisScript.setResultType(List.class);
            
            logger.debug("[TokenBucket] Created RedisScript with List return type");

            // Prepare keys list
            List<String> keys = new ArrayList<>();
            keys.add(tokenKey);
            keys.add(lastRefillKey);
            
            logger.debug("[TokenBucket] Keys: [{}, {}]", tokenKey, lastRefillKey);
            logger.debug("[TokenBucket] Arguments: [now={}, capacity={}, refillRate={}]", 
                now, capacity, refillRatePerSec);

            // Execute the script using Spring Data Redis official API
            // signature: execute(RedisScript<T> script, List<K> keys, Object... args)
            // This is framework-agnostic and works with any Redis driver
            List result = stringRedisTemplate.execute(
                redisScript,
                keys,
                String.valueOf(now),
                String.valueOf(capacity),
                String.valueOf(refillRatePerSec)
            );

            logger.debug("[TokenBucket] Script execution completed");

            if (result == null) {
                logger.error("[TokenBucket] Redis returned null result");
                return new RateLimitResult(false, 0);
            }

            if (result.isEmpty()) {
                logger.error("[TokenBucket] Redis returned empty list");
                return new RateLimitResult(false, 0);
            }

            logger.debug("[TokenBucket] Lua script returned list with {} elements", result.size());

            // Parse the result: Lua returns {allowed, remainingTokens}
            // Spring Data Redis converts it to List with Long elements
            if (result.size() < 2) {
                logger.error("[TokenBucket] Expected list with 2+ elements, got {}", result.size());
                for (int i = 0; i < result.size(); i++) {
                    logger.error("[TokenBucket]   Element[{}]: {} ({})", 
                        i, result.get(i), result.get(i) == null ? "null" : result.get(i).getClass().getSimpleName());
                }
                return new RateLimitResult(false, 0);
            }

            try {
                // Element 0: allowed flag (1 or 0 from Lua)
                Object allowedObj = result.get(0);
                Object remainingObj = result.get(1);

                logger.debug("[TokenBucket] Result[0]: {} (type: {})", 
                    allowedObj, allowedObj == null ? "null" : allowedObj.getClass().getSimpleName());
                logger.debug("[TokenBucket] Result[1]: {} (type: {})", 
                    remainingObj, remainingObj == null ? "null" : remainingObj.getClass().getSimpleName());

                // Convert to long values (Spring Data Redis typically returns Long or BigDecimal)
                long allowed = convertToLong(allowedObj);
                long remainingTokens = convertToLong(remainingObj);

                boolean requestAllowed = (allowed == 1);

                logger.info("[TokenBucket] DECISION: allowed={}, remaining={}, key={}", 
                    requestAllowed, remainingTokens, key);

                return new RateLimitResult(requestAllowed, remainingTokens);
            } catch (Exception e) {
                logger.error("[TokenBucket] Failed to parse result elements: {}", e.getMessage());
                logger.error("[TokenBucket] Result[0]: {} (type: {})", 
                    result.get(0), result.get(0) == null ? "null" : result.get(0).getClass().getSimpleName());
                logger.error("[TokenBucket] Result[1]: {} (type: {})", 
                    result.get(1), result.get(1) == null ? "null" : result.get(1).getClass().getSimpleName());
                logger.error("[TokenBucket] Parsing exception: ", e);
                return new RateLimitResult(false, 0);
            }
        } catch (Exception e) {
            logger.error("[TokenBucket] CRITICAL ERROR during script execution: {}", e.getMessage());
            logger.error("[TokenBucket] Exception type: {}", e.getClass().getName());
            logger.error("[TokenBucket] Exception: ", e);
            
            // Fail open: allow requests if Redis fails
            // Better to allow extra traffic than block all traffic when Redis is down
            logger.warn("[TokenBucket] FAIL-OPEN: allowing request due to error");
            return new RateLimitResult(true, 0);
        }
    }

    /**
     * Convert various types to long.
     * 
     * Spring Data Redis may return different types depending on configuration:
     * - Long (typical)
     * - java.math.BigDecimal
     * - String
     * - Number
     * 
     * @param value The value to convert
     * @return Long value
     * @throws NumberFormatException if conversion fails
     */
    private long convertToLong(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot convert null to long");
        }

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) value).longValue();
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        } else if (value instanceof byte[]) {
            return Long.parseLong(new String((byte[]) value));
        } else {
            return Long.parseLong(value.toString());
        }
    }
}

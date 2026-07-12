package com.backend.rate_limiter.algorithms;

import com.backend.rate_limiter.dto.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * NAIVE Token Bucket Rate Limiter using StringRedisTemplate.
 * 
 * ⚠️ DEPRECATED - Use TokenBucketLimiterAtomic instead
 * 
 * This implementation uses separate, non-atomic Redis calls to read and write
 * the token count and last-refill timestamp. This creates a RACE CONDITION
 * under concurrent load:
 * 
 * Timeline of race condition:
 * Thread 1: GET tokens (count=1) at time T1
 * Thread 2: GET tokens (count=1) at time T1  <- Both read same value!
 * Thread 1: Check availability (1 >= 1) -> allowed, SET tokens=0
 * Thread 2: Check availability (1 >= 1) -> allowed, SET tokens=0  <- BOTH allowed!
 * 
 * Two threads can both pass the availability check before either writes back,
 * violating the rate limit. This is intentional to demonstrate the problem
 * before implementing the atomic fix using Redis Lua scripts.
 * 
 * @deprecated Use TokenBucketLimiterAtomic which uses Lua scripts for atomicity
 * @see TokenBucketLimiterAtomic
 */
@Component
@Deprecated(forRemoval = true, since = "1.1.0")
public class TokenBucketLimiter {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucketLimiter.class);
    private static final String TOKEN_KEY_SUFFIX = ":tokens";
    private static final String LAST_REFILL_KEY_SUFFIX = ":lastRefill";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Check if a request can be allowed and consume a token if available.
     * 
     * RACE CONDITION WARNING:
     * This method performs separate, non-atomic Redis operations:
     * 1. GET current token count
     * 2. GET last refill timestamp
     * 3. Check availability
     * 4. SET new token count
     * 5. SET new last refill timestamp
     * 
     * Under concurrent load, multiple threads can read the same token count
     * before any thread writes back the updated value, allowing more requests
     * than the rate limit permits. This demonstrates why atomic operations
     * (Redis Lua scripts) are necessary for thread-safe rate limiting.
     * 
     * @param key The rate limit key (e.g., user ID, IP address)
     * @param capacity Maximum tokens in the bucket
     * @param refillRatePerSec Token refill rate per second
     * @return RateLimitResult with allowed flag and remaining token count
     * @deprecated Use TokenBucketLimiterAtomic instead
     */
    @Deprecated(forRemoval = true, since = "1.1.0")
    public RateLimitResult checkAndConsume(String key, int capacity, double refillRatePerSec) {
        long now = System.currentTimeMillis();
        String tokenKey = key + TOKEN_KEY_SUFFIX;
        String lastRefillKey = key + LAST_REFILL_KEY_SUFFIX;

        // RACE CONDITION: Non-atomic GET operations
        String tokenCountStr = stringRedisTemplate.opsForValue().get(tokenKey);
        String lastRefillStr = stringRedisTemplate.opsForValue().get(lastRefillKey);

        double currentTokens;
        long lastRefillTime;

        if (tokenCountStr == null) {
            // First request: initialize bucket to full capacity
            currentTokens = capacity;
            lastRefillTime = now;
        } else {
            currentTokens = Double.parseDouble(tokenCountStr);
            lastRefillTime = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;

            // Calculate tokens to add based on elapsed time
            long elapsedMs = now - lastRefillTime;
            double elapsedSec = elapsedMs / 1000.0;
            double tokensToAdd = elapsedSec * refillRatePerSec;

            currentTokens = Math.min(currentTokens + tokensToAdd, capacity);
        }

        // Check availability
        boolean allowed = currentTokens >= 1.0;
        long remainingTokens = (long) currentTokens;

        if (allowed) {
            // RACE CONDITION: Non-atomic SET operations
            // Multiple threads can reach here with the same currentTokens value
            currentTokens -= 1.0;
            remainingTokens = (long) currentTokens;

            stringRedisTemplate.opsForValue().set(tokenKey, String.valueOf(currentTokens));
            stringRedisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now));

            logger.debug("Request allowed. Key: {}, Tokens remaining: {}", key, remainingTokens);
        } else {
            logger.debug("Request rejected. Key: {}, Current tokens: {}", key, currentTokens);
        }

        return new RateLimitResult(allowed, remainingTokens);
    }
}

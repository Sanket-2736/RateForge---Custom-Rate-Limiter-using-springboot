package com.backend.rate_limiter.algorithms;

import com.backend.rate_limiter.dto.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Deprecated(forRemoval = true, since = "1.1.0")
public class TokenBucketLimiter {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucketLimiter.class);
    private static final String TOKEN_KEY_SUFFIX = ":tokens";
    private static final String LAST_REFILL_KEY_SUFFIX = ":lastRefill";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Deprecated(forRemoval = true, since = "1.1.0")
    public RateLimitResult checkAndConsume(String key, int capacity, double refillRatePerSec) {
        long now = System.currentTimeMillis();
        String tokenKey = key + TOKEN_KEY_SUFFIX;
        String lastRefillKey = key + LAST_REFILL_KEY_SUFFIX;

        String tokenCountStr = stringRedisTemplate.opsForValue().get(tokenKey);
        String lastRefillStr = stringRedisTemplate.opsForValue().get(lastRefillKey);

        double currentTokens;
        long lastRefillTime;

        if (tokenCountStr == null) {
            currentTokens = capacity;
            lastRefillTime = now;
        } else {
            currentTokens = Double.parseDouble(tokenCountStr);
            lastRefillTime = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;
            
            long elapsedMs = now - lastRefillTime;
            double elapsedSec = elapsedMs / 1000.0;
            double tokensToAdd = elapsedSec * refillRatePerSec;

            currentTokens = Math.min(currentTokens + tokensToAdd, capacity);
        }

        boolean allowed = currentTokens >= 1.0;
        long remainingTokens = (long) currentTokens;

        if (allowed) {
            
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

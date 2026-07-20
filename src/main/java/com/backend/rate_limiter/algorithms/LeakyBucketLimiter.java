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

@Component
public class LeakyBucketLimiter {
    private static final Logger logger = LoggerFactory.getLogger(LeakyBucketLimiter.class);
    private static final String QUEUE_SIZE_KEY_SUFFIX = ":queue";
    private static final String LAST_LEAK_KEY_SUFFIX = ":lastLeak";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ScriptSource leakyBucketScriptSource;
    
    public RateLimitResult checkAndEnqueue(String key, int capacity, double leakRatePerSec, long now) {
        String queueKey = key + QUEUE_SIZE_KEY_SUFFIX;
        String lastLeakKey = key + LAST_LEAK_KEY_SUFFIX;

        logger.debug("[LeakyBucket] Checking rate limit for key: {}, capacity: {}, leakRate: {}/sec", 
            key, capacity, leakRatePerSec);

        try {

            String scriptContent = leakyBucketScriptSource.getScriptAsString();
            logger.debug("[LeakyBucket] Lua script loaded, length: {} bytes", scriptContent.length());

            DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(scriptContent);
            redisScript.setResultType(List.class);
            
            logger.debug("[LeakyBucket] Created RedisScript with List return type");

            List<String> keys = new ArrayList<>();
            keys.add(queueKey);
            keys.add(lastLeakKey);
            
            logger.debug("[LeakyBucket] Keys: [{}, {}]", queueKey, lastLeakKey);
            logger.debug("[LeakyBucket] Arguments: [now={}, capacity={}, leakRate={}]", 
                now, capacity, leakRatePerSec);

            List result = stringRedisTemplate.execute(
                redisScript,
                keys,
                String.valueOf(now),
                String.valueOf(capacity),
                String.valueOf(leakRatePerSec)
            );

            logger.debug("[LeakyBucket] Script execution completed");

            if (result == null) {
                logger.error("[LeakyBucket] Redis returned null result");
                return new RateLimitResult(false, 0);
            }

            if (result.isEmpty()) {
                logger.error("[LeakyBucket] Redis returned empty list");
                return new RateLimitResult(false, 0);
            }

            logger.debug("[LeakyBucket] Lua script returned list with {} elements", result.size());

            if (result.size() < 2) {
                logger.error("[LeakyBucket] Expected list with 2+ elements, got {}", result.size());
                for (int i = 0; i < result.size(); i++) {
                    logger.error("[LeakyBucket]   Element[{}]: {} ({})", 
                        i, result.get(i), result.get(i) == null ? "null" : result.get(i).getClass().getSimpleName());
                }
                return new RateLimitResult(false, 0);
            }

            try {
                Object allowedObj = result.get(0);
                Object queueSizeObj = result.get(1);

                logger.debug("[LeakyBucket] Result[0]: {} (type: {})", 
                    allowedObj, allowedObj == null ? "null" : allowedObj.getClass().getSimpleName());
                logger.debug("[LeakyBucket] Result[1]: {} (type: {})", 
                    queueSizeObj, queueSizeObj == null ? "null" : queueSizeObj.getClass().getSimpleName());

                long allowed = convertToLong(allowedObj);
                long queueSize = convertToLong(queueSizeObj);

                boolean requestAllowed = (allowed == 1);

                logger.info("[LeakyBucket] DECISION: allowed={}, queueSize={}, key={}", 
                    requestAllowed, queueSize, key);

                return new RateLimitResult(requestAllowed, queueSize);
            } catch (Exception e) {
                logger.error("[LeakyBucket] Failed to parse result elements: {}", e.getMessage());
                logger.error("[LeakyBucket] Result[0]: {} (type: {})", 
                    result.get(0), result.get(0) == null ? "null" : result.get(0).getClass().getSimpleName());
                logger.error("[LeakyBucket] Result[1]: {} (type: {})", 
                    result.get(1), result.get(1) == null ? "null" : result.get(1).getClass().getSimpleName());
                logger.error("[LeakyBucket] Parsing exception: ", e);
                return new RateLimitResult(false, 0);
            }
        } catch (Exception e) {
            logger.error("[LeakyBucket] CRITICAL ERROR during script execution: {}", e.getMessage());
            logger.error("[LeakyBucket] Exception type: {}", e.getClass().getName());
            logger.error("[LeakyBucket] Exception: ", e);
            
            logger.warn("[LeakyBucket] FAIL-OPEN: allowing request due to error");
            return new RateLimitResult(true, 0);
        }
    }
    
    public RateLimitResult checkAndEnqueue(String key, int capacity, double leakRatePerSec) {
        return checkAndEnqueue(key, capacity, leakRatePerSec, System.currentTimeMillis());
    }
    
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
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
public class SlidingWindowLimiter {
    private static final Logger logger = LoggerFactory.getLogger(SlidingWindowLimiter.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ScriptSource slidingWindowScriptSource;

    public RateLimitResult checkAndRecord(String key, long windowSizeMs, int maxRequests, long now) {
        logger.debug("[SlidingWindow] Checking rate limit for key: {}, window: {}ms, max: {}", 
            key, windowSizeMs, maxRequests);

        try {
            // Load the Lua script from classpath
            String scriptContent = slidingWindowScriptSource.getScriptAsString();
            logger.debug("[SlidingWindow] Lua script loaded, length: {} bytes", scriptContent.length());

            DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(scriptContent);
            redisScript.setResultType(List.class);
            
            logger.debug("[SlidingWindow] Created RedisScript with List return type");

            List<String> keys = new ArrayList<>();
            keys.add(key);
            
            logger.debug("[SlidingWindow] Key: [{}]", key);
            logger.debug("[SlidingWindow] Arguments: [now={}, windowSize={}, maxRequests={}]", 
                now, windowSizeMs, maxRequests);

            List result = stringRedisTemplate.execute(
                redisScript,
                keys,
                String.valueOf(now),
                String.valueOf(windowSizeMs),
                String.valueOf(maxRequests)
            );

            logger.debug("[SlidingWindow] Script execution completed");

            if (result == null) {
                logger.error("[SlidingWindow] Redis returned null result");
                return new RateLimitResult(false, 0);
            }

            if (result.isEmpty()) {
                logger.error("[SlidingWindow] Redis returned empty list");
                return new RateLimitResult(false, 0);
            }

            logger.debug("[SlidingWindow] Lua script returned list with {} elements", result.size());

            if (result.size() < 2) {
                logger.error("[SlidingWindow] Expected list with 2+ elements, got {}", result.size());
                for (int i = 0; i < result.size(); i++) {
                    logger.error("[SlidingWindow]   Element[{}]: {} ({})", 
                        i, result.get(i), result.get(i) == null ? "null" : result.get(i).getClass().getSimpleName());
                }
                return new RateLimitResult(false, 0);
            }

            try {
                Object allowedObj = result.get(0);
                Object remainingObj = result.get(1);

                logger.debug("[SlidingWindow] Result[0]: {} (type: {})", 
                    allowedObj, allowedObj == null ? "null" : allowedObj.getClass().getSimpleName());
                logger.debug("[SlidingWindow] Result[1]: {} (type: {})", 
                    remainingObj, remainingObj == null ? "null" : remainingObj.getClass().getSimpleName());

                long allowed = convertToLong(allowedObj);
                long remainingRequests = convertToLong(remainingObj);

                boolean requestAllowed = (allowed == 1);

                logger.info("[SlidingWindow] DECISION: allowed={}, remaining={}, key={}", 
                    requestAllowed, remainingRequests, key);

                return new RateLimitResult(requestAllowed, remainingRequests);
            } catch (Exception e) {
                logger.error("[SlidingWindow] Failed to parse result elements: {}", e.getMessage());
                logger.error("[SlidingWindow] Result[0]: {} (type: {})", 
                    result.get(0), result.get(0) == null ? "null" : result.get(0).getClass().getSimpleName());
                logger.error("[SlidingWindow] Result[1]: {} (type: {})", 
                    result.get(1), result.get(1) == null ? "null" : result.get(1).getClass().getSimpleName());
                logger.error("[SlidingWindow] Parsing exception: ", e);
                return new RateLimitResult(false, 0);
            }
        } catch (Exception e) {
            logger.error("[SlidingWindow] CRITICAL ERROR during script execution: {}", e.getMessage());
            logger.error("[SlidingWindow] Exception type: {}", e.getClass().getName());
            logger.error("[SlidingWindow] Exception: ", e);
            logger.warn("[SlidingWindow] FAIL-OPEN: allowing request due to error");
            return new RateLimitResult(true, 0);
        }
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
    
    public RateLimitResult checkAndRecord(String key, long windowSizeMs, int maxRequests) {
        return checkAndRecord(key, windowSizeMs, maxRequests, System.currentTimeMillis());
    }
}

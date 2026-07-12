package com.backend.rate_limiter.controller;

import com.backend.rate_limiter.dto.HealthResponse;
import com.backend.rate_limiter.dto.RedisHealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        logger.info("Health check endpoint called");
        return ResponseEntity.ok(new HealthResponse("ok"));
    }

    @GetMapping("/health/redis")
    public ResponseEntity<RedisHealthResponse> redisHealth() {
        logger.info("Redis health check endpoint called");
        try {
            String pingResult = stringRedisTemplate.getConnectionFactory().getConnection().ping();
            logger.info("Redis PING successful: {}", pingResult);
            return ResponseEntity.ok(new RedisHealthResponse("ok", "Redis connection healthy", "connected"));
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new RedisHealthResponse("error", "Redis connection failed: " + e.getMessage(), "disconnected"));
        }
    }
}

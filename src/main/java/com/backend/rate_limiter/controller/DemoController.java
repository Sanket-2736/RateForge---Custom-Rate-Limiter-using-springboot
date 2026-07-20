package com.backend.rate_limiter.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/demo")
public class DemoController {
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/token-bucket")
    public ResponseEntity<Map<String, Object>> tokenBucketDemo(
            @RequestParam(value = "message", defaultValue = "Token bucket request") String message) {
        logger.info("Token bucket demo endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/demo/token-bucket");
        response.put("algorithm", "Token Bucket");
        response.put("description", "Accumulates tokens, allows bursts up to capacity");
        response.put("use_case", "APIs that tolerate occasional traffic spikes");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    @GetMapping("/sliding-window")
    public ResponseEntity<Map<String, Object>> slidingWindowDemo(
            @RequestParam(value = "message", defaultValue = "Sliding window request") String message) {
        logger.info("Sliding window demo endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/demo/sliding-window");
        response.put("algorithm", "Sliding Window");
        response.put("description", "Counts requests in a time window, rejects when limit exceeded");
        response.put("use_case", "Accurate per-window request counting (e.g., requests per minute)");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    @GetMapping("/leaky-bucket")
    public ResponseEntity<Map<String, Object>> leakyBucketDemo(
            @RequestParam(value = "message", defaultValue = "Leaky bucket request") String message) {
        logger.info("Leaky bucket demo endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("endpoint", "/demo/leaky-bucket");
        response.put("algorithm", "Leaky Bucket");
        response.put("description", "Queue that drains at constant rate, smooths traffic");
        response.put("use_case", "Protecting downstream systems from traffic bursts");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Info endpoint showing available demo endpoints and tier limits.
     * 
     * @return Endpoint info and tier limits
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        logger.info("Demo info endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("title", "Rateforge Demo - Rate Limiting Algorithms");
        response.put("description", "Test three rate limiting algorithms with different tiers");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("/demo/token-bucket", "Test token bucket algorithm");
        endpoints.put("/demo/sliding-window", "Test sliding window algorithm");
        endpoints.put("/demo/leaky-bucket", "Test leaky bucket algorithm");
        endpoints.put("/demo/info", "This endpoint");
        response.put("endpoints", endpoints);
        
        Map<String, Map<String, Object>> tiers = new HashMap<>();
        
        Map<String, Object> freeTier = new HashMap<>();
        freeTier.put("api_key", "demo-free");
        freeTier.put("limit", "100 requests/hour");
        freeTier.put("description", "Free tier with basic limits");
        tiers.put("free", freeTier);
        
        Map<String, Object> proTier = new HashMap<>();
        proTier.put("api_key", "demo-pro");
        proTier.put("limit", "1000 requests/hour");
        proTier.put("description", "Professional tier with higher limits");
        tiers.put("pro", proTier);
        
        Map<String, Object> enterpriseTier = new HashMap<>();
        enterpriseTier.put("api_key", "demo-enterprise");
        enterpriseTier.put("limit", "Effectively unlimited");
        enterpriseTier.put("description", "Enterprise tier with very high limits");
        tiers.put("enterprise", enterpriseTier);
        
        response.put("tiers", tiers);
        
        Map<String, String> examples = new HashMap<>();
        examples.put("token_bucket_pro", "curl -H \"X-API-Key: demo-pro\" http://localhost:3000/demo/token-bucket");
        examples.put("sliding_window_free", "curl -H \"X-API-Key: demo-free\" http://localhost:3000/demo/sliding-window");
        examples.put("leaky_bucket_enterprise", "curl -H \"X-API-Key: demo-enterprise\" http://localhost:3000/demo/leaky-bucket");
        response.put("curl_examples", examples);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-Key", "API key for tier identification (demo-free, demo-pro, demo-enterprise)");
        headers.put("X-RateLimit-Limit", "Maximum requests allowed");
        headers.put("X-RateLimit-Remaining", "Requests remaining in current window");
        headers.put("X-RateLimit-Reset", "Timestamp when limit resets");
        response.put("response_headers", headers);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}

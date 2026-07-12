package com.backend.rate_limiter.service;

import com.backend.rate_limiter.algorithms.LeakyBucketLimiter;
import com.backend.rate_limiter.algorithms.RateLimitAlgorithm;
import com.backend.rate_limiter.algorithms.SlidingWindowLimiter;
import com.backend.rate_limiter.algorithms.TokenBucketLimiterAtomic;
import com.backend.rate_limiter.dto.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Facade service for rate limiting using different algorithms.
 * 
 * Provides a unified interface to rate limiting regardless of the underlying
 * algorithm. Clients specify which algorithm to use and the service delegates
 * to the appropriate limiter implementation.
 * 
 * This pattern makes it easy to:
 * - Switch algorithms without changing client code
 * - Test different algorithms against the same key
 * - Support per-endpoint algorithm selection
 */
@Service
public class RateLimiterService {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    @Autowired
    private TokenBucketLimiterAtomic tokenBucketLimiter;

    @Autowired
    private SlidingWindowLimiter slidingWindowLimiter;

    @Autowired
    private LeakyBucketLimiter leakyBucketLimiter;

    /**
     * Check if a request should be rate limited based on the specified algorithm.
     * 
     * This method provides a unified interface across all rate limiting algorithms.
     * The algorithm determines how the rate limit is enforced:
     * 
     * - TOKEN_BUCKET: Allows bursts up to capacity, refills over time
     * - SLIDING_WINDOW: Counts requests in a time window, rejects when limit exceeded
     * - LEAKY_BUCKET: Queue drains at constant rate, smooths traffic
     * 
     * @param algorithm The rate limiting algorithm to use
     * @param key The rate limit key (e.g., user ID, API key, IP address)
     * @param capacity The rate limit capacity (tokens for bucket, max requests for window, queue size for leak)
     * @param rate The rate value (tokens/sec for bucket, window size ms for sliding, leak rate for bucket)
     * @return RateLimitResult indicating if the request is allowed
     * @throws IllegalArgumentException if algorithm is null or unsupported
     */
    public RateLimitResult checkRateLimit(RateLimitAlgorithm algorithm, String key, int capacity, double rate) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Rate limit algorithm cannot be null");
        }

        logger.debug("Rate limit check: algorithm={}, key={}, capacity={}, rate={}", 
            algorithm.getDisplayName(), key, capacity, rate);

        switch (algorithm) {
            case TOKEN_BUCKET:
                return tokenBucketLimiter.checkAndConsume(key, capacity, rate);

            case SLIDING_WINDOW:
                long windowSizeMs = (long) rate;
                return slidingWindowLimiter.checkAndRecord(key, windowSizeMs, capacity);

            case LEAKY_BUCKET:
                return leakyBucketLimiter.checkAndEnqueue(key, capacity, rate);

            default:
                throw new IllegalArgumentException("Unsupported rate limit algorithm: " + algorithm);
        }
    }

    /**
     * Check rate limit using TOKEN_BUCKET algorithm.
     * 
     * @param key The rate limit key
     * @param capacity Maximum tokens in the bucket
     * @param refillRatePerSec Token refill rate per second
     * @return RateLimitResult
     */
    public RateLimitResult checkTokenBucket(String key, int capacity, double refillRatePerSec) {
        return checkRateLimit(RateLimitAlgorithm.TOKEN_BUCKET, key, capacity, refillRatePerSec);
    }

    /**
     * Check rate limit using SLIDING_WINDOW algorithm.
     * 
     * @param key The rate limit key
     * @param windowSizeMs The sliding window size in milliseconds
     * @param maxRequests Maximum allowed requests in the window
     * @return RateLimitResult
     */
    public RateLimitResult checkSlidingWindow(String key, long windowSizeMs, int maxRequests) {
        return checkRateLimit(RateLimitAlgorithm.SLIDING_WINDOW, key, maxRequests, (double) windowSizeMs);
    }

    /**
     * Check rate limit using LEAKY_BUCKET algorithm.
     * 
     * @param key The rate limit key
     * @param capacity Maximum queue size (bucket capacity)
     * @param leakRatePerSec Rate at which requests drain from the queue
     * @return RateLimitResult
     */
    public RateLimitResult checkLeakyBucket(String key, int capacity, double leakRatePerSec) {
        return checkRateLimit(RateLimitAlgorithm.LEAKY_BUCKET, key, capacity, leakRatePerSec);
    }
}

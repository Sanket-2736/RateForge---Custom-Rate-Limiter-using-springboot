package com.backend.rate_limiter.algorithms;

/**
 * Enumeration of available rate limiting algorithms.
 */
public enum RateLimitAlgorithm {
    /**
     * Token Bucket: Accumulates tokens, allows bursts up to capacity.
     * Good for APIs that can handle occasional traffic spikes.
     */
    TOKEN_BUCKET("Token Bucket"),
    
    /**
     * Sliding Window: Tracks request timestamps in a time window.
     * Good for accurate request counting within a window.
     */
    SLIDING_WINDOW("Sliding Window"),
    
    /**
     * Leaky Bucket: Queue that drains at constant rate.
     * Good for smoothing bursty traffic into steady output.
     */
    LEAKY_BUCKET("Leaky Bucket");

    private final String displayName;

    RateLimitAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

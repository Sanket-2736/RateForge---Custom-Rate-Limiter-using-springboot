package com.backend.rate_limiter.algorithms;

/**
 * Enumeration of available rate limiting algorithms.
 */
public enum RateLimitAlgorithm {
    
    TOKEN_BUCKET("Token Bucket"),
    
    SLIDING_WINDOW("Sliding Window"),
    
    LEAKY_BUCKET("Leaky Bucket");

    private final String displayName;

    RateLimitAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

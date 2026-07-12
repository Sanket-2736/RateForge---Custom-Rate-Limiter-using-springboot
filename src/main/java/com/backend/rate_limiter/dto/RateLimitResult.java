package com.backend.rate_limiter.dto;

public record RateLimitResult(
    boolean allowed,
    long remainingTokens
) {
}

package com.backend.rate_limiter.filter;

import com.backend.rate_limiter.algorithms.RateLimitAlgorithm;
import com.backend.rate_limiter.config.RateLimitProperties;
import com.backend.rate_limiter.config.TierConfig;
import com.backend.rate_limiter.dto.RateLimitResult;
import com.backend.rate_limiter.service.RateLimiterService;
import com.backend.rate_limiter.service.UserTierService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP Filter for rate limiting.
 * 
 * Intercepts HTTP requests and enforces rate limits based on:
 * - Client identifier: extracted from X-API-Key header or falls back to client IP
 * - User tier: looked up via UserTierService based on API key
 * - Algorithm: Per-path algorithm pinning or globally configured default
 * - Tier limits: Loaded from TierConfig (free, pro, enterprise)
 * 
 * For allowed requests: sets response headers and continues the request chain.
 * For rejected requests: returns 429 Too Many Requests with JSON error body and Retry-After header.
 * 
 * Excluded paths (e.g., /health) bypass rate limiting.
 * 
 * Per-path algorithm mapping:
 * - /demo/token-bucket → TOKEN_BUCKET
 * - /demo/sliding-window → SLIDING_WINDOW
 * - /demo/leaky-bucket → LEAKY_BUCKET
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Per-path algorithm pinning
    private static final Map<String, RateLimitAlgorithm> PATH_ALGORITHM_MAP = new HashMap<>();

    static {
        PATH_ALGORITHM_MAP.put("/demo/token-bucket", RateLimitAlgorithm.TOKEN_BUCKET);
        PATH_ALGORITHM_MAP.put("/demo/sliding-window", RateLimitAlgorithm.SLIDING_WINDOW);
        PATH_ALGORITHM_MAP.put("/demo/leaky-bucket", RateLimitAlgorithm.LEAKY_BUCKET);
    }

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Autowired
    private TierConfig tierConfig;

    @Autowired
    private UserTierService userTierService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Check if rate limiting is enabled
        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if the path should be excluded from rate limiting
        if (isExcludedPath(request.getRequestURI())) {
            logger.debug("Path excluded from rate limiting: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract client identifier
            String apiKey = extractApiKey(request);
            String clientId = buildClientIdentifier(apiKey, request);
            logger.debug("Rate limit check for client: {}", clientId);

            // Look up user tier based on API key
            String tierName = userTierService.getTierForApiKey(apiKey);
            logger.debug("Client tier: {}", tierName);

            // Get tier configuration
            TierConfig.Tier tier = tierConfig.getTier(tierName);

            if (tier == null) {
                // No tier configured, allow request
                filterChain.doFilter(request, response);
                return;
            }

            // Determine algorithm: check for per-path pinning first, then use global default
            RateLimitAlgorithm algorithm = getAlgorithmForPath(request.getRequestURI());
            logger.debug("Using algorithm: {}", algorithm.getDisplayName());

            // Check rate limit
            RateLimitResult result = applyRateLimit(algorithm, clientId, tier);

            // Set rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(tier.getCapacity()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
            
            // Calculate Retry-After (when the next request would be allowed)
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 1000));

            if (result.allowed()) {
                // Request allowed, continue
                logger.debug("Request allowed for client: {}, tier: {}, remaining: {}", 
                    clientId, tierName, result.remainingTokens());
                filterChain.doFilter(request, response);
            } else {
                // Request rejected
                logger.warn("Request rejected for client: {}, tier: {} (rate limit exceeded)", 
                    clientId, tierName);
                
                // Set Retry-After header (in seconds)
                response.setHeader("Retry-After", "1");
                
                // Return 429 Too Many Requests
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("status", 429);
                errorBody.put("error", "Too Many Requests");
                errorBody.put("message", "Rate limit exceeded for tier: " + tierName);
                errorBody.put("tier", tierName);
                errorBody.put("remaining", result.remainingTokens());
                errorBody.put("limit", tier.getCapacity());

                response.getWriter().write(objectMapper.writeValueAsString(errorBody));
                response.getWriter().flush();
            }
        } catch (Exception e) {
            logger.error("Error in rate limit filter", e);
            // Fail open: allow the request if an error occurs
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extract API key from request.
     * 
     * Checks X-API-Key header (or configured header name).
     * 
     * @param request The HTTP request
     * @return API key or null if not present
     */
    private String extractApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader(rateLimitProperties.getHeaderName());
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        return null;
    }

    /**
     * Build a unique client identifier.
     * 
     * If API key is present, use it with "api-key:" prefix.
     * Otherwise, use client IP with "ip:" prefix.
     * 
     * @param apiKey The API key (may be null)
     * @param request The HTTP request
     * @return Client identifier
     */
    private String buildClientIdentifier(String apiKey, HttpServletRequest request) {
        if (apiKey != null && !apiKey.isEmpty()) {
            return "api-key:" + apiKey;
        }
        return "ip:" + getClientIp(request);
    }

    /**
     * Get the algorithm for a specific path.
     * 
     * Checks per-path pinning map first, then falls back to global algorithm setting.
     * 
     * @param path The request path
     * @return The algorithm to use for this path
     */
    private RateLimitAlgorithm getAlgorithmForPath(String path) {
        // Check for exact path match
        if (PATH_ALGORITHM_MAP.containsKey(path)) {
            return PATH_ALGORITHM_MAP.get(path);
        }

        // Fall back to global algorithm
        try {
            return RateLimitAlgorithm.valueOf(rateLimitProperties.getAlgorithm());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid algorithm in config: {}, defaulting to TOKEN_BUCKET", 
                rateLimitProperties.getAlgorithm());
            return RateLimitAlgorithm.TOKEN_BUCKET;
        }
    }

    /**
     * Apply rate limiting based on algorithm and tier.
     * 
     * @param algorithm The algorithm to use
     * @param clientId The client identifier
     * @param tier The tier configuration
     * @return RateLimitResult
     */
    private RateLimitResult applyRateLimit(RateLimitAlgorithm algorithm, String clientId, TierConfig.Tier tier) {
        switch (algorithm) {
            case TOKEN_BUCKET:
                // For token bucket: use rate (tokens/sec) = capacity/hour converted to per second
                // 100 req/hour = 100/(60*60) = 0.0277... tokens/sec
                double tokensPerSec = tier.getCapacity() / 3600.0;
                return rateLimiterService.checkTokenBucket(clientId, tier.getCapacity(), tokensPerSec);

            case SLIDING_WINDOW:
                // For sliding window: window size in ms, max requests in tier.capacity
                return rateLimiterService.checkSlidingWindow(clientId, tier.getWindowSizeMs(), tier.getCapacity());

            case LEAKY_BUCKET:
                // For leaky bucket: capacity and leak rate
                // 100 req/hour = 100/3600 = 0.0277... req/sec leak rate
                double leakRatePerSec = tier.getCapacity() / 3600.0;
                return rateLimiterService.checkLeakyBucket(clientId, tier.getCapacity(), leakRatePerSec);

            default:
                logger.warn("Unknown algorithm: {}, defaulting to TOKEN_BUCKET", algorithm);
                return rateLimiterService.checkTokenBucket(clientId, tier.getCapacity(), tier.getCapacity() / 3600.0);
        }
    }

    /**
     * Get the client IP address from the request.
     * 
     * Handles:
     * - Direct connection
     * - X-Forwarded-For header (proxy/load balancer)
     * - X-Real-IP header
     * 
     * @param request The HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        // Check for X-Forwarded-For header (proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        // Check for X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Check if the request path should be excluded from rate limiting.
     * 
     * @param path The request path
     * @return True if the path is excluded
     */
    private boolean isExcludedPath(String path) {
        String[] excludedPaths = rateLimitProperties.getExcludePaths().split(",");
        return Arrays.stream(excludedPaths)
                .anyMatch(excludedPath -> path.startsWith(excludedPath.trim()));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Don't filter OPTIONS requests
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}

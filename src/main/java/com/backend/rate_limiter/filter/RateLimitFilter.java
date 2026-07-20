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

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isExcludedPath(request.getRequestURI())) {
            logger.debug("Path excluded from rate limiting: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String apiKey = extractApiKey(request);
            String clientId = buildClientIdentifier(apiKey, request);
            logger.debug("Rate limit check for client: {}", clientId);

            String tierName = userTierService.getTierForApiKey(apiKey);
            logger.debug("Client tier: {}", tierName);

            TierConfig.Tier tier = tierConfig.getTier(tierName);

            if (tier == null) {
                // No tier configured, allow request
                filterChain.doFilter(request, response);
                return;
            }

            RateLimitAlgorithm algorithm = getAlgorithmForPath(request.getRequestURI());
            logger.debug("Using algorithm: {}", algorithm.getDisplayName());

            RateLimitResult result = applyRateLimit(algorithm, clientId, tier);

            response.setHeader("X-RateLimit-Limit", String.valueOf(tier.getCapacity()));

            long remainingCapacity = result.remainingTokens();
            if (algorithm == RateLimitAlgorithm.LEAKY_BUCKET) {

                remainingCapacity = tier.getCapacity() - result.remainingTokens();
            }
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remainingCapacity));
            
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 1000));

            if (result.allowed()) {
                logger.debug("Request allowed for client: {}, tier: {}, remaining: {}", 
                    clientId, tierName, result.remainingTokens());
                filterChain.doFilter(request, response);
            } else {
                logger.warn("Request rejected for client: {}, tier: {} (rate limit exceeded)", 
                    clientId, tierName);
                
                response.setHeader("Retry-After", "1");
                
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
                return;
            }
        } catch (Exception e) {
            logger.error("Error in rate limit filter", e);
            // Fail open: allow the request if an error occurs
            filterChain.doFilter(request, response);
        }
    }
    
    private String extractApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader(rateLimitProperties.getHeaderName());
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        return null;
    }
    
    private String buildClientIdentifier(String apiKey, HttpServletRequest request) {
        if (apiKey != null && !apiKey.isEmpty()) {
            return "api-key:" + apiKey;
        }
        return "ip:" + getClientIp(request);
    }
    
    private RateLimitAlgorithm getAlgorithmForPath(String path) {
        // Check for exact path match
        if (PATH_ALGORITHM_MAP.containsKey(path)) {
            return PATH_ALGORITHM_MAP.get(path);
        }

        try {
            return RateLimitAlgorithm.valueOf(rateLimitProperties.getAlgorithm());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid algorithm in config: {}, defaulting to TOKEN_BUCKET", 
                rateLimitProperties.getAlgorithm());
            return RateLimitAlgorithm.TOKEN_BUCKET;
        }
    }
    
    private RateLimitResult applyRateLimit(RateLimitAlgorithm algorithm, String clientId, TierConfig.Tier tier) {
        switch (algorithm) {
            case TOKEN_BUCKET:
                double tokensPerSec = tier.getCapacity() / 3600.0;
                return rateLimiterService.checkTokenBucket(clientId, tier.getCapacity(), tokensPerSec);

            case SLIDING_WINDOW:
                return rateLimiterService.checkSlidingWindow(clientId, tier.getWindowSizeMs(), tier.getCapacity());

            case LEAKY_BUCKET:
                double leakRatePerSec = tier.getCapacity() / 3600.0;
                return rateLimiterService.checkLeakyBucket(clientId, tier.getCapacity(), leakRatePerSec);

            default:
                logger.warn("Unknown algorithm: {}, defaulting to TOKEN_BUCKET", algorithm);
                return rateLimiterService.checkTokenBucket(clientId, tier.getCapacity(), tier.getCapacity() / 3600.0);
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        // Check for X-Forwarded-For header (proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private boolean isExcludedPath(String path) {
        String[] excludedPaths = rateLimitProperties.getExcludePaths().split(",");
        return Arrays.stream(excludedPaths)
                .anyMatch(excludedPath -> path.startsWith(excludedPath.trim()));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}

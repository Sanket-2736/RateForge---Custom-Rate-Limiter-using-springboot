package com.backend.rate_limiter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for looking up user tier based on API key.
 * 
 * Currently uses a hardcoded map for demonstration purposes.
 * In production, this would query a database or user service.
 * 
 * Tier levels:
 * - FREE: Basic tier, 100 req/hour
 * - PRO: Professional tier, 1000 req/hour
 * - ENTERPRISE: Enterprise tier, effectively unlimited
 */
@Service
public class UserTierService {
    private static final Logger logger = LoggerFactory.getLogger(UserTierService.class);

    // Hardcoded API key to tier mapping
    // In production, this would be loaded from a database
    private static final Map<String, String> API_KEY_TO_TIER = new HashMap<>();

    static {
        // Free tier users
        API_KEY_TO_TIER.put("free-key-1", "free");
        API_KEY_TO_TIER.put("free-key-2", "free");
        API_KEY_TO_TIER.put("demo-free", "free");

        // Pro tier users
        API_KEY_TO_TIER.put("pro-key-1", "pro");
        API_KEY_TO_TIER.put("pro-key-2", "pro");
        API_KEY_TO_TIER.put("demo-pro", "pro");

        // Enterprise tier users
        API_KEY_TO_TIER.put("enterprise-key-1", "enterprise");
        API_KEY_TO_TIER.put("enterprise-key-2", "enterprise");
        API_KEY_TO_TIER.put("demo-enterprise", "enterprise");

        // Demo keys for each algorithm
        API_KEY_TO_TIER.put("demo-token-bucket", "pro");
        API_KEY_TO_TIER.put("demo-sliding-window", "pro");
        API_KEY_TO_TIER.put("demo-leaky-bucket", "pro");
    }

    /**
     * Get the tier for a given API key.
     * 
     * @param apiKey The API key
     * @return The tier name (free, pro, enterprise), or "free" if not found
     */
    public String getTierForApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.debug("No API key provided, defaulting to free tier");
            return "free";
        }

        String tier = API_KEY_TO_TIER.getOrDefault(apiKey, "free");
        logger.debug("API key '{}' mapped to tier '{}'", apiKey, tier);
        return tier;
    }

    /**
     * Get the tier for a given IP address.
     * Currently, all IP addresses are mapped to free tier.
     * 
     * @param ipAddress The IP address
     * @return The tier name (always "free" for IP-based access)
     */
    public String getTierForIpAddress(String ipAddress) {
        logger.debug("IP address {} accessing as free tier", ipAddress);
        return "free";  // All IP-based access gets free tier
    }

    /**
     * Register an API key with a tier.
     * Used for testing/demo purposes.
     * 
     * @param apiKey The API key
     * @param tier The tier to assign
     */
    public void registerApiKey(String apiKey, String tier) {
        API_KEY_TO_TIER.put(apiKey, tier);
        logger.info("Registered API key '{}' with tier '{}'", apiKey, tier);
    }

    /**
     * Get all registered API keys and their tiers.
     * 
     * @return Map of API key to tier
     */
    public Map<String, String> getAllApiKeys() {
        return new HashMap<>(API_KEY_TO_TIER);
    }

    /**
     * Clear all registered API keys (for testing).
     */
    public void clearAllApiKeys() {
        API_KEY_TO_TIER.clear();
        logger.warn("Cleared all API key registrations");
    }
}

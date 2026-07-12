package com.backend.rate_limiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ratelimit.tiers.by-tier")
public class TierConfig {
    
    private Tier free = new Tier();
    private Tier pro = new Tier();
    private Tier enterprise = new Tier();

    /**
     * Represents a single tier's rate limit configuration.
     */
    public static class Tier {
        private int capacity = 100;      // Maximum requests in the window
        private long windowSizeMs = 3600000;  // 1 hour in milliseconds
        private double rate = 1.0;       // For token bucket: tokens/sec or for leaky: leak rate/sec

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public long getWindowSizeMs() {
            return windowSizeMs;
        }

        public void setWindowSizeMs(long windowSizeMs) {
            this.windowSizeMs = windowSizeMs;
        }

        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }

        @Override
        public String toString() {
            return "Tier{" +
                    "capacity=" + capacity +
                    ", windowSizeMs=" + windowSizeMs +
                    ", rate=" + rate +
                    '}';
        }
    }

    public Tier getFree() {
        return free;
    }

    public void setFree(Tier free) {
        this.free = free;
    }

    public Tier getPro() {
        return pro;
    }

    public void setPro(Tier pro) {
        this.pro = pro;
    }

    public Tier getEnterprise() {
        return enterprise;
    }

    public void setEnterprise(Tier enterprise) {
        this.enterprise = enterprise;
    }

    /**
     * Get tier configuration by tier name.
     * 
     * @param tierName The tier name (free, pro, enterprise)
     * @return Tier configuration, or free tier if not found
     */
    public Tier getTier(String tierName) {
        switch (tierName.toLowerCase()) {
            case "pro":
                return pro;
            case "enterprise":
                return enterprise;
            case "free":
            default:
                return free;
        }
    }

    /**
     * Get all tiers as a map.
     * 
     * @return Map of tier name to tier configuration
     */
    public Map<String, Tier> getAllTiers() {
        Map<String, Tier> tiers = new HashMap<>();
        tiers.put("free", free);
        tiers.put("pro", pro);
        tiers.put("enterprise", enterprise);
        return tiers;
    }
}

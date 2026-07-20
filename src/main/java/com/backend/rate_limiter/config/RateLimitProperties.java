package com.backend.rate_limiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {
    private boolean enabled = true;
    private String algorithm = "TOKEN_BUCKET";  // Default algorithm
    private Map<String, Tier> tiers = new HashMap<>();
    private String headerName = "X-API-Key";
    private String excludePaths = "/health,/actuator";

    public static class Tier {
        private int capacity;
        private double rate;  
        
        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Map<String, Tier> getTiers() {
        return tiers;
    }

    public void setTiers(Map<String, Tier> tiers) {
        this.tiers = tiers;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(String excludePaths) {
        this.excludePaths = excludePaths;
    }
}

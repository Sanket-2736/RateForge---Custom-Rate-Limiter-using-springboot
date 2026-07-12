package com.backend.rate_limiter.config;

import com.backend.rate_limiter.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web configuration for rate limiting filter registration.
 */
@Configuration
public class WebConfig {

    @Autowired
    private RateLimitFilter rateLimitFilter;

    /**
     * Register the RateLimitFilter as a servlet filter.
     * 
     * The filter is applied to all requests except those excluded via configuration.
     * 
     * @return FilterRegistrationBean for rate limit filter
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(rateLimitFilter);
        registrationBean.addUrlPatterns("/*");  // Apply to all URLs
        registrationBean.setOrder(1);  // Execute early in the filter chain
        registrationBean.setName("RateLimitFilter");
        return registrationBean;
    }
}

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
    
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(rateLimitFilter);
        registrationBean.addUrlPatterns("/*"); 
        registrationBean.setOrder(1);
        registrationBean.setName("RateLimitFilter");
        return registrationBean;
    }
}

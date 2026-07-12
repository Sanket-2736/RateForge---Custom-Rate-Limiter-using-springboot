package com.backend.rate_limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class RateLimiterApplication {
	private static final Logger logger = LoggerFactory.getLogger(RateLimiterApplication.class);

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(RateLimiterApplication.class, args);
		logger.info("===========================================");
		logger.info("Rateforge application started successfully");
		logger.info("Server running on port 3000");
		logger.info("Health endpoint: GET /health");
		logger.info("Redis health endpoint: GET /health/redis");
		logger.info("===========================================");
	}

}

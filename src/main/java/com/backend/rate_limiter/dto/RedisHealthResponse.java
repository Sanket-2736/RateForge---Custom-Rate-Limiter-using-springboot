package com.backend.rate_limiter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedisHealthResponse {
    private String status;
    private String message;
    private String redisStatus;

    public RedisHealthResponse(String status, String message, String redisStatus) {
        this.status = status;
        this.message = message;
        this.redisStatus = redisStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRedisStatus() {
        return redisStatus;
    }

    public void setRedisStatus(String redisStatus) {
        this.redisStatus = redisStatus;
    }
}

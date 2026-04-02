package com.RateLimiter.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTokenBucketService redisTokenBucketService;

    public boolean isAllowed(String clientId) {
        return redisTokenBucketService.isAllowed(clientId);
    }

    public long getAvailableTokens(String clientId) {
        return redisTokenBucketService.getAvailableTokens(clientId);
    }
}

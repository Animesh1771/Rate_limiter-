package com.RateLimiter.service;

import com.RateLimiter.config.RateLimiterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
@RequiredArgsConstructor
public class RedisTokenBucketService {

    private  final JedisPool jedisPool;
    private  final RateLimiterProperties rateLimiterProperties;

    private static final String TOKEN_KEY_PREFIX = "rate_limiter:token:";
    private static final String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";//static

    public boolean isAllowed(String clientId){
        String tokenkey= TOKEN_KEY_PREFIX+clientId;

        try(Jedis jedis = jedisPool.getResource()){
            refillToken(clientId,jedis);
            String tokenStr=jedis.get(tokenkey);

            long currentToken = tokenStr != null ? Long.parseLong(tokenStr) : rateLimiterProperties.getCapacity();
            if (currentToken<=0){
                return false;
            }
            long decremented = jedis.decr(tokenkey);
            return true;
        }
    }
    public long getAvailableTokens(String clientId) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        try (Jedis jedis = jedisPool.getResource()) {
            refillToken(clientId, jedis);
            String tokenStr = jedis.get(tokenKey);
            return tokenStr != null ? Long.parseLong(tokenStr) : rateLimiterProperties.getCapacity();

        }
    }

    public void refillToken(String clientId, Jedis jedis){
        String tokenKey= TOKEN_KEY_PREFIX+clientId;
        String lastRefillKey = LAST_REFILL_KEY_PREFIX+clientId;

        long now=System.currentTimeMillis();
        String lastRefillstr=jedis.get(lastRefillKey);

        if(lastRefillstr==null){
            jedis.set(tokenKey,String.valueOf(rateLimiterProperties.getCapacity()));
            jedis.set(lastRefillKey,String.valueOf(now));
            return;
        }

        long lastRefillTime =Long.parseLong(lastRefillstr);
        long elapsedTime = now-lastRefillTime;
        if(elapsedTime<=0){
            return;
        }
        long tokensToAdd = (elapsedTime * rateLimiterProperties.getCapacity()) /1000;
        if(tokensToAdd<=0){
            return;
        }
        String tokenStr=jedis.get(tokenKey);
        long currentTokens=tokenStr !=null?Long.parseLong(tokenStr):rateLimiterProperties.getCapacity();
        long newTokens=Math.min(rateLimiterProperties.getCapacity(),currentTokens+tokensToAdd);
        jedis.set(tokenKey,String.valueOf(newTokens));


    }




}

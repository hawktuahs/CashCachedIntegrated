package com.bt.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountOtpService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String PREFIX = "acc:otp:";
    private static final Duration TTL = Duration.ofMinutes(5);

    public String generateAndStore(String key) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String redisKey = PREFIX + key;
        try {
            stringRedisTemplate.opsForValue().set(redisKey, code, TTL);
            return code;
        } catch (Exception e) {
            log.warn("Failed to store OTP for key {}", key, e);
            return code;
        }
    }

    public boolean validate(String key, String code) {
        String redisKey = PREFIX + key;
        try {
            String stored = stringRedisTemplate.opsForValue().get(redisKey);
            boolean valid = stored != null && stored.equals(code);
            if (valid) {
                stringRedisTemplate.delete(redisKey);
            }
            return valid;
        } catch (Exception e) {
            log.warn("Failed to validate OTP for key {}", key, e);
            return false;
        }
    }

    public void remove(String key) {
        String redisKey = PREFIX + key;
        try {
            stringRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Failed to remove OTP for key {}", key, e);
        }
    }
}

package com._penLearning.Noddi.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    // 데이터 저장 (TTL 초 단위로 설정)
    public void setDataExpire(String key, String value, long durationSeconds) {
        Duration expireDuration = Duration.ofSeconds(durationSeconds);
        redisTemplate.opsForValue().set(key, value, expireDuration);
    }

    // 데이터 조회 (키에 해당하는 값을 조회 / 데이터가 없거나 만료되어 사라졌을 경우 null 반환)
    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 데이터 삭제
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    // Key 존재 여부 확인
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 인증번호 틀린 횟수 카운트
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }
}
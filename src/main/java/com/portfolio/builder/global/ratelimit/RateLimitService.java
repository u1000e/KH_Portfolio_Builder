package com.portfolio.builder.global.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Redis 기반 Rate Limiting 서비스
 * AI 평가 API 호출 횟수 제한
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final StringRedisTemplate redisTemplate;
    
    @Value("${rate-limit.evaluate.daily-limit:5}")
    private int dailyLimit;
    
    private static final String KEY_PREFIX = "rate:evaluate:";
    
    /**
     * 호출 허용 여부 확인 및 카운터 증가
     * @param memberId 회원 ID
     * @return 허용되면 true, 제한 초과면 false
     */
    public boolean isAllowed(Long memberId) {
        String key = buildKey(memberId);
        
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count == null) {
                log.warn("Redis increment returned null for key: {}", key);
                return true; // Redis 문제 시 허용 (fail-open)
            }
            
            // 첫 호출이면 TTL 설정 (자정까지)
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofDays(1));
            }
            
            boolean allowed = count <= dailyLimit;
            
            if (!allowed) {
                log.info("Rate limit exceeded - memberId: {}, count: {}, limit: {}", 
                    memberId, count, dailyLimit);
            }
            
            return allowed;
        } catch (Exception e) {
            log.error("Redis error, allowing request (fail-open): {}", e.getMessage());
            return true; // Redis 장애 시에도 서비스는 동작
        }
    }
    
    /**
     * 남은 호출 횟수 조회
     * @param memberId 회원 ID
     * @return 남은 횟수 (음수면 초과)
     */
    public int getRemainingCount(Long memberId) {
        String key = buildKey(memberId);
        
        try {
            String value = redisTemplate.opsForValue().get(key);
            int used = (value != null) ? Integer.parseInt(value) : 0;
            return Math.max(0, dailyLimit - used);
        } catch (Exception e) {
            log.error("Redis error while getting remaining count: {}", e.getMessage());
            return dailyLimit; // 에러 시 전체 횟수 반환
        }
    }
    
    /**
     * 현재 사용 횟수 조회
     * @param memberId 회원 ID
     * @return 사용한 횟수
     */
    public int getUsedCount(Long memberId) {
        String key = buildKey(memberId);
        
        try {
            String value = redisTemplate.opsForValue().get(key);
            return (value != null) ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            log.error("Redis error while getting used count: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Redis 키 생성
     * 형식: rate:evaluate:{memberId}:{날짜}
     */
    private String buildKey(Long memberId) {
        return KEY_PREFIX + memberId + ":" + LocalDate.now();
    }
}

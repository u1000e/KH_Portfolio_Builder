package com.portfolio.builder.til.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.builder.til.domain.TIL;
import com.portfolio.builder.til.domain.TILBooster;
import com.portfolio.builder.til.domain.TILBoosterRepository;
import com.portfolio.builder.til.dto.TILBoosterData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TILBoosterService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final TILBoosterRepository tilBoosterRepository;

    private static final String CACHE_PREFIX = "ai:til-booster:";
    private static final String RATE_PREFIX = "rate:til-booster:";
    private static final Duration CACHE_TTL = Duration.ofHours(72);

    @Value("${rate-limit.til-booster.daily-limit:10}")
    private int dailyLimit;

    @Transactional
    public TILBoosterData generateAndSave(TIL til) {
        try {
            Long memberId = til.getMember().getId();

            // Rate limit 체크
            if (isRateLimited(memberId)) {
                log.warn("TIL Booster rate limit exceeded for member: {}", memberId);
                return null;
            }

            // 캐시 확인
            String contentHash = generateContentHash(til);
            String cacheKey = CACHE_PREFIX + contentHash;
            String cached = getCached(cacheKey);
            if (cached != null) {
                log.info("TIL Booster cache hit: {}", cacheKey);
                TILBoosterData data = objectMapper.readValue(cached, TILBoosterData.class);
                saveBooster(til, cached);
                return data;
            }

            // AI 호출
            String promptText = buildPrompt(til);
            Prompt prompt = new Prompt(new UserMessage(promptText));
            String response = chatClient.prompt(prompt).call().content();
            log.debug("TIL Booster AI response: {}", response);

            // JSON 파싱
            String json = extractJson(response);
            TILBoosterData data = objectMapper.readValue(json, TILBoosterData.class);

            // 검증
            if (data.getSupplements() == null || data.getSelfCheckQuestions() == null
                    || data.getCoreKeywords() == null || data.getRelatedKeywords() == null) {
                log.warn("TIL Booster incomplete response");
                return null;
            }

            // 캐시 저장
            String dataJson = objectMapper.writeValueAsString(data);
            setCache(cacheKey, dataJson);

            // DB 저장
            saveBooster(til, dataJson);

            // Rate limit 카운트 증가
            incrementRateLimit(memberId);

            return data;
        } catch (Exception e) {
            log.error("TIL Booster 생성 실패 (tilId: {}): {}", til.getId(), e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public TILBoosterData getBoosterData(Long tilId) {
        return tilBoosterRepository.findByTilId(tilId)
                .map(this::parseBooster)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, TILBoosterData> getBoosterDataBatch(List<Long> tilIds) {
        if (tilIds == null || tilIds.isEmpty()) {
            return Map.of();
        }
        return tilBoosterRepository.findByTilIdIn(tilIds).stream()
                .collect(Collectors.toMap(
                        booster -> booster.getTil().getId(),
                        this::parseBooster,
                        (a, b) -> a
                ));
    }

    private TILBoosterData parseBooster(TILBooster booster) {
        try {
            return objectMapper.readValue(booster.getFeedbackJson(), TILBoosterData.class);
        } catch (Exception e) {
            log.warn("TIL Booster JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private void saveBooster(TIL til, String feedbackJson) {
        TILBooster existing = tilBoosterRepository.findByTilId(til.getId()).orElse(null);
        if (existing != null) {
            existing.setFeedbackJson(feedbackJson);
        } else {
            TILBooster booster = TILBooster.builder()
                    .til(til)
                    .feedbackJson(feedbackJson)
                    .build();
            tilBoosterRepository.save(booster);
        }
    }

    private String buildPrompt(TIL til) {
        String description = til.getDescription() != null ? truncate(til.getDescription(), 500) : "없음";
        String codeSnippet = til.getCodeSnippet() != null ? truncate(til.getCodeSnippet(), 300) : "없음";
        String tags = til.getTags() != null ? til.getTags() : "없음";

        return "당신은 친근한 개발 학습 도우미입니다. 학생의 TIL을 읽고 학습을 돕는 피드백을 생성합니다.\n\n"
                + "## 절대 금지\n"
                + "- \"부족합니다\", \"잘못\", \"틀렸\" 등 부정적 표현\n"
                + "- \"~해야 합니다\" 명령형\n"
                + "- 대신 \"~도 알아보면 좋아요\", \"~을 함께 살펴보면 도움이 될 거예요\" 사용\n\n"
                + "## 학생의 TIL\n"
                + "- 배운 것: " + til.getTitle() + "\n"
                + "- 어려웠던 것: " + til.getDifficulty() + "\n"
                + "- 메모: " + description + "\n"
                + "- 코드: " + codeSnippet + "\n"
                + "- 태그: " + tags + "\n\n"
                + "## 응답 (JSON만 출력)\n"
                + "{\n"
                + "  \"supplements\": [\"보충 설명 1~2문장\", \"보충 설명 1~2문장\"],\n"
                + "  \"selfCheckQuestions\": [\"셀프 체크 질문\", \"셀프 체크 질문\"],\n"
                + "  \"coreKeywords\": [\"핵심1\", \"핵심2\", \"핵심3\"],\n"
                + "  \"relatedKeywords\": [\"연관1\", \"연관2\", \"연관3\"]\n"
                + "}";
    }

    private String extractJson(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private String generateContentHash(TIL til) {
        String content = til.getTitle() + til.getDifficulty()
                + (til.getDescription() != null ? til.getDescription() : "")
                + (til.getCodeSnippet() != null ? til.getCodeSnippet() : "");
        return String.valueOf(content.hashCode());
    }

    private boolean isRateLimited(Long memberId) {
        try {
            String key = RATE_PREFIX + memberId + ":" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String count = redisTemplate.opsForValue().get(key);
            return count != null && Integer.parseInt(count) >= dailyLimit;
        } catch (Exception e) {
            log.warn("Rate limit check failed: {}", e.getMessage());
            return false;
        }
    }

    private void incrementRateLimit(Long memberId) {
        try {
            String key = RATE_PREFIX + memberId + ":" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofHours(36));
        } catch (Exception e) {
            log.warn("Rate limit increment failed: {}", e.getMessage());
        }
    }

    private String getCached(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis cache get failed: {}", e.getMessage());
            return null;
        }
    }

    private void setCache(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis cache set failed: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}

package com.portfolio.builder.ai.application;

import com.portfolio.builder.ai.dto.PortfolioData;
import com.portfolio.builder.portfolio.domain.Troubleshooting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * GPT-4o-mini 기반 표현력 평가기
 * - 자기소개 품질 평가
 * - 프로젝트 설명 품질 평가
 * - 트러블슈팅 품질 평가
 * - 24시간 캐싱으로 비용 최적화
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiExpressionEvaluator {
    
    private final ChatClient.Builder chatClientBuilder;
    private final StringRedisTemplate redisTemplate;
    
    private static final String CACHE_PREFIX = "ai:expression:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    
    /**
     * AI 표현력 평가 실행
     * @return 0~20점 사이의 점수
     */
    public ExpressionResult evaluate(PortfolioData data, List<Troubleshooting> troubleshootings) {
        // 1. 평가할 텍스트 준비
        String contentHash = generateContentHash(data, troubleshootings);
        String cacheKey = CACHE_PREFIX + contentHash;
        
        // 2. 캐시 확인
        String cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("AI expression evaluation cache hit: {}", cacheKey);
            return parseResult(cachedResult);
        }
        
        // 3. AI 평가 실행
        log.info("AI expression evaluation - calling GPT-4o-mini");
        ExpressionResult result = callAiEvaluation(data, troubleshootings);
        
        // 4. 결과 캐싱
        cacheResult(cacheKey, result);
        
        return result;
    }
    
    /**
     * GPT-4o-mini로 표현력 평가
     */
    private ExpressionResult callAiEvaluation(PortfolioData data, List<Troubleshooting> troubleshootings) {
        try {
            String prompt = buildPrompt(data, troubleshootings);
            
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            return parseAiResponse(response);
        } catch (Exception e) {
            log.error("AI expression evaluation failed: {}", e.getMessage());
            // 실패 시 기본 점수 반환
            return new ExpressionResult(10, "AI 평가를 수행할 수 없습니다.");
        }
    }
    
    /**
     * 평가 프롬프트 생성 (토큰 최적화)
     */
    private String buildPrompt(PortfolioData data, List<Troubleshooting> troubleshootings) {
        StringBuilder sb = new StringBuilder();
        sb.append("포트폴리오 표현력을 0~20점으로 평가해주세요.\n\n");
        
        // 자기소개 (최대 500자)
        if (data.getIntroduction() != null) {
            String intro = data.getIntroduction();
            if (intro.length() > 500) intro = intro.substring(0, 500) + "...";
            sb.append("[자기소개]\n").append(intro).append("\n\n");
        }
        
        // 프로젝트 설명 (최대 2개, 각 200자)
        if (data.getProjects() != null && !data.getProjects().isEmpty()) {
            sb.append("[프로젝트]\n");
            int count = 0;
            for (PortfolioData.ProjectData project : data.getProjects()) {
                if (count >= 2) break;
                String desc = project.getDescription();
                if (desc != null) {
                    if (desc.length() > 200) desc = desc.substring(0, 200) + "...";
                    sb.append("- ").append(project.getName()).append(": ").append(desc).append("\n");
                    count++;
                }
            }
            sb.append("\n");
        }
        
        // 트러블슈팅 (최대 1개, 요약)
        if (troubleshootings != null && !troubleshootings.isEmpty()) {
            Troubleshooting t = troubleshootings.get(0);
            sb.append("[트러블슈팅 예시]\n");
            sb.append("문제: ").append(truncate(t.getProblem(), 100)).append("\n");
            sb.append("해결: ").append(truncate(t.getSolution(), 100)).append("\n\n");
        }
        
        sb.append("평가 기준:\n");
        sb.append("- 구체성: 추상적 표현 vs 구체적 경험/수치\n");
        sb.append("- 명확성: 문장이 명확하고 읽기 쉬운지\n");
        sb.append("- 전문성: 기술 용어 적절히 사용하는지\n\n");
        sb.append("응답 형식 (JSON):\n");
        sb.append("{\"score\": 0~20, \"feedback\": \"한줄 피드백\"}");
        
        return sb.toString();
    }
    
    /**
     * AI 응답 파싱
     */
    private ExpressionResult parseAiResponse(String response) {
        try {
            // JSON 파싱 시도
            response = response.trim();
            if (response.startsWith("```")) {
                response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }
            
            // 간단한 JSON 파싱
            int scoreStart = response.indexOf("\"score\"");
            int scoreEnd = response.indexOf(",", scoreStart);
            if (scoreEnd == -1) scoreEnd = response.indexOf("}", scoreStart);
            
            String scoreStr = response.substring(scoreStart, scoreEnd)
                .replaceAll("[^0-9]", "");
            int score = Integer.parseInt(scoreStr);
            score = Math.min(20, Math.max(0, score)); // 0~20 범위로 제한
            
            // 피드백 추출
            int feedbackStart = response.indexOf("\"feedback\"");
            String feedback = "";
            if (feedbackStart != -1) {
                int colonPos = response.indexOf(":", feedbackStart);
                int quoteStart = response.indexOf("\"", colonPos + 1);
                int quoteEnd = response.indexOf("\"", quoteStart + 1);
                if (quoteStart != -1 && quoteEnd != -1) {
                    feedback = response.substring(quoteStart + 1, quoteEnd);
                }
            }
            
            return new ExpressionResult(score, feedback);
        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", response);
            return new ExpressionResult(10, "표현력 평가가 완료되었습니다.");
        }
    }
    
    /**
     * 컨텐츠 해시 생성 (캐싱용)
     */
    private String generateContentHash(PortfolioData data, List<Troubleshooting> troubleshootings) {
        StringBuilder sb = new StringBuilder();
        if (data.getIntroduction() != null) sb.append(data.getIntroduction());
        if (data.getProjects() != null) {
            data.getProjects().forEach(p -> {
                if (p.getDescription() != null) sb.append(p.getDescription());
            });
        }
        if (troubleshootings != null) {
            troubleshootings.forEach(t -> {
                sb.append(t.getProblem()).append(t.getSolution());
            });
        }
        return String.valueOf(sb.toString().hashCode());
    }
    
    /**
     * 캐시에서 결과 조회
     */
    private String getCachedResult(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis cache get failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 결과 캐싱
     */
    private void cacheResult(String key, ExpressionResult result) {
        try {
            String value = result.score() + "|" + result.feedback();
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis cache set failed: {}", e.getMessage());
        }
    }
    
    /**
     * 캐시된 결과 파싱
     */
    private ExpressionResult parseResult(String cached) {
        try {
            String[] parts = cached.split("\\|", 2);
            int score = Integer.parseInt(parts[0]);
            String feedback = parts.length > 1 ? parts[1] : "";
            return new ExpressionResult(score, feedback);
        } catch (Exception e) {
            return new ExpressionResult(10, "");
        }
    }
    
    /**
     * 문자열 자르기
     */
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
    
    /**
     * 표현력 평가 결과
     */
    public record ExpressionResult(int score, String feedback) {}
}

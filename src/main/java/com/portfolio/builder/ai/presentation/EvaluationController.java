package com.portfolio.builder.ai.presentation;

import com.portfolio.builder.ai.application.PortfolioEvaluationService;
import com.portfolio.builder.ai.dto.EvaluationResponse;
import com.portfolio.builder.global.ratelimit.RateLimitExceededResponse;
import com.portfolio.builder.global.ratelimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {
    
    private final PortfolioEvaluationService evaluationService;
    private final RateLimitService rateLimitService;
    
    /**
     * 포트폴리오 AI 평가 요청
     * POST /api/portfolios/{id}/evaluate
     * Rate Limit: 1일 5회
     */
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<?> evaluate(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal Long memberId) {
        
        // Rate Limit 체크
        if (!rateLimitService.isAllowed(memberId)) {
            log.warn("Rate limit exceeded - memberId: {}, portfolioId: {}", memberId, id);
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(RateLimitExceededResponse.builder()
                    .message("일일 AI 평가 횟수(5회)를 초과했습니다. 내일 다시 시도해주세요.")
                    .dailyLimit(5)
                    .used(5)
                    .remaining(0)
                    .build());
        }
        
        log.info("Portfolio evaluation requested - portfolioId: {}, memberId: {}, remaining: {}", 
            id, memberId, rateLimitService.getRemainingCount(memberId));
        
        EvaluationResponse response = evaluationService.evaluate(id, memberId);
        
        log.info("Portfolio evaluation completed - portfolioId: {}, totalScore: {}", id, response.getTotalScore());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 남은 AI 평가 횟수 조회
     * GET /api/portfolios/evaluate/remaining
     */
    @GetMapping("/evaluate/remaining")
    public ResponseEntity<?> getRemainingCount(@AuthenticationPrincipal Long memberId) {
        int remaining = rateLimitService.getRemainingCount(memberId);
        int used = rateLimitService.getUsedCount(memberId);
        
        return ResponseEntity.ok(RateLimitExceededResponse.builder()
            .message(remaining > 0 ? "평가 가능합니다" : "일일 한도를 초과했습니다")
            .dailyLimit(5)
            .used(used)
            .remaining(remaining)
            .build());
    }
}

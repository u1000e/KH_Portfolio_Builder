package com.portfolio.builder.ai.presentation;

import com.portfolio.builder.ai.application.PortfolioEvaluationService;
import com.portfolio.builder.ai.dto.EvaluationResponse;
import com.portfolio.builder.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {
    
    private final PortfolioEvaluationService evaluationService;
    
    /**
     * 포트폴리오 AI 평가 요청
     * POST /api/portfolios/{id}/evaluate
     */
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<EvaluationResponse> evaluate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("Portfolio evaluation requested - portfolioId: {}, memberId: {}", id, userDetails.getMemberId());
        
        EvaluationResponse response = evaluationService.evaluate(id, userDetails.getMemberId());
        
        log.info("Portfolio evaluation completed - portfolioId: {}, totalScore: {}", id, response.getTotalScore());
        
        return ResponseEntity.ok(response);
    }
}

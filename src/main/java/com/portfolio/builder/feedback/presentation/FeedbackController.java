package com.portfolio.builder.feedback.presentation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.builder.feedback.application.FeedbackService;
import com.portfolio.builder.feedback.dto.FeedbackRequest;
import com.portfolio.builder.feedback.dto.FeedbackResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 강사/직원의 포트폴리오 피드백 API
 */
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 피드백 작성 (직원/강사만)
     * POST /api/feedbacks/portfolio/{portfolioId}
     */
    @PostMapping("/portfolio/{portfolioId}")
    public ResponseEntity<FeedbackResponse> createFeedback(
            @PathVariable("portfolioId") Long portfolioId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody FeedbackRequest request) {
        
        log.info("Creating feedback - portfolioId: {}, memberId: {}", portfolioId, memberId);
        return ResponseEntity.ok(feedbackService.createFeedback(portfolioId, memberId, request));
    }

    /**
     * 피드백 목록 조회 (소유자/직원/강사/관리자)
     * GET /api/feedbacks/portfolio/{portfolioId}
     */
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacks(
            @PathVariable("portfolioId") Long portfolioId,
            @AuthenticationPrincipal Long memberId) {
        
        return ResponseEntity.ok(feedbackService.getFeedbacks(portfolioId, memberId));
    }

    /**
     * 피드백 개수 조회 (인증 불필요)
     * GET /api/feedbacks/portfolio/{portfolioId}/count
     */
    @GetMapping("/portfolio/{portfolioId}/count")
    public ResponseEntity<Long> getFeedbackCount(
            @PathVariable("portfolioId") Long portfolioId) {
        
        return ResponseEntity.ok(feedbackService.getFeedbackCount(portfolioId));
    }

    /**
     * 피드백 수정 (작성자 본인만)
     * PUT /api/feedbacks/{feedbackId}
     */
    @PutMapping("/{feedbackId}")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @PathVariable("feedbackId") Long feedbackId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody FeedbackRequest request) {
        
        return ResponseEntity.ok(feedbackService.updateFeedback(feedbackId, memberId, request));
    }

    /**
     * 피드백 삭제 (작성자 또는 관리자)
     * DELETE /api/feedbacks/{feedbackId}
     */
    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<Void> deleteFeedback(
            @PathVariable("feedbackId") Long feedbackId,
            @AuthenticationPrincipal Long memberId) {
        
        feedbackService.deleteFeedback(feedbackId, memberId);
        return ResponseEntity.noContent().build();
    }
}

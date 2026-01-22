package com.portfolio.builder.quiz.presentation;

import com.portfolio.builder.quiz.dto.QuizDto.*;
import com.portfolio.builder.quiz.service.BadgeService;
import com.portfolio.builder.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final BadgeService badgeService;

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryInfo>> getCategories(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(quizService.getCategories(memberId));
    }

    /**
     * 오늘의 퀴즈 조회 (카테고리별 5문제)
     */
    @GetMapping("/daily")
    public ResponseEntity<List<QuizResponse>> getDailyQuiz(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam("category") String category) {
        return ResponseEntity.ok(quizService.getDailyQuiz(memberId, category));
    }

    /**
     * 오늘의 진행 상황 조회
     */
    @GetMapping("/progress")
    public ResponseEntity<DailyProgress> getDailyProgress(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(quizService.getDailyProgress(memberId));
    }

    /**
     * 정답 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<SubmitResponse> submitAnswer(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SubmitRequest request) {
        return ResponseEntity.ok(quizService.submitAnswer(memberId, request));
    }

    /**
     * 사용자 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(quizService.getStats(memberId));
    }

    // ===== Phase 2: 오답 노트 =====

    /**
     * 오답 목록 조회
     */
    @GetMapping("/wrong-answers")
    public ResponseEntity<List<WrongAnswerResponse>> getWrongAnswers(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(quizService.getWrongAnswers(memberId, category));
    }

    /**
     * 오답 통계 조회
     */
    @GetMapping("/wrong-answers/stats")
    public ResponseEntity<WrongAnswerStats> getWrongAnswerStats(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(quizService.getWrongAnswerStats(memberId));
    }

    /**
     * 오답 다시 풀기
     */
    @GetMapping("/wrong-answers/retry")
    public ResponseEntity<List<QuizResponse>> getWrongQuizzes(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "count", defaultValue = "5") int count) {
        return ResponseEntity.ok(quizService.getWrongQuizzes(memberId, category, count));
    }

    // ===== Phase 2: 랭킹 시스템 =====

    /**
     * 랭킹 조회
     */
    @GetMapping("/ranking")
    public ResponseEntity<RankingResponse> getRanking(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "type", defaultValue = "streak") String type,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(quizService.getRanking(memberId, type, limit));
    }

    // ===== Phase 2: 복습 모드 =====

    /**
     * 복습 가능한 문제 통계 조회
     */
    @GetMapping("/review/stats")
    public ResponseEntity<ReviewStatsResponse> getReviewStats(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(quizService.getReviewStats(memberId));
    }

    /**
     * 복습 퀴즈 조회 (내가 푼 문제만, 5개 제한 없음)
     */
    @GetMapping("/review")
    public ResponseEntity<List<QuizResponse>> getReviewQuizzes(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "count", defaultValue = "10") int count,
            @RequestParam(value = "mode", defaultValue = "all") String mode) {
        return ResponseEntity.ok(quizService.getReviewQuizzes(memberId, category, count, mode));
    }

    /**
     * 복습 정답 제출 (스트릭에 영향 없음)
     */
    @PostMapping("/review/submit")
    public ResponseEntity<SubmitResponse> submitReviewAnswer(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SubmitRequest request) {
        return ResponseEntity.ok(quizService.submitReviewAnswer(memberId, request));
    }

    // ===== Phase 2: 배지/업적 =====

    /**
     * 모든 배지 조회 (미획득 포함)
     */
    @GetMapping("/badges")
    public ResponseEntity<List<BadgeResponse>> getAllBadges(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(badgeService.getAllBadges(memberId));
    }

    /**
     * 배지 요약 조회
     */
    @GetMapping("/badges/summary")
    public ResponseEntity<BadgeSummary> getBadgeSummary(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(badgeService.getBadgeSummary(memberId));
    }

    /**
     * 배지 체크 (수동 호출용)
     */
    @PostMapping("/badges/check")
    public ResponseEntity<List<BadgeResponse>> checkBadges(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(badgeService.checkAndAwardBadges(memberId));
    }
}

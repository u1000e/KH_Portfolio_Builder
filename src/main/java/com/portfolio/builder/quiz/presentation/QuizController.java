package com.portfolio.builder.quiz.presentation;

import com.portfolio.builder.quiz.dto.QuizDto.*;
import com.portfolio.builder.quiz.service.BadgeService;
import com.portfolio.builder.quiz.service.BorderService;
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
    private final BorderService borderService;

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryInfo>> getCategories(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "type", defaultValue = "INTERVIEW") String quizType) {
        return ResponseEntity.ok(quizService.getCategories(memberId, quizType));
    }

    /**
     * 오늘의 퀴즈 조회 (카테고리별 5문제) - 면접 대비용
     */
    @GetMapping("/daily")
    public ResponseEntity<List<QuizResponse>> getDailyQuiz(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam("category") String category) {
        return ResponseEntity.ok(quizService.getDailyQuiz(memberId, category));
    }

    /**
     * 수업 복습 퀴즈 조회 (무제한)
     */
    @GetMapping("/practice")
    public ResponseEntity<List<QuizResponse>> getPracticeQuiz(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam("category") String category,
            @RequestParam(value = "count", defaultValue = "10") int count) {
        return ResponseEntity.ok(quizService.getPracticeQuiz(memberId, category, count));
    }

    /**
     * 오늘의 진행 상황 조회
     */
    @GetMapping("/progress")
    public ResponseEntity<DailyProgress> getDailyProgress(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "type", defaultValue = "INTERVIEW") String quizType) {
        return ResponseEntity.ok(quizService.getDailyProgress(memberId, quizType));
    }

    /**
     * 정답 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<SubmitResponse> submitAnswer(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SubmitRequest request) {
        SubmitResponse response = quizService.submitAnswer(memberId, request);
        // 퀴즈 제출 후 배지 자동 체크 및 새 배지 정보 포함
        var newBadges = badgeService.checkAndAwardBadges(memberId);
        response.setNewBadges(newBadges);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "type", defaultValue = "INTERVIEW") String quizType) {
        return ResponseEntity.ok(quizService.getStats(memberId, quizType));
    }

    // ===== Phase 2: 오답 노트 =====

    /**
     * 오답 목록 조회
     */
    @GetMapping("/wrong-answers")
    public ResponseEntity<List<WrongAnswerResponse>> getWrongAnswers(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", defaultValue = "INTERVIEW") String quizType) {
        return ResponseEntity.ok(quizService.getWrongAnswers(memberId, category, quizType));
    }

    /**
     * 오답 통계 조회
     */
    @GetMapping("/wrong-answers/stats")
    public ResponseEntity<WrongAnswerStats> getWrongAnswerStats(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "type", defaultValue = "INTERVIEW") String quizType) {
        return ResponseEntity.ok(quizService.getWrongAnswerStats(memberId, quizType));
    }

    /**
     * 오답 다시 풀기
     */
    @GetMapping("/wrong-answers/retry")
    public ResponseEntity<List<QuizResponse>> getWrongQuizzes(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "count", defaultValue = "5") int count,
            @RequestParam(value = "type", defaultValue = "INTERVIEW") String quizType) {
        return ResponseEntity.ok(quizService.getWrongQuizzes(memberId, category, count, quizType));
    }

    // ===== Phase 2: 랭킹 시스템 =====

    /**
     * 랭킹 조회
     */
    @GetMapping("/ranking")
    public ResponseEntity<RankingResponse> getRanking(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "type", defaultValue = "streak") String type,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "classFilter", defaultValue = "false") boolean classFilter) {
        return ResponseEntity.ok(quizService.getRanking(memberId, type, limit, classFilter));
    }

    // ===== Phase 2: 복습 모드 =====

    /**
     * 복습 가능한 문제 통계 조회
     */
    @GetMapping("/review/stats")
    public ResponseEntity<ReviewStatsResponse> getReviewStats(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "type", defaultValue = "INTERVIEW") String quizType) {
        return ResponseEntity.ok(quizService.getReviewStats(memberId, quizType));
    }

    /**
     * 복습 퀴즈 조회 (내가 푼 문제만, 5개 제한 없음)
     */
    @GetMapping("/review")
    public ResponseEntity<List<QuizResponse>> getReviewQuizzes(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "count", defaultValue = "10") int count,
            @RequestParam(value = "mode", defaultValue = "all") String mode,
            @RequestParam(value = "type", defaultValue = "INTERVIEW") String quizType) {
        return ResponseEntity.ok(quizService.getReviewQuizzes(memberId, category, count, mode, quizType));
    }

    /**
     * 복습 정답 제출
     */
    @PostMapping("/review/submit")
    public ResponseEntity<SubmitResponse> submitReviewAnswer(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SubmitRequest request) {
        SubmitResponse response = quizService.submitReviewAnswer(memberId, request);
        // 복습 제출 후에도 배지 자동 체크
        var newBadges = badgeService.checkAndAwardBadges(memberId);
        response.setNewBadges(newBadges);
        return ResponseEntity.ok(response);
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

    /**
     * 대표 배지 선택
     */
    @PostMapping("/badges/select")
    public ResponseEntity<Void> selectBadge(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SelectBadgeRequest request) {
        badgeService.selectBadge(memberId, request.getBadgeId());
        return ResponseEntity.ok().build();
    }

    /**
     * 선택된 대표 배지 조회
     */
    @GetMapping("/badges/selected")
    public ResponseEntity<BadgeResponse> getSelectedBadge(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(badgeService.getSelectedBadge(memberId));
    }

    /**
     * 학습 캘린더 히트맵 데이터 조회
     * 최근 6개월간의 날짜별 퀴즈 풀이 횟수
     */
    @GetMapping("/heatmap")
    public ResponseEntity<List<HeatmapData>> getHeatmapData(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(quizService.getHeatmapData(memberId));
    }

    /**
     * 나의 학습 통계 조회
     */
    @GetMapping("/stats/learning")
    public ResponseEntity<com.portfolio.builder.quiz.dto.LearningStatsDto> getLearningStats(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(quizService.getLearningStats(memberId));
    }

    // ===== 레벨 테두리 시스템 =====

    /**
     * 테두리 목록 조회 (해금 여부 포함)
     */
    @GetMapping("/borders")
    public ResponseEntity<BorderListResponse> getBorders(
            @RequestAttribute("memberId") Long memberId) {
        return ResponseEntity.ok(borderService.getBorders(memberId));
    }

    /**
     * 테두리 선택
     */
    @PostMapping("/borders/select")
    public ResponseEntity<Void> selectBorder(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SelectBorderRequest request) {
        borderService.selectBorder(memberId, request.getBorderId());
        return ResponseEntity.ok().build();
    }

    /**
     * 배경색 선택
     */
    @PostMapping("/backgrounds/select")
    public ResponseEntity<Void> selectBackground(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SelectBackgroundRequest request) {
        borderService.selectBackground(memberId, request.getBackgroundId());
        return ResponseEntity.ok().build();
    }

    /**
     * 칭호 선택
     */
    @PostMapping("/titles/select")
    public ResponseEntity<Void> selectTitle(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SelectTitleRequest request) {
        borderService.selectTitle(memberId, request.getTitleId());
        return ResponseEntity.ok().build();
    }

    /**
     * 헤더 색상 선택
     */
    @PostMapping("/headers/select")
    public ResponseEntity<Void> selectHeader(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody SelectHeaderRequest request) {
        borderService.selectHeader(memberId, request.getHeaderId());
        return ResponseEntity.ok().build();
    }

    /**
     * 레벨 마일스톤 기록 (10레벨 단위)
     */
    @PostMapping("/level-milestone")
    public ResponseEntity<Void> recordLevelMilestone(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody java.util.Map<String, Integer> request) {
        Integer level = request.get("level");
        if (level != null && level > 0 && level % 10 == 0) {
            quizService.recordLevelMilestone(memberId, level);
        }
        return ResponseEntity.ok().build();
    }
}

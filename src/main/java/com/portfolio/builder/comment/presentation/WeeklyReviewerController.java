package com.portfolio.builder.comment.presentation;

import com.portfolio.builder.comment.application.WeeklyReviewerService;
import com.portfolio.builder.comment.dto.WeeklyRankingResponse;
import com.portfolio.builder.comment.dto.WeeklyReviewerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/weekly-reviewers")
@RequiredArgsConstructor
public class WeeklyReviewerController {

    private final WeeklyReviewerService weeklyReviewerService;

    /**
     * 현재(최신) 주간 베스트 리뷰어 목록 조회
     * - 1, 2, 3등 정보 반환
     * - 대시보드에서 표시용
     */
    @GetMapping("/current")
    public ResponseEntity<List<WeeklyReviewerResponse>> getCurrentWeeklyReviewers() {
        List<WeeklyReviewerResponse> reviewers = weeklyReviewerService.getCurrentWeeklyReviewers();
        return ResponseEntity.ok(reviewers);
    }

    /**
     * 이번 주 주간 랭킹 조회 (복습왕, 토론왕, 반영왕)
     */
    @GetMapping("/rankings")
    public ResponseEntity<List<WeeklyRankingResponse>> getWeeklyRankings() {
        List<WeeklyRankingResponse> rankings = weeklyReviewerService.getWeeklyRankings();
        return ResponseEntity.ok(rankings);
    }
}

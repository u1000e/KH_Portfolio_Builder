package com.portfolio.builder.scheduler;

import com.portfolio.builder.comment.application.WeeklyReviewerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyReviewerScheduler {

    private final WeeklyReviewerService weeklyReviewerService;

    /**
     * 매주 화요일 17:05에 주간 베스트 리뷰어 선정
     * - 집계 기간: 전주 화요일 17:06 ~ 금주 화요일 17:05
     * - 조건: 댓글 3개 이상 (본인 포트폴리오 제외)
     * - 보상: 1, 2, 3등 배지 + 칭호
     */
    @Scheduled(cron = "0 5 17 * * TUE", zone = "Asia/Seoul")
    public void processWeeklyReviewers() {
        log.info("주간 베스트 리뷰어 스케줄러 시작");
        try {
            weeklyReviewerService.processWeeklyReviewers();
            log.info("주간 베스트 리뷰어 스케줄러 완료");
        } catch (Exception e) {
            log.error("주간 베스트 리뷰어 스케줄러 오류", e);
        }
    }
}

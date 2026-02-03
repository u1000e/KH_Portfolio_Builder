package com.portfolio.builder.comment.application;

import com.portfolio.builder.activity.application.ActivityFeedService;
import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.comment.domain.WeeklyReviewer;
import com.portfolio.builder.comment.domain.WeeklyReviewerRepository;
import com.portfolio.builder.comment.dto.WeeklyReviewerResponse;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.quiz.service.BadgeService;
import com.portfolio.builder.quiz.service.BorderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WeeklyReviewerService {

    private final WeeklyReviewerRepository weeklyReviewerRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final BadgeService badgeService;
    private final ActivityFeedService activityFeedService;

    // 순위별 배지 ID
    private static final String[] BADGE_IDS = {
        "hidden_weekly_reviewer_1st",
        "hidden_weekly_reviewer_2nd",
        "hidden_weekly_reviewer_3rd"
    };

    // 순위별 칭호 ID
    private static final String[] TITLE_IDS = {
        "title_weekly_reviewer_1st",
        "title_weekly_reviewer_2nd",
        "title_weekly_reviewer_3rd"
    };

    /**
     * 주간 베스트 리뷰어 처리 (스케줄러에서 호출)
     */
    @Transactional
    public void processWeeklyReviewers() {
        LocalDate today = LocalDate.now();

        // 이번 주 화요일 계산 (오늘이 화요일이면 오늘)
        LocalDate thisTuesday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY));

        // 이미 이번 주 수상자가 있으면 스킵
        if (weeklyReviewerRepository.existsByWeekEndDate(thisTuesday)) {
            log.info("이미 이번 주({}) 수상자가 존재합니다. 스킵합니다.", thisTuesday);
            return;
        }

        // 전주 화요일 17:06 ~ 금주 화요일 17:05
        LocalDate lastTuesday = thisTuesday.minusWeeks(1);
        LocalDateTime startTime = LocalDateTime.of(lastTuesday, LocalTime.of(17, 6));
        LocalDateTime endTime = LocalDateTime.of(thisTuesday, LocalTime.of(17, 5));

        log.info("주간 리뷰어 집계 시작: {} ~ {}", startTime, endTime);

        // 후보 조회 (본인 포폴 제외, 최소 3개 이상)
        List<Object[]> candidates = commentRepository.findWeeklyReviewerCandidates(startTime, endTime);

        if (candidates.isEmpty()) {
            log.info("조건을 충족하는 주간 리뷰어 후보가 없습니다.");
            return;
        }

        log.info("주간 리뷰어 후보 {} 명 발견", candidates.size());

        // 상위 3명 선정 및 처리
        int rank = 1;
        for (int i = 0; i < Math.min(3, candidates.size()); i++) {
            Object[] candidate = candidates.get(i);
            Long memberId = (Long) candidate[0];
            Long commentCount = (Long) candidate[1];

            Member member = memberRepository.findById(memberId).orElse(null);
            if (member == null) {
                log.warn("회원 ID {} 를 찾을 수 없습니다. 스킵합니다.", memberId);
                continue;
            }

            String badgeId = BADGE_IDS[rank - 1];
            String titleId = TITLE_IDS[rank - 1];

            // WeeklyReviewer 저장
            WeeklyReviewer weeklyReviewer = WeeklyReviewer.builder()
                    .member(member)
                    .rankPosition(rank)
                    .commentCount(commentCount.intValue())
                    .weekStartDate(lastTuesday)
                    .weekEndDate(thisTuesday)
                    .awardedBadgeId(badgeId)
                    .awardedTitleId(titleId)
                    .build();
            weeklyReviewerRepository.save(weeklyReviewer);

            // 배지 부여 (숨김 배지)
            boolean badgeAwarded = badgeService.awardHiddenBadge(memberId, badgeId);
            if (badgeAwarded) {
                log.info("{}등 회원 {} 에게 배지 {} 부여", rank, member.getName(), badgeId);
            }

            // 활동 피드 기록
            activityFeedService.recordWeeklyReviewerAward(memberId, rank);

            log.info("주간 리뷰어 {}등: {} (댓글 {}개)", rank, member.getName(), commentCount);
            rank++;
        }

        log.info("주간 리뷰어 처리 완료");
    }

    /**
     * 현재(최신) 주간 베스트 리뷰어 목록 조회
     */
    public List<WeeklyReviewerResponse> getCurrentWeeklyReviewers() {
        List<WeeklyReviewer> reviewers = weeklyReviewerRepository.findLatestWeeklyReviewers();

        return reviewers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * WeeklyReviewer 엔티티를 Response DTO로 변환
     */
    private WeeklyReviewerResponse toResponse(WeeklyReviewer reviewer) {
        Member member = reviewer.getMember();
        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();

        // 순위별 이모지와 칭호명
        String emoji;
        String titleName;
        switch (reviewer.getRankPosition()) {
            case 1:
                emoji = "💖";
                titleName = "주간 리뷰왕";
                break;
            case 2:
                emoji = "💞";
                titleName = "주간 리뷰메이트";
                break;
            case 3:
                emoji = "💌";
                titleName = "주간 리뷰버디";
                break;
            default:
                emoji = "🏅";
                titleName = "주간 리뷰어";
        }

        return WeeklyReviewerResponse.builder()
                .memberId(member.getId())
                .nickname(displayName)
                .avatarUrl(member.getAvatarUrl())
                .rank(reviewer.getRankPosition())
                .commentCount(reviewer.getCommentCount())
                .emoji(emoji)
                .titleName(titleName)
                .weekStartDate(reviewer.getWeekStartDate())
                .weekEndDate(reviewer.getWeekEndDate())
                .awardedAt(reviewer.getAwardedAt())
                .build();
    }
}

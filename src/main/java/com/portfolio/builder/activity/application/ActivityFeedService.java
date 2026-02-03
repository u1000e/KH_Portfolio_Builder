package com.portfolio.builder.activity.application;

import com.portfolio.builder.activity.domain.ActivityFeed;
import com.portfolio.builder.activity.domain.ActivityFeedRepository;
import com.portfolio.builder.activity.dto.ActivityFeedDto;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ActivityFeedService {

    private final ActivityFeedRepository activityFeedRepository;
    private final MemberRepository memberRepository;

    /**
     * S등급 달성 활동 기록
     */
    @Transactional
    public void recordSGradeAchievement(Long memberId, Long portfolioId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();

        ActivityFeed feed = ActivityFeed.builder()
                .member(member)
                .activityType("S_GRADE")
                .message(displayName + "님이 S등급을 달성했습니다!")
                .extraData(String.valueOf(portfolioId))
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .build();

        activityFeedRepository.save(feed);
        log.info("Activity recorded: S_GRADE for member {}", memberId);
    }

    /**
     * 포트폴리오 공개 활동 기록
     */
    @Transactional
    public void recordPortfolioPublic(Long memberId, Long portfolioId, String portfolioTitle) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();

        ActivityFeed feed = ActivityFeed.builder()
                .member(member)
                .activityType("PORTFOLIO_PUBLIC")
                .message(displayName + "님이 포트폴리오를 공개했습니다")
                .extraData(String.valueOf(portfolioId))
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .build();

        activityFeedRepository.save(feed);
        log.info("Activity recorded: PORTFOLIO_PUBLIC for member {}", memberId);
    }

    /**
     * 연속 학습 달성 활동 기록 (7일, 14일, 30일)
     */
    @Transactional
    public void recordStreak(Long memberId, int days) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();
        String activityType = "STREAK_" + days;
        String emoji = days >= 30 ? "👑" : days >= 14 ? "💪" : "🔥";
        String message = displayName + "님이 " + days + "일 연속 학습 달성! " + emoji;

        ActivityFeed feed = ActivityFeed.builder()
                .member(member)
                .activityType(activityType)
                .message(message)
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .build();

        activityFeedRepository.save(feed);
        log.info("Activity recorded: {} for member {}", activityType, memberId);
    }

    /**
     * 문제 풀이 마일스톤 달성 활동 기록 (100, 200, 300, 400, 500, 600문제)
     */
    @Transactional
    public void recordQuizMilestone(Long memberId, int count) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();
        String activityType = "QUIZ_" + count;
        String message = displayName + "님이 " + count + "문제 돌파! 🎯";

        ActivityFeed feed = ActivityFeed.builder()
                .member(member)
                .activityType(activityType)
                .message(message)
                .extraData(String.valueOf(count))
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .build();

        activityFeedRepository.save(feed);
        log.info("Activity recorded: {} for member {}", activityType, memberId);
    }

    /**
     * 레벨 마일스톤 달성 활동 기록 (10, 20, 30... 100레벨)
     */
    @Transactional
    public void recordLevelMilestone(Long memberId, int level) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();
        String activityType = "LEVEL_" + level;

        // 레벨별 이모지와 등급명
        String emoji;
        String tierName;
        if (level >= 100) {
            emoji = "🏅";
            tierName = "개발왕";
        } else if (level >= 80) {
            emoji = "🏴‍☠️";
            tierName = "전국재패";
        } else if (level >= 60) {
            emoji = "🏆";
            tierName = "도내남바완";
        } else if (level >= 40) {
            emoji = "💎";
            tierName = "숙련자";
        } else if (level >= 20) {
            emoji = "🌟";
            tierName = "견습생";
        } else {
            emoji = "⭐";
            tierName = "입문자";
        }

        String message = displayName + "님이 Lv." + level + " 달성! " + emoji + " [" + tierName + "]";

        ActivityFeed feed = ActivityFeed.builder()
                .member(member)
                .activityType(activityType)
                .message(message)
                .extraData(String.valueOf(level))
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .build();

        activityFeedRepository.save(feed);
        log.info("Activity recorded: {} for member {}", activityType, memberId);
    }

    /**
     * 숨겨진 배지 획득 활동 기록
     */
    @Transactional
    public void recordHiddenBadge(Long memberId, String badgeId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();

        ActivityFeed feed = ActivityFeed.builder()
                .member(member)
                .activityType("HIDDEN_BADGE")
                .message(displayName + "님이 숨겨진 배지를 발견했습니다!")
                .extraData(badgeId)
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .build();

        activityFeedRepository.save(feed);
        log.info("Activity recorded: HIDDEN_BADGE {} for member {}", badgeId, memberId);
    }

    /**
     * 주간 베스트 리뷰어 수상 활동 기록
     */
    @Transactional
    public void recordWeeklyReviewerAward(Long memberId, int rank) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();
        String activityType = "WEEKLY_REVIEWER_" + rank;

        // 순위별 이모지와 칭호
        String emoji;
        String titleName;
        switch (rank) {
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

        String message = displayName + "님이 주간 베스트 리뷰어 " + rank + "등에 선정되었습니다! " + emoji + " [" + titleName + "]";

        ActivityFeed feed = ActivityFeed.builder()
                .member(member)
                .activityType(activityType)
                .message(message)
                .extraData(String.valueOf(rank))
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .build();

        activityFeedRepository.save(feed);
        log.info("Activity recorded: {} for member {}", activityType, memberId);
    }

    /**
     * 활동 피드 조회 (같은 반+기수/전체)
     */
    public List<ActivityFeedDto> getRecentActivities(String branch, String classroom, String cohort, int limit) {
        List<ActivityFeed> feeds;

        if (branch != null && classroom != null && cohort != null) {
            feeds = activityFeedRepository.findRecentByBranchAndClassroomAndCohort(branch, classroom, cohort);
        } else {
            feeds = activityFeedRepository.findTop20ByOrderByCreatedAtDesc();
        }

        return feeds.stream()
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ActivityFeedDto toDto(ActivityFeed feed) {
        Member member = feed.getMember();
        return ActivityFeedDto.builder()
                .id(feed.getId())
                .activityType(feed.getActivityType())
                .message(feed.getMessage())
                .memberName(member.getName() != null ? member.getName() : member.getGithubUsername())
                .memberAvatarUrl(member.getAvatarUrl())
                .extraData(feed.getExtraData())
                .createdAt(feed.getCreatedAt())
                .icon(getIconForType(feed.getActivityType()))
                .build();
    }

    private String getIconForType(String activityType) {
        switch (activityType) {
            case "S_GRADE": return "🏆";
            case "PORTFOLIO_PUBLIC": return "📢";
            case "STREAK_7": return "🔥";
            case "STREAK_14": return "💪";
            case "STREAK_30": return "👑";
            case "HIDDEN_BADGE": return "🎉";
            case "QUIZ_100": return "💯";
            case "QUIZ_200": return "🎯";
            case "QUIZ_300": return "⭐";
            case "QUIZ_400": return "🌟";
            case "QUIZ_500": return "💎";
            case "QUIZ_600": return "🏅";
            // 레벨 마일스톤
            case "LEVEL_10": return "⭐";
            case "LEVEL_20": return "🌟";
            case "LEVEL_30": return "🌟";
            case "LEVEL_40": return "💎";
            case "LEVEL_50": return "💎";
            case "LEVEL_60": return "🏆";
            case "LEVEL_70": return "🏆";
            case "LEVEL_80": return "🏴‍☠️";
            case "LEVEL_90": return "🏴‍☠️";
            case "LEVEL_100": return "🏅";
            // 주간 베스트 리뷰어
            case "WEEKLY_REVIEWER_1": return "💖";
            case "WEEKLY_REVIEWER_2": return "💞";
            case "WEEKLY_REVIEWER_3": return "💌";
            default: return "✨";
        }
    }
}

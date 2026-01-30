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
            default: return "✨";
        }
    }
}

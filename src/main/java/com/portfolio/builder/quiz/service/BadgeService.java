package com.portfolio.builder.quiz.service;

import com.portfolio.builder.activity.application.ActivityFeedService;
import com.portfolio.builder.global.exception.NotFoundException;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.quiz.domain.Badge;
import com.portfolio.builder.quiz.domain.QuizStreak;
import com.portfolio.builder.quiz.dto.QuizDto.BadgeResponse;
import com.portfolio.builder.quiz.dto.QuizDto.BadgeSummary;
import com.portfolio.builder.quiz.repository.BadgeRepository;
import com.portfolio.builder.quiz.repository.QuizAttemptRepository;
import com.portfolio.builder.quiz.repository.QuizStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final QuizStreakRepository quizStreakRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MemberRepository memberRepository;
    private final ActivityFeedService activityFeedService;

    // 배지 정의
    private static final List<BadgeDefinition> BADGE_DEFINITIONS = List.of(
            // 첫 걸음
            new BadgeDefinition("first_quiz", "첫 퀴즈", "첫 번째 퀴즈를 풀었습니다!", "🎯", 1),
            
            // 연속 학습
            new BadgeDefinition("streak_3", "3일 연속", "3일 연속 학습 달성!", "🔥", 3),
            new BadgeDefinition("streak_7", "일주일 연속", "7일 연속 학습 달성!", "💪", 7),
            new BadgeDefinition("streak_14", "2주 연속", "14일 연속 학습 달성!", "🌟", 14),
            new BadgeDefinition("streak_30", "한 달 연속", "30일 연속 학습 달성!", "💍", 30),
            
            // 문제 풀이
            new BadgeDefinition("quiz_10", "10문제 달성", "총 10문제를 풀었습니다!", "📚", 10),
            new BadgeDefinition("quiz_50", "50문제 달성", "총 50문제를 풀었습니다!", "📖", 50),
            new BadgeDefinition("quiz_100", "100문제 달성", "총 100문제를 풀었습니다!", "🏆", 100),
            new BadgeDefinition("quiz_200", "200문제 달성", "총 200문제를 풀었습니다!", "💎", 200),
            new BadgeDefinition("quiz_300", "300문제 달성", "총 300문제를 풀었습니다!", "🎆", 300),
            new BadgeDefinition("quiz_400", "400문제 달성", "총 400문제를 풀었습니다!", "💻", 400),
            new BadgeDefinition("quiz_500", "500문제 달성", "총 500문제를 풀었습니다!", "🖥", 500),
            new BadgeDefinition("quiz_600", "600문제 달성", "총 600문제를 풀었습니다!", "🌌", 600),
            
            // 정확도
            new BadgeDefinition("accuracy_80", "정확도 80%", "정확도 80% 이상 달성! (최소 30문제)", "✨", 80),
            new BadgeDefinition("accuracy_90", "정확도 90%", "정확도 90% 이상 달성! (최소 100문제)", "🎖️", 90),
            
            // 카테고리 마스터
            new BadgeDefinition("master_html", "HTML/CSS 마스터", "HTML/CSS 20문제 모두 완료!", "🎨", 20),
            new BadgeDefinition("master_js", "JavaScript 마스터", "JavaScript 20문제 모두 완료!", "⚡", 20),
            new BadgeDefinition("master_react", "React 마스터", "React 20문제 모두 완료!", "⚛️", 20),
            new BadgeDefinition("master_spring", "Spring 마스터", "Spring 20문제 모두 완료!", "🍃", 20),
            new BadgeDefinition("master_spring_adv", "Spring의 왕", "Spring 심화 30문제 모두 완료!", "🌄", 30),
            new BadgeDefinition("master_db", "Database 마스터", "Database 20문제 모두 완료!", "🗄️", 20),
            new BadgeDefinition("master_network", "Network 마스터", "Network 20문제 모두 완료!", "🌐", 20),
            new BadgeDefinition("master_cs", "CS 기초 마스터", "CS 기초 20문제 모두 완료!", "💡", 20),
            new BadgeDefinition("master_java", "Java 마스터", "Java 20문제 모두 완료!", "☕", 20),
            new BadgeDefinition("master_devops", "DevOps 마스터", "DevOps 22문제 모두 완료!", "🐳", 22),
            new BadgeDefinition("master_javacore", "JavaCore 마스터", "JavaCore 20문제 모두 완료!", "🌠", 20),
            new BadgeDefinition("master_architecture", "Architecture 마스터", "Architecture 10문제 모두 완료!", "🏗", 10),
            new BadgeDefinition("master_security", "Security 마스터", "Security 15문제 모두 완료!", "🔰", 15),
            
            // 특별
            new BadgeDefinition("all_categories", "전 분야 학습", "모든 카테고리에서 최소 5문제씩!", "🎓", 10),
            new BadgeDefinition("perfect_day", "완벽한 하루", "하루 10문제 모두 정답!", "💯", 10),
            
            // 입문 & 복습
            new BadgeDefinition("master_beginner", "입문 완료", "입문 40문제 모두 완료!", "🌱", 40),
            new BadgeDefinition("review_master", "복습의 왕", "복습 모드로 200문제 이상 풀기!", "🥇", 200),
            
            // 수업 복습 배지 - Java
            new BadgeDefinition("master_java_class", "Java 수업 정복", "Java 수업 30문제 모두 완료!", "📗", 30),
            new BadgeDefinition("master_java_class_adv", "Java 고급 정복", "Java 수업 고급 30문제 모두 완료!", "📘", 30),
            new BadgeDefinition("master_java_class_deep", "Java 심화 정복", "Java 수업 심화 18문제 모두 완료!", "📕", 18),
            new BadgeDefinition("master_java_class_all", "Java 수업 완전 정복", "Java 수업 배지 3개 모두 획득!", "🍾", 3),
            
            // 수업 복습 배지 - SQL
            new BadgeDefinition("master_sql", "SQL 정복", "SQL 30문제 모두 완료!", "🗃️", 30),
            
            // 수업 복습 배지 - JDBC
            new BadgeDefinition("master_jdbc", "JDBC 정복", "JDBC 22문제 모두 완료!", "🔌", 22),
            
            // 수업 복습 배지 - JavaScript/jQuery
            new BadgeDefinition("master_javascript_class", "JavaScript 수업 정복", "JavaScript 25문제 모두 완료!", "🧩", 25),
            new BadgeDefinition("master_jquery", "jQuery 정복", "jQuery 10문제 모두 완료!", "💠", 10),
            
            // 수업 복습 배지 - React/Infrastructure
            new BadgeDefinition("master_react_class", "React 수업 정복", "React 30문제 모두 완료!", "🔮", 30),
            new BadgeDefinition("master_infrastructure", "Infrastructure 정복", "Infrastructure 30문제 모두 완료!", "🏭", 30),
            
            // 수업 복습 배지 - 웹 개발
            new BadgeDefinition("master_servlet_jsp", "Servlet/JSP 정복", "Servlet/JSP 25문제 모두 완료!", "🎢", 25),
            new BadgeDefinition("master_spring_mvc", "Spring MVC 정복", "Spring MVC 20문제 모두 완료!", "🎇", 20),
            new BadgeDefinition("master_spring_security", "Spring Security 정복", "Spring Security 20문제 모두 완료!", "🔐", 20),
            new BadgeDefinition("master_spring_boot_adv", "Spring Boot 심화 정복", "Spring Boot 심화 18문제 모두 완료!", "🚀", 18),
            new BadgeDefinition("master_web_class_all", "웹 개발 수업 완전 정복", "웹 개발 수업 배지 4개 모두 획득!", "🎊", 4),
            
            // 최종 완료
            new BadgeDefinition("complete_master", "컴플리트", "모든 배지 획득!", "👑", 47)
    );

    // 숨겨진 배지 정의 (뱃지 탭에 안 보이고, 컴플리트 계산에서 제외)
    private static final List<BadgeDefinition> HIDDEN_BADGE_DEFINITIONS = List.of(
            new BadgeDefinition("hidden_owl", "올빼미", "새벽 2~6시에 퀴즈를 풀기", "🦉", 1),
            new BadgeDefinition("hidden_comeback", "귀환", "1주일 이상 잠수 후 복귀", "👻", 7),
            new BadgeDefinition("hidden_popular", "인기스타", "내 포트폴리오에 좋아요 5개 받기", "❤️", 5),
            new BadgeDefinition("hidden_supporter", "서포터", "다른 사람 포트폴리오에 좋아요 10개 누르기", "👍", 10),
            new BadgeDefinition("hidden_social", "소통왕", "내 포트폴리오에 댓글 5개 받기", "💬", 5),
            new BadgeDefinition("hidden_cheerleader", "응원단", "다른 사람 포트폴리오에 댓글 5개 달기", "📣", 5),
            new BadgeDefinition("hidden_diligent", "성실왕", "피드백 5회 반영 하기", "✅", 5),
            // 주간 베스트 리뷰어 배지
            new BadgeDefinition("hidden_weekly_reviewer_1st", "주간 리뷰왕", "주간 베스트 리뷰어 1등 달성!", "💖", 1),
            new BadgeDefinition("hidden_weekly_reviewer_2nd", "주간 리뷰메이트", "주간 베스트 리뷰어 2등 달성!", "💞", 1),
            new BadgeDefinition("hidden_weekly_reviewer_3rd", "주간 리뷰버디", "주간 베스트 리뷰어 3등 달성!", "💌", 1),
            // 면접 토론 배지 (총 좋아요 기준)
            new BadgeDefinition("hidden_best_answer_1st", "면접 해결사", "면접 토론 답변에 총 좋아요 100개 달성!", "🕵️", 100),
            new BadgeDefinition("hidden_best_answer_2nd", "지식 전도사", "면접 토론 답변에 총 좋아요 50개 달성!", "🧙", 50),
            new BadgeDefinition("hidden_best_answer_3rd", "답변메이트", "면접 토론 답변에 총 좋아요 10개 달성!", "✨", 10),
            new BadgeDefinition("hidden_discussion_master", "토론왕", "면접 토론 답변 100개 이상 작성!", "🏇", 100),
            // 피드백 관련 배지
            new BadgeDefinition("hidden_feedback_star", "참잘했어요", "피드백 5회 이상 반영!", "🍒", 5)
    );

    /**
     * 사용자의 모든 배지 조회 (미획득 포함)
     */
    public List<BadgeResponse> getAllBadges(Long memberId) {
        Set<String> earnedBadgeIds = badgeRepository.findByMemberIdOrderByEarnedAtDesc(memberId)
                .stream()
                .map(Badge::getBadgeId)
                .collect(Collectors.toSet());

        Map<String, Badge> earnedBadges = badgeRepository.findByMemberIdOrderByEarnedAtDesc(memberId)
                .stream()
                .collect(Collectors.toMap(Badge::getBadgeId, b -> b));

        List<BadgeResponse> result = new ArrayList<>();

        // 일반 배지 (모두 표시)
        for (BadgeDefinition def : BADGE_DEFINITIONS) {
            boolean earned = earnedBadgeIds.contains(def.id);
            Badge badge = earnedBadges.get(def.id);
            int progress = calculateProgress(memberId, def);

            result.add(BadgeResponse.builder()
                    .badgeId(def.id)
                    .name(def.name)
                    .description(def.description)
                    .icon(def.icon)
                    .earned(earned)
                    .earnedAt(badge != null ? badge.getEarnedAt().toString() : null)
                    .progress(earned ? 100 : progress)
                    .progressText(getProgressText(memberId, def, earned))
                    .isHidden(false)
                    .build());
        }

        // 숨겨진 배지 (획득한 것만 표시)
        for (BadgeDefinition def : HIDDEN_BADGE_DEFINITIONS) {
            if (earnedBadgeIds.contains(def.id)) {
                Badge badge = earnedBadges.get(def.id);
                result.add(BadgeResponse.builder()
                        .badgeId(def.id)
                        .name(def.name)
                        .description(def.description)
                        .icon(def.icon)
                        .earned(true)
                        .earnedAt(badge != null ? badge.getEarnedAt().toString() : null)
                        .progress(100)
                        .progressText("완료!")
                        .isHidden(true)
                        .build());
            }
        }

        return result;
    }

    /**
     * 배지 요약 조회
     */
    public BadgeSummary getBadgeSummary(Long memberId) {
        List<Badge> recentBadges = badgeRepository.findTop5ByMemberIdOrderByEarnedAtDesc(memberId);
        long earnedCount = badgeRepository.countByMemberId(memberId);

        List<BadgeResponse> recentBadgeResponses = recentBadges.stream()
                .map(badge -> {
                    BadgeDefinition def = findDefinition(badge.getBadgeId());
                    return BadgeResponse.builder()
                            .badgeId(badge.getBadgeId())
                            .name(def != null ? def.name : badge.getBadgeId())
                            .description(def != null ? def.description : "")
                            .icon(def != null ? def.icon : "🏅")
                            .earned(true)
                            .earnedAt(badge.getEarnedAt().toString())
                            .progress(100)
                            .build();
                })
                .collect(Collectors.toList());

        return BadgeSummary.builder()
                .totalBadges(BADGE_DEFINITIONS.size())
                .earnedBadges((int) earnedCount)
                .recentBadges(recentBadgeResponses)
                .build();
    }

    /**
     * 배지 체크 및 부여 (퀴즈 제출 후 호출)
     */
    @Transactional
    public List<BadgeResponse> checkAndAwardBadges(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        QuizStreak streak = quizStreakRepository.findByMemberId(memberId).orElse(null);
        List<BadgeResponse> newBadges = new ArrayList<>();

        // 일반 배지 체크
        for (BadgeDefinition def : BADGE_DEFINITIONS) {
            if (badgeRepository.existsByMemberIdAndBadgeId(memberId, def.id)) {
                continue;
            }

            if (checkBadgeCondition(memberId, def, streak)) {
                Badge badge = Badge.builder()
                        .member(member)
                        .badgeId(def.id)
                        .build();
                badgeRepository.save(badge);

                newBadges.add(BadgeResponse.builder()
                        .badgeId(def.id)
                        .name(def.name)
                        .description(def.description)
                        .icon(def.icon)
                        .earned(true)
                        .progress(100)
                        .build());
            }
        }

        // 숨겨진 배지 체크
        for (BadgeDefinition def : HIDDEN_BADGE_DEFINITIONS) {
            if (badgeRepository.existsByMemberIdAndBadgeId(memberId, def.id)) {
                continue;
            }

            if (checkHiddenBadgeCondition(memberId, def, streak)) {
                Badge badge = Badge.builder()
                        .member(member)
                        .badgeId(def.id)
                        .build();
                badgeRepository.save(badge);

                // 숨겨진 배지 획득 시 활동 피드 기록
                activityFeedService.recordHiddenBadge(memberId, def.id);

                newBadges.add(BadgeResponse.builder()
                        .badgeId(def.id)
                        .name(def.name)
                        .description(def.description)
                        .icon(def.icon)
                        .earned(true)
                        .progress(100)
                        .isHidden(true)
                        .build());
            }
        }

        return newBadges;
    }

    /**
     * 특정 숨겨진 배지 부여 (외부 서비스에서 호출)
     * @param memberId 회원 ID
     * @param badgeId 배지 ID (hidden_popular, hidden_supporter 등)
     * @return 배지가 새로 부여되었으면 true
     */
    @Transactional
    public boolean awardHiddenBadge(Long memberId, String badgeId) {
        // 이미 획득한 배지인지 확인
        if (badgeRepository.existsByMemberIdAndBadgeId(memberId, badgeId)) {
            return false;
        }

        // 배지 정의 찾기
        BadgeDefinition def = HIDDEN_BADGE_DEFINITIONS.stream()
                .filter(d -> d.id.equals(badgeId))
                .findFirst()
                .orElse(null);

        if (def == null) {
            return false;
        }

        // 배지 부여
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            return false;
        }

        Badge badge = Badge.builder()
                .member(member)
                .badgeId(badgeId)
                .build();
        badgeRepository.save(badge);

        // 활동 피드 기록
        activityFeedService.recordHiddenBadge(memberId, badgeId);

        return true;
    }

    /**
     * 숨겨진 배지 조건 체크
     */
    private boolean checkHiddenBadgeCondition(Long memberId, BadgeDefinition def, QuizStreak streak) {
        if (streak == null) return false;

        switch (def.id) {
            // 올빼미: 새벽 2~6시에 퀴즈 풀기
            case "hidden_owl":
                int hour = java.time.LocalTime.now().getHour();
                return hour >= 2 && hour < 6;

            // 귀환: 1주 이상 잠수 후 복귀
            case "hidden_comeback":
                if (streak.getLastStudyDate() == null) return false;
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate lastStudy = streak.getLastStudyDate();
                // 마지막 학습일이 오늘이고, 그 전에 7일 이상 쉬었으면
                // 이건 퀴즈 제출 시점에 체크되므로, 오늘 첫 퀴즈인 경우 확인 필요
                // 간단히: 현재 스트릭이 1이고, 이전 학습일과 오늘의 차이가 7일 이상
                if (streak.getCurrentStreak() == 1 && lastStudy.equals(today)) {
                    // 이전 학습 기록을 확인해야 하는데, QuizStreak에는 이전 날짜가 없음
                    // 대안: QuizAttempt에서 이전 마지막 학습일 조회
                    return checkComebackCondition(memberId, today);
                }
                return false;

            default:
                return false;
        }
    }

    /**
     * 귀환 배지 조건 상세 체크 (1주 이상 잠수 후 복귀)
     */
    private boolean checkComebackCondition(Long memberId, java.time.LocalDate today) {
        // 이전에 활동 기록이 있는지 확인 (신규 유저 제외)
        Long totalCount = quizAttemptRepository.countByMemberId(memberId);  
        Long todayCount = quizAttemptRepository.countByMemberIdAndAttemptDate(memberId, today);
        if (totalCount == null || todayCount == null || totalCount <= todayCount) {
            return false; // 이전 활동 없음 = 복귀가 아님
        }

        // 오늘 이전 7일간 기록이 없는지 확인
        for (int i = 1; i <= 7; i++) {
            java.time.LocalDate checkDate = today.minusDays(i);
            Long count = quizAttemptRepository.countByMemberIdAndAttemptDate(memberId, checkDate);
            if (count != null && count > 0) {
                return false; // 7일 내에 푼 기록 있음
            }
        }
        return true; // 7일 이상 쉬다가 복귀
    }

    private boolean checkBadgeCondition(Long memberId, BadgeDefinition def, QuizStreak streak) {
        if (streak == null) return false;

        switch (def.id) {
            // 첫 퀴즈
            case "first_quiz":
                return streak.getTotalQuizCount() >= 1;
            
            // 연속 학습
            case "streak_3":
                return streak.getCurrentStreak() >= 3;
            case "streak_7":
                return streak.getCurrentStreak() >= 7;
            case "streak_14":
                return streak.getCurrentStreak() >= 14;
            case "streak_30":
                return streak.getCurrentStreak() >= 30;
            
            // 문제 수
            case "quiz_10":
                return streak.getTotalQuizCount() >= 10;
            case "quiz_50":
                return streak.getTotalQuizCount() >= 50;
            case "quiz_100":
                return streak.getTotalQuizCount() >= 100;
            case "quiz_200":
                return streak.getTotalQuizCount() >= 200;
            case "quiz_300":
                return streak.getTotalQuizCount() >= 300;
            case "quiz_400":
                return streak.getTotalQuizCount() >= 400;
            case "quiz_500":
                return streak.getTotalQuizCount() >= 500;
            case "quiz_600":
                return streak.getTotalQuizCount() >= 600;
            
            // 정확도
            case "accuracy_80":
                return streak.getTotalQuizCount() >= 30 && 
                       (streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount()) >= 80;
            case "accuracy_90":
                return streak.getTotalQuizCount() >= 100 && 
                       (streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount()) >= 90;
            
            // 카테고리 마스터
            case "master_html":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "HTML/CSS") >= 20;
            case "master_js":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaScript") >= 20;
            case "master_react":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "React") >= 20;
            case "master_spring":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring") >= 20;
            case "master_spring_adv":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring 심화") >= 30;
            case "master_db":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Database") >= 20;
            case "master_network":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Network") >= 20;
            case "master_cs":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "CS 기초") >= 20;
            case "master_java":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java") >= 20;
            case "master_devops":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "DevOps") >= 22;
            case "master_javacore":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaCore") >= 20;
            case "master_architecture":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Architecture") >= 10;
            case "master_security":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Security") >= 15;

            // 입문 완료
            case "master_beginner":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "입문") >= 40;

            // 복습 마스터 - 면접대비 복습모드 + 수업복습 복습모드 (isReviewMode=true) 합산 200문제 이상
            case "review_master":
                return quizAttemptRepository.countReviewModeByMemberId(memberId) >= 200;
            
            // 수업 복습 배지 - Java
            case "master_java_class":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업") >= 30;
            case "master_java_class_adv":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업 고급") >= 30;
            case "master_java_class_deep":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업 심화") >= 18;
            case "master_java_class_all":
                return badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class") &&
                       badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class_adv") &&
                       badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class_deep");
            
            // 수업 복습 배지 - SQL
            case "master_sql":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "SQL") >= 30;
            
            // 수업 복습 배지 - JDBC
            case "master_jdbc":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "JDBC") >= 22;
            
            // 수업 복습 배지 - JavaScript/jQuery
            case "master_javascript_class":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaScript 수업") >= 25;
            case "master_jquery":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "jQuery") >= 10;
            
            // 수업 복습 배지 - React/Infrastructure
            case "master_react_class":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "React 수업") >= 30;
            case "master_infrastructure":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Infrastructure") >= 30;
            
            // 수업 복습 배지 - 웹 개발
            case "master_servlet_jsp":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Servlet/JSP") >= 25;
            case "master_spring_mvc":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring MVC") >= 20;
            case "master_spring_security":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring Security") >= 20;
            case "master_spring_boot_adv":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring Boot 심화") >= 18;
            case "master_web_class_all":
                return badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_servlet_jsp") &&
                       badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_mvc") &&
                       badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_security") &&
                       badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_boot_adv");
            
            // 완벽한 하루 (하루 10문제 모두 정답)
            case "perfect_day":
                Long todayCorrect = quizAttemptRepository.countTodayCorrectByMemberId(memberId, java.time.LocalDate.now());
                Long todayTotal = quizAttemptRepository.countByMemberIdAndAttemptDateAndIsReviewModeFalse(memberId, java.time.LocalDate.now());
                return todayTotal != null && todayTotal >= 10 && todayCorrect != null && todayCorrect.equals(todayTotal);
            
            // 전 분야 학습 (모든 면접대비 카테고리에서 최소 5문제씩)
            case "all_categories":
                String[] categories = {"HTML/CSS", "JavaScript", "React", "Spring", "Spring 심화", "Database", "Network", "CS 기초", "Java", "DevOps", "JavaCore", "Architecture", "Security"};
                for (String category : categories) {
                    if (quizAttemptRepository.countByMemberIdAndCategory(memberId, category) < 5) {
                        return false;
                    }
                }
                return true;
            
            // 컴플리트 마스터 (모든 배지 획득 - 자기 자신 제외)
            case "complete_master":
                long earnedCount = badgeRepository.countByMemberId(memberId);
                // complete_master를 제외한 모든 배지를 획득했는지 확인
                return earnedCount >= BADGE_DEFINITIONS.size() - 1;

            default:
                return false;
        }
    }

    private int calculateProgress(Long memberId, BadgeDefinition def) {
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId).orElse(null);
        if (streak == null) return 0;

        switch (def.id) {
            case "first_quiz":
                return streak.getTotalQuizCount() >= 1 ? 100 : 0;
            case "streak_3":
                return Math.min(100, streak.getCurrentStreak() * 100 / 3);
            case "streak_7":
                return Math.min(100, streak.getCurrentStreak() * 100 / 7);
            case "streak_14":
                return Math.min(100, streak.getCurrentStreak() * 100 / 14);
            case "streak_30":
                return Math.min(100, streak.getCurrentStreak() * 100 / 30);
            case "quiz_10":
                return Math.min(100, streak.getTotalQuizCount() * 100 / 10);
            case "quiz_50":
                return Math.min(100, streak.getTotalQuizCount() * 100 / 50);
            case "quiz_100":
                return Math.min(100, streak.getTotalQuizCount() * 100 / 100);
            case "quiz_200":
                return Math.min(100, streak.getTotalQuizCount() * 100 / 200);
            case "quiz_300":
                return Math.min(100, streak.getTotalQuizCount() * 100 / 300);
            case "quiz_400":
                return Math.min(100, streak.getTotalQuizCount() * 100 / 400);
            case "quiz_500":
                return Math.min(100, streak.getTotalQuizCount() * 100 / 500);
            case "quiz_600":
                return Math.min(100, streak.getTotalQuizCount() * 100 / 600);
            case "master_beginner":
                Long beginnerCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "입문");
                return Math.min(100, (int)(beginnerCount * 100 / 40));
            case "master_devops":
                Long devopsCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "DevOps");
                return Math.min(100, (int)(devopsCount * 100 / 22));
            case "master_javacore":
                Long javaCoreCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaCore");
                return Math.min(100, (int)(javaCoreCount * 100 / 20));
            case "master_architecture":
                Long archCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Architecture");
                return Math.min(100, (int)(archCount * 100 / 10));
            case "master_security":
                Long securityCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Security");
                return Math.min(100, (int)(securityCount * 100 / 15));
            case "review_master":
                Long reviewCount = quizAttemptRepository.countReviewModeByMemberId(memberId);
                return Math.min(100, (int)(reviewCount * 100 / 200));
            case "master_java_class":
                Long javaClassCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업");
                return Math.min(100, (int)(javaClassCount * 100 / 30));
            case "master_java_class_adv":
                Long javaClassAdvCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업 고급");
                return Math.min(100, (int)(javaClassAdvCount * 100 / 30));
            case "master_java_class_deep":
                Long javaClassDeepCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업 심화");
                return Math.min(100, (int)(javaClassDeepCount * 100 / 18));
            case "master_java_class_all":
                int classCount = 0;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class")) classCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class_adv")) classCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class_deep")) classCount++;
                return Math.min(100, classCount * 100 / 3);
            case "master_sql":
                Long sqlCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "SQL");
                return Math.min(100, (int)(sqlCount * 100 / 30));
            case "master_jdbc":
                Long jdbcCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JDBC");
                return Math.min(100, (int)(jdbcCount * 100 / 22));
            case "master_javascript_class":
                Long jsClassCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaScript 수업");
                return Math.min(100, (int)(jsClassCount * 100 / 25));
            case "master_jquery":
                Long jqueryCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "jQuery");
                return Math.min(100, (int)(jqueryCount * 100 / 10));
            case "master_react_class":
                Long reactClassCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "React 수업");
                return Math.min(100, (int)(reactClassCount * 100 / 30));
            case "master_infrastructure":
                Long infraCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Infrastructure");
                return Math.min(100, (int)(infraCount * 100 / 30));
            case "master_servlet_jsp":
                Long servletJspCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Servlet/JSP");
                return Math.min(100, (int)(servletJspCount * 100 / 25));
            case "master_spring_mvc":
                Long springMvcCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring MVC");
                return Math.min(100, (int)(springMvcCount * 100 / 20));
            case "master_spring_security":
                Long springSecurityCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring Security");
                return Math.min(100, (int)(springSecurityCount * 100 / 20));
            case "master_spring_boot_adv":
                Long springBootAdvCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring Boot 심화");
                return Math.min(100, (int)(springBootAdvCount * 100 / 18));
            case "master_web_class_all":
                int webClassCount = 0;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_servlet_jsp")) webClassCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_mvc")) webClassCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_security")) webClassCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_boot_adv")) webClassCount++;
                return Math.min(100, webClassCount * 100 / 4);
            // 면접대비 카테고리 마스터 배지 진행률
            case "master_html":
                Long htmlCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "HTML/CSS");
                return Math.min(100, (int)(htmlCount * 100 / 20));
            case "master_js":
                Long jsCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaScript");
                return Math.min(100, (int)(jsCount * 100 / 20));
            case "master_react":
                Long reactCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "React");
                return Math.min(100, (int)(reactCount * 100 / 20));
            case "master_spring":
                Long springCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring");
                return Math.min(100, (int)(springCount * 100 / 20));
            case "master_spring_adv":
                Long springAdvCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring 심화");
                return Math.min(100, (int)(springAdvCount * 100 / 30));
            case "master_db":
                Long dbCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Database");
                return Math.min(100, (int)(dbCount * 100 / 20));
            case "master_network":
                Long networkCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Network");
                return Math.min(100, (int)(networkCount * 100 / 20));
            case "master_cs":
                Long csCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "CS 기초");
                return Math.min(100, (int)(csCount * 100 / 20));
            case "master_java":
                Long javaCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java");
                return Math.min(100, (int)(javaCount * 100 / 20));
            // 정확도 배지 진행률
            case "accuracy_80":
                if (streak.getTotalQuizCount() < 20) return 0;
                return Math.min(100, (int)(streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount()));
            case "accuracy_90":
                if (streak.getTotalQuizCount() < 30) return 0;
                return Math.min(100, (int)(streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount()));
            // 특별 배지 진행률
            case "perfect_day":
                Long todayCorrectP = quizAttemptRepository.countTodayCorrectByMemberId(memberId, java.time.LocalDate.now());
                Long todayTotalP = quizAttemptRepository.countByMemberIdAndAttemptDateAndIsReviewModeFalse(memberId, java.time.LocalDate.now());
                if (todayTotalP == null || todayTotalP < 10) return (int)(todayTotalP != null ? todayTotalP * 10 : 0);
                return todayCorrectP != null && todayCorrectP.equals(todayTotalP) ? 100 : (int)(todayCorrectP * 100 / todayTotalP);
            case "all_categories":
                String[] allCats = {"HTML/CSS", "JavaScript", "React", "Spring", "Spring 심화", "Database", "Network", "CS 기초", "Java", "DevOps", "JavaCore", "Architecture", "Security"};
                int completedCats = 0;
                for (String cat : allCats) {
                    if (quizAttemptRepository.countByMemberIdAndCategory(memberId, cat) >= 5) completedCats++;
                }
                return Math.min(100, completedCats * 100 / allCats.length);
            case "complete_master":
                long earned = badgeRepository.countByMemberId(memberId);
                int totalMinusOne = BADGE_DEFINITIONS.size() - 1; // 자기 자신 제외
                return Math.min(100, (int)(earned * 100 / totalMinusOne));
            default:
                return 0;
        }
    }

    private String getProgressText(Long memberId, BadgeDefinition def, boolean earned) {
        if (earned) return "완료!";
        
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId).orElse(null);
        if (streak == null) return "0/" + def.threshold;

        switch (def.id) {
            case "streak_3":
            case "streak_7":
            case "streak_14":
            case "streak_30":
                return streak.getCurrentStreak() + "/" + def.threshold + "일";
            case "quiz_10":
            case "quiz_50":
            case "quiz_100":
            case "quiz_200":
            case "quiz_300":
            case "quiz_400":
            case "quiz_500":
            case "quiz_600":
                return streak.getTotalQuizCount() + "/" + def.threshold + "문제";
            case "master_beginner":
                Long beginnerCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "입문");
                return beginnerCnt + "/40문제";
            case "master_javacore":
                Long javaCoreCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaCore");
                return javaCoreCnt + "/20문제";
            case "master_architecture":
                Long archCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Architecture");
                return archCnt + "/10문제";
            case "master_security":
                Long securityCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Security");
                return securityCnt + "/15문제";
            case "review_master":
                Long reviewCnt = quizAttemptRepository.countReviewModeByMemberId(memberId);
                return reviewCnt + "/200문제";
            case "master_java_class":
                Long javaClassCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업");
                return javaClassCnt + "/30문제";
            case "master_java_class_adv":
                Long javaClassAdvCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업 고급");
                return javaClassAdvCnt + "/30문제";
            case "master_java_class_deep":
                Long javaClassDeepCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java 수업 심화");
                return javaClassDeepCnt + "/18문제";
            case "master_java_class_all":
                int classBadgeCount = 0;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class")) classBadgeCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class_adv")) classBadgeCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_java_class_deep")) classBadgeCount++;
                return classBadgeCount + "/3개 배지";
            case "master_sql":
                Long sqlCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "SQL");
                return sqlCnt + "/30문제";
            case "master_jdbc":
                Long jdbcCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JDBC");
                return jdbcCnt + "/22문제";
            case "master_javascript_class":
                Long jsClassCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaScript 수업");
                return jsClassCnt + "/25문제";
            case "master_jquery":
                Long jqueryCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "jQuery");
                return jqueryCnt + "/10문제";
            case "master_react_class":
                Long reactClassCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "React 수업");
                return reactClassCnt + "/30문제";
            case "master_infrastructure":
                Long infraCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Infrastructure");
                return infraCnt + "/30문제";
            case "master_servlet_jsp":
                Long servletJspCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Servlet/JSP");
                return servletJspCnt + "/25문제";
            case "master_spring_mvc":
                Long springMvcCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring MVC");
                return springMvcCnt + "/20문제";
            case "master_spring_security":
                Long springSecurityCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring Security");
                return springSecurityCnt + "/20문제";
            case "master_spring_boot_adv":
                Long springBootAdvCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring Boot 심화");
                return springBootAdvCnt + "/18문제";
            case "master_web_class_all":
                int webBadgeCount = 0;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_servlet_jsp")) webBadgeCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_mvc")) webBadgeCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_security")) webBadgeCount++;
                if (badgeRepository.existsByMemberIdAndBadgeId(memberId, "master_spring_boot_adv")) webBadgeCount++;
                return webBadgeCount + "/4개 배지";
            // 면접대비 카테고리 마스터 배지 진행 텍스트
            case "master_html":
                Long htmlCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "HTML/CSS");
                return htmlCnt + "/20문제";
            case "master_js":
                Long jsCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JavaScript");
                return jsCnt + "/20문제";
            case "master_react":
                Long reactCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "React");
                return reactCnt + "/20문제";
            case "master_spring":
                Long springCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring");
                return springCnt + "/20문제";
            case "master_spring_adv":
                Long springAdvCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Spring 심화");
                return springAdvCnt + "/30문제";
            case "master_db":
                Long dbCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Database");
                return dbCnt + "/20문제";
            case "master_network":
                Long networkCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Network");
                return networkCnt + "/20문제";
            case "master_cs":
                Long csCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "CS 기초");
                return csCnt + "/20문제";
            case "master_java":
                Long javaCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "Java");
                return javaCnt + "/20문제";
            // 정확도 배지 진행 텍스트
            case "accuracy_80":
                if (streak.getTotalQuizCount() < 20) return streak.getTotalQuizCount() + "/20문제 (최소)";
                return String.format("%.1f%%/80%%", streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount());
            case "accuracy_90":
                if (streak.getTotalQuizCount() < 30) return streak.getTotalQuizCount() + "/30문제 (최소)";
                return String.format("%.1f%%/90%%", streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount());
            // 특별 배지 진행 텍스트
            case "perfect_day":
                Long todayTotalT = quizAttemptRepository.countByMemberIdAndAttemptDateAndIsReviewModeFalse(memberId, java.time.LocalDate.now());
                Long todayCorrectT = quizAttemptRepository.countTodayCorrectByMemberId(memberId, java.time.LocalDate.now());
                return todayCorrectT + "/" + todayTotalT + " 정답";
            case "all_categories":
                String[] catList = {"HTML/CSS", "JavaScript", "React", "Spring", "Spring 심화", "Database", "Network", "CS 기초", "Java", "DevOps", "JavaCore", "Architecture", "Security"};
                int doneCats = 0;
                for (String c : catList) {
                    if (quizAttemptRepository.countByMemberIdAndCategory(memberId, c) >= 5) doneCats++;
                }
                return doneCats + "/" + catList.length + "개 분야";
            case "complete_master":
                long earnedCnt = badgeRepository.countByMemberId(memberId);
                return earnedCnt + "/" + (BADGE_DEFINITIONS.size() - 1) + "개";
            default:
                return "";
        }
    }

    private BadgeDefinition findDefinition(String badgeId) {
        // 일반 배지에서 먼저 찾기
        BadgeDefinition found = BADGE_DEFINITIONS.stream()
                .filter(d -> d.id.equals(badgeId))
                .findFirst()
                .orElse(null);

        // 없으면 숨겨진 배지에서 찾기
        if (found == null) {
            found = HIDDEN_BADGE_DEFINITIONS.stream()
                    .filter(d -> d.id.equals(badgeId))
                    .findFirst()
                    .orElse(null);
        }

        return found;
    }

    /**
     * 배지 ID로 아이콘 조회 (갤러리용)
     */
    public String getBadgeIcon(String badgeId) {
        BadgeDefinition def = findDefinition(badgeId);
        return def != null ? def.icon : "🏅";
    }
    
    /**
     * 배지 ID로 이름 조회
     */
    public String getBadgeName(String badgeId) {
        BadgeDefinition def = findDefinition(badgeId);
        return def != null ? def.name : "배지";
    }
    
    /**
     * 배지 ID로 설명 조회
     */
    public String getBadgeDescription(String badgeId) {
        BadgeDefinition def = findDefinition(badgeId);
        return def != null ? def.description : "";
    }

    /**
     * 대표 배지 선택
     */
    @Transactional
    public void selectBadge(Long memberId, String badgeId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        // badgeId가 null이면 선택 해제
        if (badgeId == null || badgeId.isEmpty()) {
            member.setSelectedBadgeId(null);
            memberRepository.save(member);
            return;
        }
        
        // 해당 배지를 획득했는지 확인
        if (!badgeRepository.existsByMemberIdAndBadgeId(memberId, badgeId)) {
            throw new NotFoundException("획득하지 않은 배지입니다.");
        }
        
        member.setSelectedBadgeId(badgeId);
        memberRepository.save(member);
    }

    /**
     * 선택된 대표 배지 조회
     */
    public BadgeResponse getSelectedBadge(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        String selectedBadgeId = member.getSelectedBadgeId();
        
        // 선택된 배지가 없으면 최신 획득 배지 반환
        if (selectedBadgeId == null || selectedBadgeId.isEmpty()) {
            List<Badge> recentBadges = badgeRepository.findTop5ByMemberIdOrderByEarnedAtDesc(memberId);
            if (recentBadges.isEmpty()) {
                return null;
            }
            Badge latestBadge = recentBadges.get(0);
            BadgeDefinition def = findDefinition(latestBadge.getBadgeId());
            if (def == null) return null;
            
            return BadgeResponse.builder()
                    .badgeId(latestBadge.getBadgeId())
                    .name(def.name)
                    .description(def.description)
                    .icon(def.icon)
                    .earned(true)
                    .earnedAt(latestBadge.getEarnedAt().toString())
                    .progress(100)
                    .build();
        }
        
        // 선택된 배지 반환
        BadgeDefinition def = findDefinition(selectedBadgeId);
        if (def == null) return null;
        
        Badge badge = badgeRepository.findByMemberIdAndBadgeId(memberId, selectedBadgeId).orElse(null);
        
        return BadgeResponse.builder()
                .badgeId(selectedBadgeId)
                .name(def.name)
                .description(def.description)
                .icon(def.icon)
                .earned(true)
                .earnedAt(badge != null ? badge.getEarnedAt().toString() : null)
                .progress(100)
                .build();
    }

    // 배지 정의 내부 클래스
    private static class BadgeDefinition {
        final String id;
        final String name;
        final String description;
        final String icon;
        final int threshold;

        BadgeDefinition(String id, String name, String description, String icon, int threshold) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.threshold = threshold;
        }
    }
}

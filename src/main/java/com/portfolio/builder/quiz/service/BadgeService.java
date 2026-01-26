package com.portfolio.builder.quiz.service;

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
            
            // 정확도
            new BadgeDefinition("accuracy_80", "정확도 80%", "정확도 80% 이상 달성! (최소 20문제)", "✨", 80),
            new BadgeDefinition("accuracy_90", "정확도 90%", "정확도 90% 이상 달성! (최소 30문제)", "🎖️", 90),
            
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
            
            // 수업 복습 배지 - JDBC
            new BadgeDefinition("master_jdbc", "JDBC 정복", "JDBC 22문제 모두 완료!", "🔌", 22),
            
            // 수업 복습 배지 - 웹 개발
            new BadgeDefinition("master_servlet_jsp", "Servlet/JSP 정복", "Servlet/JSP 25문제 모두 완료!", "🎢", 25),
            new BadgeDefinition("master_spring_mvc", "Spring MVC 정복", "Spring MVC 20문제 모두 완료!", "🎇", 20),
            new BadgeDefinition("master_spring_security", "Spring Security 정복", "Spring Security 20문제 모두 완료!", "🔐", 20),
            new BadgeDefinition("master_spring_boot_adv", "Spring Boot 심화 정복", "Spring Boot 심화 18문제 모두 완료!", "🚀", 18),
            new BadgeDefinition("master_web_class_all", "웹 개발 수업 완전 정복", "웹 개발 수업 배지 4개 모두 획득!", "🎊", 4),
            
            // 최종 완료
            new BadgeDefinition("complete_master", "컴플리트", "모든 배지 획득!", "👑", 34)
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

        return BADGE_DEFINITIONS.stream()
                .map(def -> {
                    boolean earned = earnedBadgeIds.contains(def.id);
                    Badge badge = earnedBadges.get(def.id);
                    int progress = calculateProgress(memberId, def);
                    
                    return BadgeResponse.builder()
                            .badgeId(def.id)
                            .name(def.name)
                            .description(def.description)
                            .icon(def.icon)
                            .earned(earned)
                            .earnedAt(badge != null ? badge.getEarnedAt().toString() : null)
                            .progress(earned ? 100 : progress)
                            .progressText(getProgressText(memberId, def, earned))
                            .build();
                })
                .collect(Collectors.toList());
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
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId).orElse(null);
        List<BadgeResponse> newBadges = new ArrayList<>();

        for (BadgeDefinition def : BADGE_DEFINITIONS) {
            // 이미 보유 중이면 스킵
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

        return newBadges;
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
            
            // 정확도
            case "accuracy_80":
                return streak.getTotalQuizCount() >= 20 && 
                       (streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount()) >= 80;
            case "accuracy_90":
                return streak.getTotalQuizCount() >= 30 && 
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

            // 입문 완료
            case "master_beginner":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "입문") >= 40;

            // 복습 마스터
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
            
            // 수업 복습 배지 - JDBC
            case "master_jdbc":
                return quizAttemptRepository.countByMemberIdAndCategory(memberId, "JDBC") >= 22;
            
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
            
            // 전 분야 학습 (모든 카테고리에서 최소 5문제씩)
            case "all_categories":
                String[] categories = {"HTML/CSS", "JavaScript", "React", "Spring", "Spring 심화", "Database", "Network", "CS 기초", "Java", "DevOps"};
                for (String category : categories) {
                    if (quizAttemptRepository.countByMemberIdAndCategory(memberId, category) < 5) {
                        return false;
                    }
                }
                return true;
            
            // 컴플리트 마스터 (모든 배지 획득 - 자기 자신 제외)
            case "complete_master":
                long earnedCount = badgeRepository.countByMemberId(memberId);
                // complete_master를 제외한 모든 배지(23개)를 획득했는지 확인
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
            case "master_beginner":
                Long beginnerCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "입문");
                return Math.min(100, (int)(beginnerCount * 100 / 40));
            case "master_devops":
                Long devopsCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "DevOps");
                return Math.min(100, (int)(devopsCount * 100 / 22));
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
            case "master_jdbc":
                Long jdbcCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JDBC");
                return Math.min(100, (int)(jdbcCount * 100 / 22));
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
                return streak.getTotalQuizCount() + "/" + def.threshold + "문제";
            case "master_beginner":
                Long beginnerCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "입문");
                return beginnerCnt + "/40문제";
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
            case "master_jdbc":
                Long jdbcCnt = quizAttemptRepository.countByMemberIdAndCategory(memberId, "JDBC");
                return jdbcCnt + "/22문제";
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
            case "complete_master":
                long earnedCnt = badgeRepository.countByMemberId(memberId);
                return earnedCnt + "/" + (BADGE_DEFINITIONS.size() - 1) + "개";
            default:
                return "";
        }
    }

    private BadgeDefinition findDefinition(String badgeId) {
        return BADGE_DEFINITIONS.stream()
                .filter(d -> d.id.equals(badgeId))
                .findFirst()
                .orElse(null);
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
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        
        // badgeId가 null이면 선택 해제
        if (badgeId == null || badgeId.isEmpty()) {
            member.setSelectedBadgeId(null);
            memberRepository.save(member);
            return;
        }
        
        // 해당 배지를 획득했는지 확인
        if (!badgeRepository.existsByMemberIdAndBadgeId(memberId, badgeId)) {
            throw new RuntimeException("획득하지 않은 배지입니다.");
        }
        
        member.setSelectedBadgeId(badgeId);
        memberRepository.save(member);
    }

    /**
     * 선택된 대표 배지 조회
     */
    public BadgeResponse getSelectedBadge(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        
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

package com.portfolio.builder.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.builder.activity.application.ActivityFeedService;
import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.portfolio.domain.PortfolioLikeRepository;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.quiz.domain.Quiz;
import com.portfolio.builder.quiz.domain.QuizAttempt;
import com.portfolio.builder.quiz.domain.QuizStreak;
import com.portfolio.builder.quiz.dto.QuizDto.*;
import com.portfolio.builder.quiz.repository.QuizAttemptRepository;
import com.portfolio.builder.quiz.repository.QuizRepository;
import com.portfolio.builder.quiz.repository.QuizStreakRepository;
import com.portfolio.builder.quiz.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizStreakRepository quizStreakRepository;
    private final MemberRepository memberRepository;
    private final BadgeRepository badgeRepository;
    private final ActivityFeedService activityFeedService;
    private final CommentRepository commentRepository;
    private final PortfolioLikeRepository portfolioLikeRepository;
    private final ObjectMapper objectMapper;

    // 스트릭 마일스톤 (7일, 14일, 30일)
    private static final int[] STREAK_MILESTONES = {7, 14, 30};
    // 퀴즈 풀이 마일스톤 (100, 200, 300, 400, 500, 600문제)
    private static final int[] QUIZ_MILESTONES = {100, 200, 300, 400, 500, 600};

    private static final int DAILY_LIMIT = 10; 
    private static final int PRACTICE_QUIZ_COUNT = 10;  // 수업 복습 기본 문제 수

    /**
     * 카테고리 목록 조회 (사용자별 진행도 포함)
     */
    public List<CategoryInfo> getCategories(Long memberId, String quizType) {
        List<String> categories = quizRepository.findAllCategoriesByQuizType(quizType);
        
        return categories.stream().map(category -> {
            long totalCount = quizRepository.countByCategoryAndQuizType(category, quizType);
            long solvedCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, category);
            
            return CategoryInfo.builder()
                    .category(category)
                    .totalCount(totalCount)
                    .solvedCount(solvedCount)
                    .build();
        }).collect(Collectors.toList());
    }

    // 기존 메서드 호환용 (기본값 INTERVIEW)
    public List<CategoryInfo> getCategories(Long memberId) {
        return getCategories(memberId, "INTERVIEW");
    }

    /**
     * 오늘의 퀴즈 조회 (카테고리별) - 면접 대비용
     */
    public List<QuizResponse> getDailyQuiz(Long memberId, String category) {
        // 오늘 이미 푼 문제 확인 (면접 대비 타입만, 복습 모드 제외)
        LocalDate today = LocalDate.now();
        Long solvedToday = quizAttemptRepository.countByMemberIdAndAttemptDateAndQuizTypeAndIsReviewModeFalse(memberId, today, "INTERVIEW");
        
        if (solvedToday != null && solvedToday >= DAILY_LIMIT) {
            return new ArrayList<>();  // 일일 제한 완료
        }

        int remaining = DAILY_LIMIT - (solvedToday != null ? solvedToday.intValue() : 0);
        
        // 안 푼 문제 중 랜덤 조회 (이미 푼 문제는 제외)
        List<Quiz> quizzes = quizRepository.findUnsolvedRandomByCategory(category, memberId, remaining);
        
        // 안 푼 문제가 없으면 빈 배열 반환 (이미 다 푼 카테고리)
        if (quizzes.isEmpty()) {
            return new ArrayList<>();
        }

        return quizzes.stream()
                .map(this::toQuizResponse)
                .collect(Collectors.toList());
    }

    /**
     * 수업 복습 퀴즈 조회 (안 푼 문제만 출제)
     */
    public List<QuizResponse> getPracticeQuiz(Long memberId, String category, int count) {
        // 안 푼 문제 중 랜덤 조회 (수업 복습 타입)
        List<Quiz> quizzes = quizRepository.findUnsolvedRandomByCategoryAndQuizType(
                category, "PRACTICE", memberId, count);
        
        // 안 푼 문제가 없으면 빈 배열 반환 (해당 카테고리 완료)
        if (quizzes.isEmpty()) {
            return new ArrayList<>();
        }

        return quizzes.stream()
                .map(this::toQuizResponse)
                .collect(Collectors.toList());
    }

    /**
     * 정답 제출
     */
    @Transactional
    public SubmitResponse submitAnswer(Long memberId, SubmitRequest request) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        boolean isCorrect = quiz.getAnswer().equals(request.getUserAnswer());

        // 퀴즈 타입 결정 (요청에서 받거나 퀴즈 자체의 타입 사용)
        String quizType = request.getQuizType() != null ? request.getQuizType() : quiz.getQuizType();

        // 시도 기록 저장
        QuizAttempt attempt = QuizAttempt.builder()
                .member(member)
                .quiz(quiz)
                .userAnswer(request.getUserAnswer())
                .isCorrect(isCorrect)
                .attemptDate(LocalDate.now())
                .isReviewMode(request.getIsReviewMode() != null && request.getIsReviewMode())
                .quizType(quizType)
                .build();
        quizAttemptRepository.save(attempt);

        // 스트릭 업데이트 (면접 OR 수업복습 모두 스트릭 갱신)
        updateStreak(memberId, isCorrect);

        return SubmitResponse.builder()
                .quizId(quiz.getId())
                .isCorrect(isCorrect)
                .correctAnswer(quiz.getAnswer())
                .explanation(quiz.getExplanation())
                .build();
    }

    /**
     * 오늘의 진행 상황 (퀴즈타입별)
     */
    public DailyProgress getDailyProgress(Long memberId, String quizType) {
        LocalDate today = LocalDate.now();
        
        if ("PRACTICE".equals(quizType)) {
            // 수업 복습은 무제한이므로 오늘 푼 문제 수만 반환
            Long solvedToday = quizAttemptRepository.countByMemberIdAndAttemptDateAndQuizTypeAndIsReviewModeFalse(memberId, today, quizType);
            return DailyProgress.builder()
                    .solvedToday(solvedToday != null ? solvedToday.intValue() : 0)
                    .dailyLimit(0)  // 0은 무제한 의미
                    .completed(false)  // 수업 복습은 완료 개념 없음
                    .build();
        }
        
        // 면접 대비 - quizType으로 필터링해서 면접 문제만 카운트
        Long solvedToday = quizAttemptRepository.countByMemberIdAndAttemptDateAndQuizTypeAndIsReviewModeFalse(memberId, today, "INTERVIEW");
        return DailyProgress.builder()
                .solvedToday(solvedToday != null ? solvedToday.intValue() : 0)
                .dailyLimit(DAILY_LIMIT)
                .completed(solvedToday != null && solvedToday >= DAILY_LIMIT)
                .build();
    }

    // 기존 호환용
    public DailyProgress getDailyProgress(Long memberId) {
        return getDailyProgress(memberId, "INTERVIEW");
    }

    /**
     * 사용자 통계 조회 (퀴즈타입별)
     */
    public StatsResponse getStats(Long memberId, String quizType) {
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId)
                .orElse(QuizStreak.builder()
                        .currentStreak(0)
                        .maxStreak(0)
                        .totalQuizCount(0)
                        .correctCount(0)
                        .build());

        List<String> categories = quizRepository.findAllCategoriesByQuizType(quizType);
        List<CategoryStats> categoryStats = categories.stream().map(category -> {
            long totalCount = quizRepository.countByCategoryAndQuizType(category, quizType);
            long solvedCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, category);
            long correctCount = quizAttemptRepository.countCorrectByMemberIdAndCategory(memberId, category);
            double accuracy = solvedCount > 0 ? (correctCount * 100.0 / solvedCount) : 0;

            return CategoryStats.builder()
                    .category(category)
                    .totalCount(totalCount)
                    .solvedCount(solvedCount)
                    .correctCount(correctCount)
                    .accuracy(Math.round(accuracy * 10) / 10.0)
                    .build();
        }).collect(Collectors.toList());

        double totalAccuracy = streak.getTotalQuizCount() > 0
                ? (streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount())
                : 0;

        // 레벨 계산 (복습모드 횟수만 - isReviewMode=true)
        Long reviewCount = quizAttemptRepository.countReviewModeByMemberId(memberId);
        if (reviewCount == null) reviewCount = 0L;

        // 커뮤니티 활동 보너스 (좋아요 + 댓글)
        int likesGiven = portfolioLikeRepository.countLikesGivenByMemberId(memberId);
        int commentsGiven = commentRepository.countCommentsGivenByMemberId(memberId);

        double[] levelData = calculateLevel(streak.getTotalQuizCount(), Math.round(totalAccuracy * 10) / 10.0, reviewCount, streak.getMaxStreak(), likesGiven, commentsGiven);

        return StatsResponse.builder()
                .currentStreak(streak.getCurrentStreak())
                .maxStreak(streak.getMaxStreak())
                .totalQuizCount(streak.getTotalQuizCount())
                .correctCount(streak.getCorrectCount())
                .accuracy(Math.round(totalAccuracy * 10) / 10.0)
                .categoryStats(categoryStats)
                .level((int) levelData[0])
                .currentXp(levelData[1])
                .nextLevelXp(levelData[2])
                .xpProgress(levelData[3])
                .reviewCount(reviewCount)
                .build();
    }

    /**
     * 레벨 계산
     * rawScore = (푼 문제 수 × 정답률/100) + (복습 횟수 / 10) + (최대 스트릭 × 5) + (좋아요 × 2) + (댓글 × 2)
     * Level = rawScore / 10
     * 최대 레벨: 100
     */
    private double[] calculateLevel(int totalQuizCount, double accuracy, long reviewCount, int maxStreak, int likesGiven, int commentsGiven) {
        double rawScore = (totalQuizCount * (accuracy / 100.0))
                + (reviewCount / 2.0)
                + (maxStreak * 5)
                + (likesGiven * 2)
                + (commentsGiven * 2);
        int level = Math.min(100, (int) Math.floor(rawScore / 10.0));
        double currentXp = (level >= 100) ? 10.0 : rawScore % 10.0;
        double xpProgress = (level >= 100) ? 100.0 : (currentXp / 10.0) * 100.0;
        return new double[]{level, Math.round(currentXp * 10) / 10.0, 10.0, Math.round(xpProgress * 10) / 10.0};
    }

    // 기존 호환용
    public StatsResponse getStats(Long memberId) {
        return getStats(memberId, "INTERVIEW");
    }

    /**
     * 레벨 마일스톤 기록 (10레벨 단위 달성 시 활동 피드에 기록)
     */
    @Transactional
    public void recordLevelMilestone(Long memberId, int level) {
        activityFeedService.recordLevelMilestone(memberId, level);
    }

    /**
     * 스트릭 업데이트
     */
    private void updateStreak(Long memberId, boolean isCorrect) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        QuizStreak streak = quizStreakRepository.findByMemberId(memberId)
                .orElseGet(() -> QuizStreak.builder()
                        .member(member)
                        .currentStreak(0)
                        .maxStreak(0)
                        .lastStudyDate(LocalDate.now().minusDays(1))
                        .totalQuizCount(0)
                        .correctCount(0)
                        .build());

        LocalDate today = LocalDate.now();
        LocalDate lastStudy = streak.getLastStudyDate();

        int previousStreak = streak.getCurrentStreak();
        int previousTotal = streak.getTotalQuizCount();

        // 연속 학습일 계산
        if (lastStudy == null || lastStudy.isBefore(today.minusDays(1))) {
            // 하루 이상 공백 → 스트릭 리셋
            streak.setCurrentStreak(1);
        } else if (lastStudy.equals(today.minusDays(1))) {
            // 어제 학습 → 스트릭 증가
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        }
        // 오늘 이미 학습한 경우 스트릭 유지

        streak.setLastStudyDate(today);
        streak.setTotalQuizCount(streak.getTotalQuizCount() + 1);
        if (isCorrect) {
            streak.setCorrectCount(streak.getCorrectCount() + 1);
        }

        if (streak.getCurrentStreak() > streak.getMaxStreak()) {
            streak.setMaxStreak(streak.getCurrentStreak());
        }

        quizStreakRepository.save(streak);

        // 스트릭 마일스톤 체크 (7일, 14일, 30일)
        int newStreak = streak.getCurrentStreak();
        for (int milestone : STREAK_MILESTONES) {
            if (previousStreak < milestone && newStreak >= milestone) {
                activityFeedService.recordStreak(memberId, milestone);
            }
        }

        // 퀴즈 풀이 마일스톤 체크 (100, 200, 300, 400, 500, 600문제)
        int newTotal = streak.getTotalQuizCount();
        for (int milestone : QUIZ_MILESTONES) {
            if (previousTotal < milestone && newTotal >= milestone) {
                activityFeedService.recordQuizMilestone(memberId, milestone);
            }
        }
    }

    /**
     * Quiz → QuizResponse 변환
     */
    private QuizResponse toQuizResponse(Quiz quiz) {
        List<String> options = null;
        if (quiz.getOptions() != null && !quiz.getOptions().isEmpty()) {
            try {
                options = objectMapper.readValue(quiz.getOptions(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                options = null;
            }
        }

        return QuizResponse.builder()
                .id(quiz.getId())
                .category(quiz.getCategory())
                .type(quiz.getType())
                .question(quiz.getQuestion())
                .options(options)
                .build();
    }

    // ===== Phase 2: 오답 노트 =====
    
    /**
     * 오답 목록 조회 (퀴즈타입별)
     */
    public List<WrongAnswerResponse> getWrongAnswers(Long memberId, String category, String quizType) {
        List<QuizAttempt> wrongAttempts;
        
        if (category != null && !category.isEmpty()) {
            wrongAttempts = quizAttemptRepository.findWrongAnswersByMemberIdAndCategoryAndQuizType(memberId, category, quizType);
        } else {
            wrongAttempts = quizAttemptRepository.findWrongAnswersByMemberIdAndQuizType(memberId, quizType);
        }

        return wrongAttempts.stream()
                .map(this::toWrongAnswerResponse)
                .collect(Collectors.toList());
    }

    // 기존 호환용
    public List<WrongAnswerResponse> getWrongAnswers(Long memberId, String category) {
        return getWrongAnswers(memberId, category, "INTERVIEW");
    }

    /**
     * 오답 통계 조회 (퀴즈타입별)
     */
    public WrongAnswerStats getWrongAnswerStats(Long memberId, String quizType) {
        List<Object[]> categoryWrongCounts = quizAttemptRepository.countWrongByMemberIdGroupByCategoryAndQuizType(memberId, quizType);
        
        List<CategoryWrongCount> breakdown = categoryWrongCounts.stream()
                .map(row -> CategoryWrongCount.builder()
                        .category((String) row[0])
                        .wrongCount(((Number) row[1]).intValue())
                        .build())
                .collect(Collectors.toList());

        int totalWrong = breakdown.stream().mapToInt(CategoryWrongCount::getWrongCount).sum();

        return WrongAnswerStats.builder()
                .totalWrongCount(totalWrong)
                .categoryBreakdown(breakdown)
                .build();
    }

    // 기존 호환용
    public WrongAnswerStats getWrongAnswerStats(Long memberId) {
        return getWrongAnswerStats(memberId, "INTERVIEW");
    }

    /**
     * 오답 다시 풀기 (5개 제한 없음 - 복습 모드, 퀴즈타입별)
     */
    public List<QuizResponse> getWrongQuizzes(Long memberId, String category, int count, String quizType) {
        List<QuizAttempt> wrongAttempts;
        
        if (category != null && !category.isEmpty()) {
            wrongAttempts = quizAttemptRepository.findWrongAnswersByMemberIdAndCategoryAndQuizType(memberId, category, quizType);
        } else {
            wrongAttempts = quizAttemptRepository.findWrongAnswersByMemberIdAndQuizType(memberId, quizType);
        }

        // 중복 제거 및 섞기
        List<Quiz> uniqueQuizzes = wrongAttempts.stream()
                .map(QuizAttempt::getQuiz)
                .distinct()
                .collect(Collectors.toList());
        
        java.util.Collections.shuffle(uniqueQuizzes);
        
        return uniqueQuizzes.stream()
                .limit(count)
                .map(this::toQuizResponse)
                .collect(Collectors.toList());
    }

    // 기존 호환용
    public List<QuizResponse> getWrongQuizzes(Long memberId, String category, int count) {
        return getWrongQuizzes(memberId, category, count, "INTERVIEW");
    }

    private WrongAnswerResponse toWrongAnswerResponse(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        List<String> options = null;
        
        if (quiz.getOptions() != null && !quiz.getOptions().isEmpty()) {
            try {
                options = objectMapper.readValue(quiz.getOptions(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                options = null;
            }
        }

        return WrongAnswerResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getId())
                .category(quiz.getCategory())
                .type(quiz.getType())
                .question(quiz.getQuestion())
                .options(options)
                .userAnswer(attempt.getUserAnswer())
                .correctAnswer(quiz.getAnswer())
                .explanation(quiz.getExplanation())
                .attemptDate(attempt.getAttemptDate().toString())
                .build();
    }

    // ===== Phase 2: 랭킹 시스템 =====
    
    /**
     * 랭킹 조회
     */
    public RankingResponse getRanking(Long memberId, String type, int limit, boolean classFilter) {
        // 현재 사용자 정보 조회 (classFilter용)
        Member currentMember = classFilter ? memberRepository.findById(memberId).orElse(null) : null;
        
        // 특수 랭킹 타입들은 별도 처리
        switch (type) {
            case "review":
                return getReviewRanking(memberId, limit, currentMember);
            case "earlybird":
                return getEarlyBirdRanking(memberId, limit, currentMember);
            case "nightowl":
                return getNightOwlRanking(memberId, limit, currentMember);
            case "today":
                return getTodayRanking(memberId, limit, currentMember);
            case "weekly":
                return getWeeklyRanking(memberId, limit, currentMember);
            case "badge":
                return getBadgeRanking(memberId, limit, currentMember);
            case "rare":
                return getRareBadgeRanking(memberId, limit, currentMember);
            case "level":
                return getLevelRanking(memberId, limit, currentMember);
        }
        
        List<QuizStreak> streaks;
        
        switch (type) {
            case "accuracy":
                streaks = quizStreakRepository.findTopByAccuracy(10);
                break;
            case "total":
                streaks = quizStreakRepository.findTopByTotalQuizCount();
                break;
            case "streak":
            default:
                streaks = quizStreakRepository.findTopByCurrentStreak();
                break;
        }
        
        // classFilter 적용
        if (currentMember != null) {
            streaks = streaks.stream()
                    .filter(s -> isSameClass(s.getMember(), currentMember))
                    .collect(java.util.stream.Collectors.toList());
        }

        List<RankingEntry> rankings = new ArrayList<>();
        RankingEntry myRanking = null;
        
        int currentRank = 1;
        Integer prevValue = null;

        for (int i = 0; i < Math.min(streaks.size(), limit); i++) {
            QuizStreak streak = streaks.get(i);
            int value = getStreakValue(streak, type);
            
            // 동점자 처리: 이전 값과 다르면 현재 순번(i+1)으로 순위 갱신
            if (prevValue == null || !prevValue.equals(value)) {
                currentRank = i + 1;
            }
            prevValue = value;
            
            RankingEntry entry = toRankingEntry(streak, currentRank, type);
            rankings.add(entry);
            
            if (streak.getMember().getId().equals(memberId)) {
                myRanking = entry;
            }
        }

        // 내 순위가 Top에 없으면 별도 조회 (동점자 처리 포함)
        if (myRanking == null) {
            currentRank = 1;
            prevValue = null;
            for (int i = 0; i < streaks.size(); i++) {
                int value = getStreakValue(streaks.get(i), type);
                if (prevValue == null || !prevValue.equals(value)) {
                    currentRank = i + 1;
                }
                prevValue = value;
                
                if (streaks.get(i).getMember().getId().equals(memberId)) {
                    myRanking = toRankingEntry(streaks.get(i), currentRank, type);
                    break;
                }
            }
        }

        return RankingResponse.builder()
                .rankings(rankings)
                .myRanking(myRanking)
                .build();
    }
    
    /**
     * 같은 반인지 확인 (position=수강생, branch, classroom, cohort 모두 일치)
     */
    private boolean isSameClass(Member member, Member currentMember) {
        if (member == null || currentMember == null) return false;
        if (!"수강생".equals(currentMember.getPosition())) return true; // 수강생이 아니면 필터 안함
        
        return "수강생".equals(member.getPosition()) &&
               java.util.Objects.equals(member.getBranch(), currentMember.getBranch()) &&
               java.util.Objects.equals(member.getClassroom(), currentMember.getClassroom()) &&
               java.util.Objects.equals(member.getCohort(), currentMember.getCohort());
    }
    
    /**
     * Object[] 배열에서 같은 반인지 확인 (index 4~7: position, branch, classroom, cohort)
     */
    private boolean isSameClassFromArray(Object[] row, Member currentMember) {
        if (currentMember == null) return true;
        if (!"수강생".equals(currentMember.getPosition())) return true; // 수강생이 아니면 필터 안함
        
        String position = (String) row[4];
        String branch = (String) row[5];
        String classroom = (String) row[6];
        String cohort = (String) row[7];
        
        return "수강생".equals(position) &&
               java.util.Objects.equals(branch, currentMember.getBranch()) &&
               java.util.Objects.equals(classroom, currentMember.getClassroom()) &&
               java.util.Objects.equals(cohort, currentMember.getCohort());
    }
    
    /**
     * 희귀 배지 배열용 필터 (index 4~7: position, branch, classroom, cohort)
     * findMembersWithRareBadges는 row[3]이 badgeId이므로 index가 동일
     */
    private boolean isSameClassFromRareBadgeArray(Object[] row, Member currentMember) {
        if (currentMember == null) return true;
        if (!"수강생".equals(currentMember.getPosition())) return true;
        
        String position = (String) row[4];
        String branch = (String) row[5];
        String classroom = (String) row[6];
        String cohort = (String) row[7];
        
        return "수강생".equals(position) &&
               java.util.Objects.equals(branch, currentMember.getBranch()) &&
               java.util.Objects.equals(classroom, currentMember.getClassroom()) &&
               java.util.Objects.equals(cohort, currentMember.getCohort());
    }
    
    private int getStreakValue(QuizStreak streak, String type) {
        switch (type) {
            case "accuracy":
                return streak.getTotalQuizCount() > 0 
                        ? (int) Math.round(streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount())
                        : 0;
            case "total":
                return streak.getTotalQuizCount();
            case "streak":
            default:
                return streak.getCurrentStreak();
        }
    }

    private RankingEntry toRankingEntry(QuizStreak streak, int rank, String type) {
        int value;
        String displayValue;
        
        switch (type) {
            case "accuracy":
                value = streak.getTotalQuizCount() > 0 
                        ? (int) Math.round(streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount())
                        : 0;
                displayValue = value + "%";
                break;
            case "total":
                value = streak.getTotalQuizCount();
                displayValue = value + "문제";
                break;
            case "streak":
            default:
                value = streak.getCurrentStreak();
                displayValue = value + "일";
                break;
        }

        return RankingEntry.builder()
                .rank(rank)
                .memberId(streak.getMember().getId())
                .nickname(streak.getMember().getName())
                .avatarUrl(streak.getMember().getAvatarUrl())
                .position(buildPositionString(streak.getMember()))
                .value(value)
                .displayValue(displayValue)
                .build();
    }

    /**
     * 소속 정보 문자열 조합 (예: "수강생 종로 501 1기")
     */
    private String buildPositionString(com.portfolio.builder.member.domain.Member member) {
        if (member == null) return null;
        
        StringBuilder sb = new StringBuilder();
        if (member.getPosition() != null) {
            sb.append(member.getPosition());
        }
        if (member.getBranch() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(member.getBranch());
        }
        if (member.getClassroom() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(member.getClassroom());
        }
        if (member.getCohort() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(member.getCohort());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Object[] 배열에서 소속 정보 추출 (index 4~7: position, branch, classroom, cohort)
     */
    private String buildPositionStringFromArray(Object[] row) {
        String position = (String) row[4];
        String branch = (String) row[5];
        String classroom = (String) row[6];
        String cohort = (String) row[7];
        
        StringBuilder sb = new StringBuilder();
        if (position != null) sb.append(position);
        if (branch != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(branch);
        }
        if (classroom != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(classroom);
        }
        if (cohort != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(cohort);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 복습 랭킹 조회 (QuizAttempt에서 집계)
     */
    private RankingResponse getReviewRanking(Long memberId, int limit, Member currentMember) {
        List<Object[]> results = quizAttemptRepository.findTopByReviewCount();
        
        // classFilter 적용
        if (currentMember != null) {
            results = results.stream()
                    .filter(row -> isSameClassFromArray(row, currentMember))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        List<RankingEntry> rankings = new ArrayList<>();
        RankingEntry myRanking = null;
        
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Long rowMemberId = (Long) row[0];
            String name = (String) row[1];
            String avatarUrl = (String) row[2];
            Long reviewCount = (Long) row[3];
            
            RankingEntry entry = RankingEntry.builder()
                    .rank(i + 1)
                    .memberId(rowMemberId)
                    .nickname(name)
                    .avatarUrl(avatarUrl)
                    .position(buildPositionStringFromArray(row))
                    .value(reviewCount.intValue())
                    .displayValue(reviewCount + "문제")
                    .build();
            
            if (i < limit) {
                rankings.add(entry);
            }
            
            if (rowMemberId.equals(memberId)) {
                myRanking = entry;
            }
        }
        
        return RankingResponse.builder()
                .rankings(rankings)
                .myRanking(myRanking)
                .build();
    }

    /**
     * 🌅 얼리버드 랭킹 (아침 6~9시 풀이 횟수)
     */
    private RankingResponse getEarlyBirdRanking(Long memberId, int limit, Member currentMember) {
        return buildGenericRanking(
            quizAttemptRepository.findTopByEarlyBird(),
            memberId, limit, "회", currentMember
        );
    }

    /**
     * 🦉 올빼미 랭킹 (밤 22시~새벽 2시 풀이 횟수)
     */
    private RankingResponse getNightOwlRanking(Long memberId, int limit, Member currentMember) {
        return buildGenericRanking(
            quizAttemptRepository.findTopByNightOwl(),
            memberId, limit, "회", currentMember
        );
    }

    /**
     * 🔥 오늘의 챔피언 (오늘 풀이 횟수)
     */
    private RankingResponse getTodayRanking(Long memberId, int limit, Member currentMember) {
        return buildGenericRanking(
            quizAttemptRepository.findTopByToday(LocalDate.now()),
            memberId, limit, "문제", currentMember
        );
    }

    /**
     * 📅 이번 주 MVP (이번 주 풀이 횟수)
     */
    private RankingResponse getWeeklyRanking(Long memberId, int limit, Member currentMember) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(java.time.DayOfWeek.SUNDAY);
        
        return buildGenericRanking(
            quizAttemptRepository.findTopByThisWeek(weekStart, weekEnd),
            memberId, limit, "문제", currentMember
        );
    }

    /**
     * 👑 배지 컬렉터 랭킹 (배지 개수)
     */
    private RankingResponse getBadgeRanking(Long memberId, int limit, Member currentMember) {
        return buildGenericRanking(
            badgeRepository.findTopByBadgeCount(),
            memberId, limit, "개", currentMember
        );
    }

    /**
     * ⭐ 희귀 배지 랭킹 (10% 미만 획득률 배지 보유 수)
     */
    private RankingResponse getRareBadgeRanking(Long memberId, int limit, Member currentMember) {
        // 1. 전체 수강생 수 계산
        long totalStudents = memberRepository.countByPosition("수강생");
        if (totalStudents == 0) totalStudents = 1; // 0으로 나누기 방지
        
        // 2. 배지별 획득자 수 조회
        List<Object[]> badgeCounts = badgeRepository.countByBadgeIdGrouped();
        
        // 3. 10% 미만 획득률인 희귀 배지 ID 추출
        List<String> rareBadgeIds = new ArrayList<>();
        for (Object[] row : badgeCounts) {
            String badgeId = (String) row[0];
            Long count = (Long) row[1];
            double rate = (count * 100.0) / totalStudents;
            if (rate < 10) {
                rareBadgeIds.add(badgeId);
            }
        }
        
        if (rareBadgeIds.isEmpty()) {
            return RankingResponse.builder()
                    .rankings(new ArrayList<>())
                    .myRanking(null)
                    .build();
        }
        
        // 4. 희귀 배지 보유자별 개수 집계
        List<Object[]> membersWithRare = badgeRepository.findMembersWithRareBadges(rareBadgeIds);
        
        // classFilter 적용
        if (currentMember != null) {
            membersWithRare = membersWithRare.stream()
                    .filter(row -> isSameClassFromRareBadgeArray(row, currentMember))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        Map<Long, RankingEntry> memberMap = new LinkedHashMap<>();
        
        for (Object[] row : membersWithRare) {
            Long rowMemberId = (Long) row[0];
            String name = (String) row[1];
            String avatarUrl = (String) row[2];
            // row[3] = badgeId
            String positionStr = buildPositionStringFromArray(new Object[]{null, null, null, null, row[4], row[5], row[6], row[7]});
            
            memberMap.compute(rowMemberId, (k, v) -> {
                if (v == null) {
                    return RankingEntry.builder()
                            .memberId(rowMemberId)
                            .nickname(name)
                            .avatarUrl(avatarUrl)
                            .position(positionStr)
                            .value(1)
                            .build();
                } else {
                    v.setValue(v.getValue() + 1);
                    return v;
                }
            });
        }
        
        // 5. 개수 기준 정렬 및 순위 부여 (동점자 처리 포함)
        List<RankingEntry> sorted = memberMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(java.util.stream.Collectors.toList());
        
        List<RankingEntry> rankings = new ArrayList<>();
        RankingEntry myRanking = null;
        
        int currentRank = 1;
        Integer prevValue = null;
        
        for (int i = 0; i < sorted.size(); i++) {
            RankingEntry entry = sorted.get(i);
            
            // 동점자 처리: 이전 값과 다르면 현재 순번(i+1)으로 순위 갱신
            if (prevValue == null || !prevValue.equals(entry.getValue())) {
                currentRank = i + 1;
            }
            prevValue = entry.getValue();
            
            entry.setRank(currentRank);
            entry.setDisplayValue(entry.getValue() + "개");
            
            if (i < limit) {
                rankings.add(entry);
            }
            if (entry.getMemberId().equals(memberId)) {
                myRanking = entry;
            }
        }
        
        return RankingResponse.builder()
                .rankings(rankings)
                .myRanking(myRanking)
                .build();
    }

    /**
     * 🏆 레벨 랭킹
     */
    private RankingResponse getLevelRanking(Long memberId, int limit, Member currentMember) {
        // 모든 QuizStreak을 가져와서 레벨 계산
        List<QuizStreak> allStreaks = quizStreakRepository.findAll();

        // classFilter 적용
        if (currentMember != null) {
            allStreaks = allStreaks.stream()
                    .filter(s -> isSameClass(s.getMember(), currentMember))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 레벨 계산 및 정렬을 위한 리스트
        List<Map.Entry<QuizStreak, Integer>> levelList = new ArrayList<>();
        for (QuizStreak streak : allStreaks) {
            int level = calculateLevelForStreak(streak);
            levelList.add(Map.entry(streak, level));
        }

        // 레벨 내림차순 정렬
        levelList.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<RankingEntry> rankings = new ArrayList<>();
        RankingEntry myRanking = null;

        int currentRank = 1;
        Integer prevLevel = null;

        for (int i = 0; i < levelList.size(); i++) {
            Map.Entry<QuizStreak, Integer> entry = levelList.get(i);
            QuizStreak streak = entry.getKey();
            int level = entry.getValue();
            Member member = streak.getMember();

            // 동점자 처리
            if (prevLevel == null || !prevLevel.equals(level)) {
                currentRank = i + 1;
            }
            prevLevel = level;

            String positionStr = buildPositionStringFromMember(member);

            RankingEntry rankEntry = RankingEntry.builder()
                    .rank(currentRank)
                    .memberId(member.getId())
                    .nickname(member.getName())
                    .avatarUrl(member.getAvatarUrl())
                    .position(positionStr)
                    .value(level)
                    .displayValue("Lv." + level)
                    .build();

            if (i < limit) {
                rankings.add(rankEntry);
            }
            if (member.getId().equals(memberId)) {
                myRanking = rankEntry;
            }
        }

        return RankingResponse.builder()
                .rankings(rankings)
                .myRanking(myRanking)
                .build();
    }

    /**
     * QuizStreak에서 레벨 계산 (커뮤니티 활동 포함)
     */
    private int calculateLevelForStreak(QuizStreak streak) {
        Long memberId = streak.getMember().getId();

        double accuracy = streak.getTotalQuizCount() > 0
                ? (streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount())
                : 0;

        Long reviewCount = quizAttemptRepository.countReviewModeByMemberId(memberId);
        if (reviewCount == null) reviewCount = 0L;

        int likesGiven = portfolioLikeRepository.countLikesGivenByMemberId(memberId);
        int commentsGiven = commentRepository.countCommentsGivenByMemberId(memberId);

        double rawScore = (streak.getTotalQuizCount() * (accuracy / 100.0))
                + (reviewCount / 2.0)
                + (streak.getMaxStreak() * 5)
                + (likesGiven * 2)
                + (commentsGiven * 2);

        return Math.min(100, (int) Math.floor(rawScore / 10.0));
    }

    /**
     * Member에서 소속 정보 문자열 생성
     */
    private String buildPositionStringFromMember(Member member) {
        if (member == null) return null;
        StringBuilder sb = new StringBuilder();
        if (member.getPosition() != null) sb.append(member.getPosition());
        if (member.getBranch() != null) sb.append(" ").append(member.getBranch());
        if (member.getClassroom() != null) sb.append(" ").append(member.getClassroom());
        if (member.getCohort() != null) sb.append(" ").append(member.getCohort());
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    /**
     * 공통 랭킹 빌더 (Object[] 결과를 RankingResponse로 변환) - 동점자 처리 포함
     */
    private RankingResponse buildGenericRanking(List<Object[]> results, Long memberId, int limit, String unit, Member currentMember) {
        // classFilter 적용
        if (currentMember != null) {
            results = results.stream()
                    .filter(row -> isSameClassFromArray(row, currentMember))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        List<RankingEntry> rankings = new ArrayList<>();
        RankingEntry myRanking = null;
        
        int currentRank = 1;
        Long prevValue = null;
        
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Long rowMemberId = (Long) row[0];
            String name = (String) row[1];
            String avatarUrl = (String) row[2];
            Long count = (Long) row[3];
            
            // 동점자 처리: 이전 값과 다르면 현재 순번(i+1)으로 순위 갱신
            if (prevValue == null || !prevValue.equals(count)) {
                currentRank = i + 1;
            }
            prevValue = count;
            
            RankingEntry entry = RankingEntry.builder()
                    .rank(currentRank)
                    .memberId(rowMemberId)
                    .nickname(name)
                    .avatarUrl(avatarUrl)
                    .position(buildPositionStringFromArray(row))
                    .value(count.intValue())
                    .displayValue(count + unit)
                    .build();
            
            if (i < limit) {
                rankings.add(entry);
            }
            
            if (rowMemberId.equals(memberId)) {
                myRanking = entry;
            }
        }
        
        return RankingResponse.builder()
                .rankings(rankings)
                .myRanking(myRanking)
                .build();
    }

    // ===== Phase 2: 복습 모드 =====
    
    /**
     * 복습 퀴즈 조회 (내가 푼 문제만, 5개 제한 없음, 퀴즈타입별)
     */
    public List<QuizResponse> getReviewQuizzes(Long memberId, String category, int count, String mode, String quizType) {
        List<QuizAttempt> attempts;
        
        switch (mode) {
            case "wrong":
                // 틀린 문제만
                if (category != null && !category.isEmpty()) {
                    attempts = quizAttemptRepository.findWrongAnswersByMemberIdAndCategoryAndQuizType(memberId, category, quizType);
                } else {
                    attempts = quizAttemptRepository.findWrongAnswersByMemberIdAndQuizType(memberId, quizType);
                }
                break;
            case "correct":
                // 맞은 문제만
                if (category != null && !category.isEmpty()) {
                    attempts = quizAttemptRepository.findCorrectAnswersByMemberIdAndCategory(memberId, category);
                } else {
                    attempts = quizAttemptRepository.findCorrectAnswersByMemberId(memberId);
                }
                break;
            case "all":
            default:
                // 내가 푼 모든 문제
                if (category != null && !category.isEmpty()) {
                    attempts = quizAttemptRepository.findAllSolvedByMemberIdAndCategory(memberId, category);
                } else {
                    attempts = quizAttemptRepository.findAllSolvedByMemberId(memberId);
                }
                break;
        }

        // 중복 제거 및 섞기
        List<Quiz> uniqueQuizzes = attempts.stream()
                .map(QuizAttempt::getQuiz)
                .distinct()
                .collect(Collectors.toList());
        
        java.util.Collections.shuffle(uniqueQuizzes);

        return uniqueQuizzes.stream()
                .limit(count)
                .map(this::toQuizResponse)
                .collect(Collectors.toList());
    }

    // 기존 호환용
    public List<QuizResponse> getReviewQuizzes(Long memberId, String category, int count, String mode) {
        return getReviewQuizzes(memberId, category, count, mode, "INTERVIEW");
    }

    /**
     * 복습 가능한 문제 통계 조회 (퀴즈타입별)
     */
    public ReviewStatsResponse getReviewStats(Long memberId, String quizType) {
        List<Object[]> solvedByCategory = quizAttemptRepository.countSolvedByMemberIdGroupByCategory(memberId);
        List<Object[]> wrongByCategory = quizAttemptRepository.countWrongByMemberIdGroupByCategoryAndQuizType(memberId, quizType);

        // 카테고리별 푼 문제 수
        java.util.Map<String, Long> solvedMap = solvedByCategory.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // 카테고리별 오답 수
        java.util.Map<String, Long> wrongMap = wrongByCategory.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        List<String> categories = quizRepository.findAllCategoriesByQuizType(quizType);
        List<ReviewCategoryStats> categoryStats = categories.stream()
                .map(cat -> ReviewCategoryStats.builder()
                        .category(cat)
                        .solvedCount(solvedMap.getOrDefault(cat, 0L))
                        .wrongCount(wrongMap.getOrDefault(cat, 0L))
                        .correctCount(solvedMap.getOrDefault(cat, 0L) - wrongMap.getOrDefault(cat, 0L))
                        .build())
                .filter(stat -> stat.getSolvedCount() > 0)  // 푼 문제가 있는 카테고리만
                .collect(Collectors.toList());

        long totalSolved = categoryStats.stream().mapToLong(ReviewCategoryStats::getSolvedCount).sum();
        long totalWrong = categoryStats.stream().mapToLong(ReviewCategoryStats::getWrongCount).sum();

        return ReviewStatsResponse.builder()
                .totalSolvedCount(totalSolved)
                .totalWrongCount(totalWrong)
                .totalCorrectCount(totalSolved - totalWrong)
                .categoryStats(categoryStats)
                .build();
    }

    // 기존 호환용
    public ReviewStatsResponse getReviewStats(Long memberId) {
        return getReviewStats(memberId, "INTERVIEW");
    }

    /**
     * 복습 정답 제출 (스트릭/일일 제한에 영향 없음, 복습 마스터 배지용 기록)
     */
    @Transactional
    public SubmitResponse submitReviewAnswer(Long memberId, SubmitRequest request) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        boolean isCorrect = quiz.getAnswer().equals(request.getUserAnswer());

        // 퀴즈 타입 결정
        String quizType = request.getQuizType() != null ? request.getQuizType() : quiz.getQuizType();

        // 복습 모드 기록 저장 (복습 마스터 배지용)
        QuizAttempt attempt = QuizAttempt.builder()
                .member(member)
                .quiz(quiz)
                .userAnswer(request.getUserAnswer())
                .isCorrect(isCorrect)
                .attemptDate(LocalDate.now())
                .isReviewMode(true)  // 복습 모드 플래그
                .quizType(quizType)
                .build();
        quizAttemptRepository.save(attempt);

        // 복습 모드에서도 스트릭 업데이트
        updateStreak(memberId, isCorrect);

        return SubmitResponse.builder()
                .quizId(quiz.getId())
                .isCorrect(isCorrect)
                .correctAnswer(quiz.getAnswer())
                .explanation(quiz.getExplanation())
                .build();
    }

    /**
     * 학습 캘린더 히트맵 데이터 조회
     * 최근 6개월간의 날짜별 퀴즈 풀이 횟수
     */
    public List<HeatmapData> getHeatmapData(Long memberId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);

        List<Object[]> rawData = quizAttemptRepository.findDailyCountsByMemberIdBetween(
                memberId, startDate, endDate);

        return rawData.stream()
                .map(row -> HeatmapData.builder()
                        .date(((LocalDate) row[0]).toString())
                        .count(((Long) row[1]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 나의 학습 통계 조회
     */
    public com.portfolio.builder.quiz.dto.LearningStatsDto getLearningStats(Long memberId) {
        // 1. 기본 통계 조회
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId).orElse(null);
        int totalQuizCount = streak != null ? streak.getTotalQuizCount() : 0;
        int maxStreak = streak != null ? streak.getMaxStreak() : 0;
        double accuracyRate = 0.0;
        if (streak != null && streak.getTotalQuizCount() > 0) {
            accuracyRate = Math.round(streak.getCorrectCount() * 1000.0 / streak.getTotalQuizCount()) / 10.0;
        }

        // 2. 배지 수
        int earnedBadgeCount = (int) badgeRepository.countByMemberId(memberId);

        // 3. 강점 분야 TOP 3 (입문 제외, 최소 10문제 이상 푼 카테고리만)
        List<Object[]> categoryAccuracy = quizAttemptRepository.findCategoryAccuracyByMemberId(memberId);

        // 전체 카테고리별 정답률 계산
        List<com.portfolio.builder.quiz.dto.LearningStatsDto.StrengthCategory> allCategories = categoryAccuracy.stream()
                .filter(row -> !"입문".equals(row[0]))  // 입문 제외
                .filter(row -> ((Long) row[2]) >= 10)   // 최소 10문제 이상
                .map(row -> {
                    String category = (String) row[0];
                    long correct = (Long) row[1];
                    long total = (Long) row[2];
                    double rate = Math.round(correct * 1000.0 / total) / 10.0;
                    return com.portfolio.builder.quiz.dto.LearningStatsDto.StrengthCategory.builder()
                            .category(category)
                            .icon(getCategoryIcon(category))
                            .accuracyRate(rate)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getAccuracyRate(), a.getAccuracyRate()))
                .collect(Collectors.toList());

        // TOP 3 강점 분야
        List<com.portfolio.builder.quiz.dto.LearningStatsDto.StrengthCategory> topStrengths = allCategories.stream()
                .limit(3)
                .collect(Collectors.toList());

        // 100% 정답률 카테고리 (완벽한 분야)
        List<com.portfolio.builder.quiz.dto.LearningStatsDto.StrengthCategory> perfectCategories = allCategories.stream()
                .filter(c -> c.getAccuracyRate() == 100.0)
                .collect(Collectors.toList());

        // 60% 이하 정답률 카테고리 (취약 분야)
        List<com.portfolio.builder.quiz.dto.LearningStatsDto.StrengthCategory> weakCategories = allCategories.stream()
                .filter(c -> c.getAccuracyRate() <= 60.0)
                .sorted((a, b) -> Double.compare(a.getAccuracyRate(), b.getAccuracyRate()))  // 낮은 순
                .collect(Collectors.toList());

        // 4. 개발자 성향 태그
        List<String> personalityTags = new ArrayList<>();

        // 시간대별 성향
        Long morningCount = quizAttemptRepository.countMorningQuizzesByMemberId(memberId);
        Long nightCount = quizAttemptRepository.countNightQuizzesByMemberId(memberId);
        if (morningCount != null && nightCount != null && totalQuizCount > 0) {
            double morningRate = morningCount * 100.0 / totalQuizCount;
            double nightRate = nightCount * 100.0 / totalQuizCount;
            if (morningRate >= 30) {
                personalityTags.add("☀️ 아침형");
            } else if (nightRate >= 30) {
                personalityTags.add("🦉 올빼미");
            }
        }

        // 꾸준함 (최고 스트릭 14일 이상)
        if (maxStreak >= 14) {
            personalityTags.add("🔥 꾸준함");
        }

        // 신중함 (정답률 85% 이상, 최소 30문제)
        if (totalQuizCount >= 30 && accuracyRate >= 85) {
            personalityTags.add("🎯 신중함");
        }

        // 전문 분야 태그 (해당 카테고리에서 20문제 이상 + 80% 이상 정답률)
        Map<String, double[]> categoryStats = new java.util.HashMap<>();
        for (Object[] row : categoryAccuracy) {
            String category = (String) row[0];
            long correct = (Long) row[1];
            long total = (Long) row[2];
            categoryStats.put(category, new double[]{correct, total});
        }

        // 백엔드 전문가 (Spring + Java)
        double[] springStats = categoryStats.getOrDefault("Spring", new double[]{0, 0});
        double[] javaStats = categoryStats.getOrDefault("Java", new double[]{0, 0});
        double backendTotal = springStats[1] + javaStats[1];
        double backendCorrect = springStats[0] + javaStats[0];
        if (backendTotal >= 20 && backendCorrect / backendTotal >= 0.8) {
            personalityTags.add("🍃 백엔드 전문가");
        }

        // 프론트 전문가 (React + JavaScript)
        double[] reactStats = categoryStats.getOrDefault("React", new double[]{0, 0});
        double[] jsStats = categoryStats.getOrDefault("JavaScript", new double[]{0, 0});
        double frontendTotal = reactStats[1] + jsStats[1];
        double frontendCorrect = reactStats[0] + jsStats[0];
        if (frontendTotal >= 20 && frontendCorrect / frontendTotal >= 0.8) {
            personalityTags.add("⚛️ 프론트 전문가");
        }

        // SQL 전문가 (Database + SQL)
        double[] dbStats = categoryStats.getOrDefault("Database", new double[]{0, 0});
        double[] sqlStats = categoryStats.getOrDefault("SQL", new double[]{0, 0});
        double sqlTotal = dbStats[1] + sqlStats[1];
        double sqlCorrect = dbStats[0] + sqlStats[0];
        if (sqlTotal >= 20 && sqlCorrect / sqlTotal >= 0.8) {
            personalityTags.add("🗄️ SQL 전문가");
        }

        // Infra 전문가 (DevOps + Infrastructure)
        double[] devopsStats = categoryStats.getOrDefault("DevOps", new double[]{0, 0});
        double[] infraStats = categoryStats.getOrDefault("Infrastructure", new double[]{0, 0});
        double infraTotal = devopsStats[1] + infraStats[1];
        double infraCorrect = devopsStats[0] + infraStats[0];
        if (infraTotal >= 20 && infraCorrect / infraTotal >= 0.8) {
            personalityTags.add("🐳 Infra 전문가");
        }

        // 복습왕 (복습 횟수가 전체의 50% 이상)
        Long reviewCount = quizAttemptRepository.countReviewModeByMemberId(memberId);
        if (reviewCount == null) reviewCount = 0L;
        if (totalQuizCount > 0 && reviewCount >= totalQuizCount * 0.5) {
            personalityTags.add("📚 복습왕");
        }

        // 풀스택 (백엔드 + 프론트 둘 다 조건 충족)
        if (backendTotal >= 15 && frontendTotal >= 15 &&
            backendCorrect / backendTotal >= 0.75 && frontendCorrect / frontendTotal >= 0.75) {
            personalityTags.add("🌐 풀스택");
        }

        // 피어리뷰어 (다른 사람 포트폴리오에 댓글 10개 이상)
        int commentsGiven = commentRepository.countCommentsGivenByMemberId(memberId);
        if (commentsGiven >= 10) {
            personalityTags.add("💬 피어리뷰어");
        }

        // 스프린터 & 주말 전사 - 날짜별 데이터로 계산
        List<Object[]> dailyCounts = quizAttemptRepository.findDailyCountsByMemberId(memberId);
        long maxDailyCount = 0;
        long weekendQuizCount = 0;
        for (Object[] row : dailyCounts) {
            LocalDate date = (LocalDate) row[0];
            long count = (Long) row[1];
            // 스프린터: 하루 최대 풀이 수
            if (count > maxDailyCount) {
                maxDailyCount = count;
            }
            // 주말 전사: 토(6), 일(7) 풀이 합산
            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                weekendQuizCount += count;
            }
        }

        // 스프린터 (하루 최대 풀이 20문제 이상)
        if (maxDailyCount >= 20) {
            personalityTags.add("⚡ 스프린터");
        }

        // 주말 전사 (주말 학습 비율 50% 이상, 최소 20문제)
        if (totalQuizCount >= 20 && weekendQuizCount * 100.0 / totalQuizCount >= 50) {
            personalityTags.add("📅 주말 전사");
        }

        // 5. 레벨 및 티어 계산
        int likesGiven = portfolioLikeRepository.countLikesGivenByMemberId(memberId);

        double rawScore = (totalQuizCount * (accuracyRate / 100.0))
                + (reviewCount / 2.0)
                + (maxStreak * 5)
                + (likesGiven * 2)
                + (commentsGiven * 2);
        int level = Math.min(100, (int) Math.floor(rawScore / 10.0));

        // 티어 결정
        String tierName;
        String tierEmoji;
        if (level >= 100) {
            tierName = "개발왕";
            tierEmoji = "👑";
        } else if (level >= 80) {
            tierName = "전국재패";
            tierEmoji = "🏴‍☠️";
        } else if (level >= 60) {
            tierName = "도내남바완";
            tierEmoji = "🏆";
        } else if (level >= 40) {
            tierName = "숙련자";
            tierEmoji = "💪";
        } else if (level >= 20) {
            tierName = "견습생";
            tierEmoji = "📚";
        } else {
            tierName = "입문자";
            tierEmoji = "🌱";
        }

        return com.portfolio.builder.quiz.dto.LearningStatsDto.builder()
                .level(level)
                .tierName(tierName)
                .tierEmoji(tierEmoji)
                .totalQuizCount(totalQuizCount)
                .maxStreak(maxStreak)
                .accuracyRate(accuracyRate)
                .earnedBadgeCount(earnedBadgeCount)
                .topStrengths(topStrengths)
                .perfectCategories(perfectCategories)
                .weakCategories(weakCategories)
                .personalityTags(personalityTags)
                .build();
    }

    /**
     * 카테고리별 아이콘
     */
    private String getCategoryIcon(String category) {
        switch (category) {
            case "Spring": case "Spring 심화": return "🍃";
            case "Java": case "JavaCore": return "☕";
            case "React": case "React 수업": return "⚛️";
            case "JavaScript": case "JavaScript 수업": return "⚡";
            case "Database": case "SQL": return "🗄️";
            case "Network": return "🌐";
            case "CS 기초": return "💡";
            case "DevOps": case "Infrastructure": return "🐳";
            case "HTML/CSS": return "🎨";
            case "Architecture": return "🏗️";
            case "Security": case "Spring Security": return "🔐";
            default: return "📚";
        }
    }
}

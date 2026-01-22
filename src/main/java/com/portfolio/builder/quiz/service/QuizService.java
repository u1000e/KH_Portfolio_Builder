package com.portfolio.builder.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.quiz.domain.Quiz;
import com.portfolio.builder.quiz.domain.QuizAttempt;
import com.portfolio.builder.quiz.domain.QuizStreak;
import com.portfolio.builder.quiz.dto.QuizDto.*;
import com.portfolio.builder.quiz.repository.QuizAttemptRepository;
import com.portfolio.builder.quiz.repository.QuizRepository;
import com.portfolio.builder.quiz.repository.QuizStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizStreakRepository quizStreakRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    private static final int DAILY_LIMIT = 10; 

    /**
     * 카테고리 목록 조회 (사용자별 진행도 포함)
     */
    public List<CategoryInfo> getCategories(Long memberId) {
        List<String> categories = quizRepository.findAllCategories();
        
        return categories.stream().map(category -> {
            long totalCount = quizRepository.countByCategory(category);
            long solvedCount = quizAttemptRepository.countByMemberIdAndCategory(memberId, category);
            
            return CategoryInfo.builder()
                    .category(category)
                    .totalCount(totalCount)
                    .solvedCount(solvedCount)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 오늘의 퀴즈 조회 (카테고리별)
     */
    public List<QuizResponse> getDailyQuiz(Long memberId, String category) {
        // 오늘 이미 푼 문제 확인 (복습 모드 제외)
        LocalDate today = LocalDate.now();
        Long solvedToday = quizAttemptRepository.countByMemberIdAndAttemptDateAndIsReviewModeFalse(memberId, today);
        
        if (solvedToday >= DAILY_LIMIT) {
            return new ArrayList<>();  // 일일 제한 완료
        }

        int remaining = DAILY_LIMIT - solvedToday.intValue();
        
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
     * 정답 제출
     */
    @Transactional
    public SubmitResponse submitAnswer(Long memberId, SubmitRequest request) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        boolean isCorrect = quiz.getAnswer().equals(request.getUserAnswer());

        // 시도 기록 저장
        QuizAttempt attempt = QuizAttempt.builder()
                .member(member)
                .quiz(quiz)
                .userAnswer(request.getUserAnswer())
                .isCorrect(isCorrect)
                .attemptDate(LocalDate.now())
                .isReviewMode(request.getIsReviewMode() != null && request.getIsReviewMode())
                .build();
        quizAttemptRepository.save(attempt);

        // 스트릭 업데이트
        updateStreak(memberId, isCorrect);

        return SubmitResponse.builder()
                .quizId(quiz.getId())
                .isCorrect(isCorrect)
                .correctAnswer(quiz.getAnswer())
                .explanation(quiz.getExplanation())
                .build();
    }

    /**
     * 오늘의 진행 상황
     */
    public DailyProgress getDailyProgress(Long memberId) {
        LocalDate today = LocalDate.now();
        // 복습 모드 제외한 일반 풀이만 카운트
        Long solvedToday = quizAttemptRepository.countByMemberIdAndAttemptDateAndIsReviewModeFalse(memberId, today);

        return DailyProgress.builder()
                .solvedToday(solvedToday.intValue())
                .dailyLimit(DAILY_LIMIT)
                .completed(solvedToday >= DAILY_LIMIT)
                .build();
    }

    /**
     * 사용자 통계 조회
     */
    public StatsResponse getStats(Long memberId) {
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId)
                .orElse(QuizStreak.builder()
                        .currentStreak(0)
                        .maxStreak(0)
                        .totalQuizCount(0)
                        .correctCount(0)
                        .build());

        List<String> categories = quizRepository.findAllCategories();
        List<CategoryStats> categoryStats = categories.stream().map(category -> {
            long totalCount = quizRepository.countByCategory(category);
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

        return StatsResponse.builder()
                .currentStreak(streak.getCurrentStreak())
                .maxStreak(streak.getMaxStreak())
                .totalQuizCount(streak.getTotalQuizCount())
                .correctCount(streak.getCorrectCount())
                .accuracy(Math.round(totalAccuracy * 10) / 10.0)
                .categoryStats(categoryStats)
                .build();
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
     * 오답 목록 조회
     */
    public List<WrongAnswerResponse> getWrongAnswers(Long memberId, String category) {
        List<QuizAttempt> wrongAttempts;
        
        if (category != null && !category.isEmpty()) {
            wrongAttempts = quizAttemptRepository.findWrongAnswersByMemberIdAndCategory(memberId, category);
        } else {
            wrongAttempts = quizAttemptRepository.findWrongAnswersByMemberId(memberId);
        }

        return wrongAttempts.stream()
                .map(this::toWrongAnswerResponse)
                .collect(Collectors.toList());
    }

    /**
     * 오답 통계 조회
     */
    public WrongAnswerStats getWrongAnswerStats(Long memberId) {
        List<Object[]> categoryWrongCounts = quizAttemptRepository.countWrongByMemberIdGroupByCategory(memberId);
        
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

    /**
     * 오답 다시 풀기 (5개 제한 없음 - 복습 모드)
     */
    public List<QuizResponse> getWrongQuizzes(Long memberId, String category, int count) {
        List<QuizAttempt> wrongAttempts;
        
        if (category != null && !category.isEmpty()) {
            wrongAttempts = quizAttemptRepository.findWrongAnswersByMemberIdAndCategory(memberId, category);
        } else {
            wrongAttempts = quizAttemptRepository.findWrongAnswersByMemberId(memberId);
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
    public RankingResponse getRanking(Long memberId, String type, int limit) {
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

        List<RankingEntry> rankings = new ArrayList<>();
        RankingEntry myRanking = null;
        int myRank = -1;

        for (int i = 0; i < Math.min(streaks.size(), limit); i++) {
            QuizStreak streak = streaks.get(i);
            RankingEntry entry = toRankingEntry(streak, i + 1, type);
            rankings.add(entry);
            
            if (streak.getMember().getId().equals(memberId)) {
                myRanking = entry;
                myRank = i + 1;
            }
        }

        // 내 순위가 Top에 없으면 별도 조회
        if (myRanking == null) {
            for (int i = 0; i < streaks.size(); i++) {
                if (streaks.get(i).getMember().getId().equals(memberId)) {
                    myRanking = toRankingEntry(streaks.get(i), i + 1, type);
                    break;
                }
            }
        }

        return RankingResponse.builder()
                .rankings(rankings)
                .myRanking(myRanking)
                .build();
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
                .value(value)
                .displayValue(displayValue)
                .build();
    }

    // ===== Phase 2: 복습 모드 =====
    
    /**
     * 복습 퀴즈 조회 (내가 푼 문제만, 5개 제한 없음)
     */
    public List<QuizResponse> getReviewQuizzes(Long memberId, String category, int count, String mode) {
        List<QuizAttempt> attempts;
        
        switch (mode) {
            case "wrong":
                // 틀린 문제만
                if (category != null && !category.isEmpty()) {
                    attempts = quizAttemptRepository.findWrongAnswersByMemberIdAndCategory(memberId, category);
                } else {
                    attempts = quizAttemptRepository.findWrongAnswersByMemberId(memberId);
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

    /**
     * 복습 가능한 문제 통계 조회
     */
    public ReviewStatsResponse getReviewStats(Long memberId) {
        List<Object[]> solvedByCategory = quizAttemptRepository.countSolvedByMemberIdGroupByCategory(memberId);
        List<Object[]> wrongByCategory = quizAttemptRepository.countWrongByMemberIdGroupByCategory(memberId);

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

        List<String> categories = quizRepository.findAllCategories();
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

        // 복습 모드 기록 저장 (복습 마스터 배지용)
        QuizAttempt attempt = QuizAttempt.builder()
                .member(member)
                .quiz(quiz)
                .userAnswer(request.getUserAnswer())
                .isCorrect(isCorrect)
                .attemptDate(LocalDate.now())
                .isReviewMode(true)  // 복습 모드 플래그
                .build();
        quizAttemptRepository.save(attempt);
        
        return SubmitResponse.builder()
                .quizId(quiz.getId())
                .isCorrect(isCorrect)
                .correctAnswer(quiz.getAnswer())
                .explanation(quiz.getExplanation())
                .build();
    }
}

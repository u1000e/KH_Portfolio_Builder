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

package com.portfolio.builder.quiz.dto;

import lombok.*;
import java.util.List;

public class QuizDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizResponse {
        private Long id;
        private String category;
        private String type;  // OX, MULTIPLE
        private String question;
        private List<String> options;  // 객관식 보기 (OX는 null)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizWithAnswer {
        private Long id;
        private String category;
        private String type;
        private String question;
        private List<String> options;
        private Integer answer;
        private String explanation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitRequest {
        private Long quizId;
        private Integer userAnswer;  // OX: 0=X, 1=O / 객관식: 0~3
        private Boolean isReviewMode = false;  // 복습 모드 여부
        private String quizType = "INTERVIEW";  // INTERVIEW: 면접 대비, PRACTICE: 수업 복습
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitResponse {
        private Long quizId;
        private Boolean isCorrect;
        private Integer correctAnswer;
        private String explanation;
        private List<BadgeResponse> newBadges;  // 새로 획득한 배지
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyProgress {
        private int solvedToday;
        private int dailyLimit;
        private boolean completed;
        private String selectedCategory;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatsResponse {
        private int currentStreak;
        private int maxStreak;
        private int totalQuizCount;
        private int correctCount;
        private double accuracy;
        private List<CategoryStats> categoryStats;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryStats {
        private String category;
        private long totalCount;
        private long solvedCount;
        private long correctCount;
        private double accuracy;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryInfo {
        private String category;
        private long totalCount;
        private long solvedCount;
    }

    // ===== Phase 2: 오답 노트 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WrongAnswerResponse {
        private Long attemptId;
        private Long quizId;
        private String category;
        private String type;
        private String question;
        private List<String> options;
        private Integer userAnswer;
        private Integer correctAnswer;
        private String explanation;
        private String attemptDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WrongAnswerStats {
        private int totalWrongCount;
        private List<CategoryWrongCount> categoryBreakdown;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryWrongCount {
        private String category;
        private int wrongCount;
    }

    // ===== Phase 2: 랭킹 시스템 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RankingResponse {
        private List<RankingEntry> rankings;
        private RankingEntry myRanking;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RankingEntry {
        private int rank;
        private Long memberId;
        private String nickname;
        private String avatarUrl;
        private String position;  // 소속 정보 (예: "수강생 종로 501 1기")
        private int value;  // 랭킹 기준 값 (스트릭, 정확도 등)
        private String displayValue;  // 표시용 (예: "15일", "87%")
    }

    // ===== Phase 2: 배지/업적 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BadgeResponse {
        private String badgeId;
        private String name;
        private String description;
        private String icon;
        private boolean earned;
        private String earnedAt;
        private int progress;  // 진행률 (0-100)
        private String progressText;  // "5/10 문제"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BadgeSummary {
        private int totalBadges;
        private int earnedBadges;
        private List<BadgeResponse> recentBadges;
    }

    // ===== Phase 2: 복습 모드 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewQuizRequest {
        private String category;
        private int count;  // 복습할 문제 수
        private String mode;  // "all", "wrong", "correct"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewStatsResponse {
        private long totalSolvedCount;  // 총 푼 문제 수
        private long totalWrongCount;   // 총 오답 수
        private long totalCorrectCount; // 총 정답 수
        private List<ReviewCategoryStats> categoryStats;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewCategoryStats {
        private String category;
        private long solvedCount;  // 해당 카테고리에서 푼 문제 수
        private long wrongCount;   // 해당 카테고리 오답 수
        private long correctCount; // 해당 카테고리 정답 수
    }

    // ===== 대표 배지 선택 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectBadgeRequest {
        private String badgeId;  // 선택한 배지 ID (null이면 선택 해제)
    }
}

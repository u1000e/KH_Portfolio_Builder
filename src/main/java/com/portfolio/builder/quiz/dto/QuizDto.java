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
        // 레벨 시스템
        private int level;           // 현재 레벨 (0-100)
        private double currentXp;    // 현재 XP (0-9.99)
        private double nextLevelXp;  // 다음 레벨 XP (항상 10)
        private double xpProgress;   // 진행률 % (0-100)
        private long reviewCount;    // 복습 횟수
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
        private boolean isHidden;  // 숨겨진 배지 여부
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

    // ===== 학습 캘린더 히트맵 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeatmapData {
        private String date;  // yyyy-MM-dd 형식
        private int count;    // 해당 날짜의 퀴즈 풀이 횟수
    }

    // ===== 레벨 테두리 시스템 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BorderResponse {
        private String borderId;      // 테두리 ID (border_0, border_10, ...)
        private String name;          // 테두리 이름
        private int requiredLevel;    // 필요 레벨
        private String borderStyle;   // CSS 스타일 (dark mode)
        private String borderStyleLight; // CSS 스타일 (light mode)
        private String gradientFrom;  // 그라데이션 시작 색상
        private String gradientTo;    // 그라데이션 끝 색상
        private boolean unlocked;     // 해금 여부
        private boolean selected;     // 현재 선택 여부
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectBorderRequest {
        private String borderId;  // 선택한 테두리 ID (null이면 선택 해제)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BorderListResponse {
        private List<BorderResponse> borders;
        private List<BackgroundResponse> backgrounds;
        private List<TitleResponse> titles;
        private List<HeaderResponse> headers;
        private String selectedBorderId;
        private String selectedBackgroundId;
        private String selectedTitleId;
        private String selectedHeaderId;
        private int currentLevel;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackgroundResponse {
        private String backgroundId;  // 배경 ID
        private String name;          // 배경 이름
        private String colorClass;    // Tailwind 색상 클래스
        private String colorHex;      // HEX 색상 코드
        private boolean selected;     // 현재 선택 여부
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectBackgroundRequest {
        private String backgroundId;  // 선택한 배경 ID (null이면 기본)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TitleResponse {
        private String titleId;       // 칭호 ID
        private String name;          // 칭호 이름
        private String emoji;         // 이모지
        private String colorClass;    // 색상 클래스
        private String colorHex;      // HEX 색상
        private int requiredLevel;    // 필요 레벨 (0이면 특별 칭호)
        private String condition;     // 해금 조건 설명
        private boolean unlocked;     // 해금 여부
        private boolean selected;     // 현재 선택 여부
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectTitleRequest {
        private String titleId;  // 선택한 칭호 ID (null이면 해제)
    }

    // ===== 헤더 색상 시스템 =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeaderResponse {
        private String headerId;      // 헤더 ID (header_0, header_10, ...)
        private String name;          // 헤더 이름
        private int requiredLevel;    // 필요 레벨
        private String colorClass;    // Tailwind 색상 클래스
        private String colorHex;      // HEX 색상 코드
        private String gradientFrom;  // 그라데이션 시작 (옵션)
        private String gradientTo;    // 그라데이션 끝 (옵션)
        private boolean unlocked;     // 해금 여부
        private boolean selected;     // 현재 선택 여부
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectHeaderRequest {
        private String headerId;  // 선택한 헤더 ID (null이면 기본)
    }
}

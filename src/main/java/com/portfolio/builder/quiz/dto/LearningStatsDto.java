package com.portfolio.builder.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStatsDto {

    private int level;                 // 현재 레벨
    private String tierName;           // 티어 이름 (입문자, 견습생, 도내남바완 등)
    private String tierEmoji;          // 티어 이모지
    private int totalQuizCount;        // 총 문제 수
    private int maxStreak;             // 최고 연속 일수
    private double accuracyRate;       // 정답률 (%)
    private int earnedBadgeCount;      // 획득 배지 수
    private long tilCount;             // TIL 작성 수
    private long interviewAnswerCount; // 면접 토론 답변 수
    private double currentXp;          // 현재 레벨 내 XP
    private double nextLevelXp;        // 다음 레벨까지 필요한 XP (항상 10)
    private double xpProgress;         // XP 진행률 (0~100%)
    private String selectedTitleName;  // 장착한 칭호 이름
    private String selectedTitleColor; // 장착한 칭호 색상 (hex)
    private String selectedBadgeIcon;  // 대표 배지 아이콘 (이모지)
    private String selectedBadgeName;  // 대표 배지 이름
    private List<StrengthCategory> topStrengths;  // 강점 분야 TOP 3
    private List<StrengthCategory> perfectCategories;  // 100% 정답률 카테고리
    private List<StrengthCategory> weakCategories;     // 60% 이하 정답률 카테고리
    private List<String> personalityTags;         // 개발자 성향 태그들

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrengthCategory {
        private String category;       // 카테고리명
        private String icon;           // 아이콘
        private double accuracyRate;   // 해당 카테고리 정답률
    }
}

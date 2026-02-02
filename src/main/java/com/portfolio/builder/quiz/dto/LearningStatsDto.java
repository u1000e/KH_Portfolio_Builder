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

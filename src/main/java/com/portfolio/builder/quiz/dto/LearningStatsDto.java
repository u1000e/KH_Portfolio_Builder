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

    private int totalQuizCount;        // 총 문제 수
    private int maxStreak;             // 최고 연속 일수
    private double accuracyRate;       // 정답률 (%)
    private int earnedBadgeCount;      // 획득 배지 수
    private List<StrengthCategory> topStrengths;  // 강점 분야 TOP 3
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

package com.portfolio.builder.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyRankingResponse {
    private String category;    // QUIZ, DISCUSSION, FEEDBACK
    private String title;       // "복습왕", "토론왕", "반영왕"
    private String emoji;       // 카테고리별 이모지
    private Long memberId;
    private String nickname;
    private String avatarUrl;
    private int count;          // 해당 활동 수
    private String countLabel;  // "42문제", "8답변", "5반영"
}

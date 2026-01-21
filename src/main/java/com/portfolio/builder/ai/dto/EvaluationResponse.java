package com.portfolio.builder.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {
    private int totalScore;                    // 총점 (130점 만점)
    private ScoreBreakdown breakdown;          // 세부 점수
    private String overallFeedback;            // AI 생성 종합 피드백
    private List<String> tips;                 // AI 생성 개선 팁
    private LocalDateTime evaluatedAt;         // 평가 시간
}

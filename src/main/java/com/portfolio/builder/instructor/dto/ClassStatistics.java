package com.portfolio.builder.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class ClassStatistics {
    private int totalStudents;          // 총 수강생 수
    private int portfolioCount;         // 포폴 작성 수
    private double completionRate;      // 작성률 (%)
    private Double averageAiScore;      // 평균 AI 점수 (null이면 평가 없음)
    private long totalFeedbacks;        // 총 피드백 수
}

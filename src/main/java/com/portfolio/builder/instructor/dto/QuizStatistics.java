package com.portfolio.builder.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizStatistics {
    private int totalStudents;          // 총 수강생 수
    private int participants;           // 퀴즈 참여자 수
    private double participationRate;   // 참여율 (%)
    private Double averageAccuracy;     // 평균 정답률 (%)
    private List<String> strongCategories;  // 반 강점 카테고리 (정답률 70%+)
    private List<String> weakCategories;    // 반 취약 카테고리 (정답률 60% 미만)
}

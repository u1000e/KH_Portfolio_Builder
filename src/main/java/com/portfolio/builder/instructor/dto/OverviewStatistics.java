package com.portfolio.builder.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverviewStatistics {
    private int totalStudents;
    private double portfolioRate;
    private double quizRate;
    private double tilRate;
    private double interviewRate;
}

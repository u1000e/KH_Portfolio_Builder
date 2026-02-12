package com.portfolio.builder.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewStatistics {
    private int totalStudents;
    private int participants;
    private double participationRate;
    private long totalAnswers;
    private double averageAnswers;
    private long totalLikes;
}

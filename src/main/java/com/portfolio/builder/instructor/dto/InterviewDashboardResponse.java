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
public class InterviewDashboardResponse {
    private InterviewStatistics statistics;
    private List<StudentInterviewStatusDto> students;
}

package com.portfolio.builder.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentOverviewDto {
    private Long id;
    private String name;
    private String githubUsername;
    private String avatarUrl;
    private boolean hasPortfolio;
    private boolean hasQuiz;
    private int totalQuizCount;
    private long tilCount;
    private long interviewAnswerCount;
    private String quizWeakCategory;
    private boolean inactive;
}

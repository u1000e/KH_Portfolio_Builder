package com.portfolio.builder.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentInterviewStatusDto {
    private Long id;
    private String name;
    private String githubUsername;
    private String avatarUrl;
    private long answerCount;
    private long totalLikes;
    private LocalDateTime lastAnswerDate;
}

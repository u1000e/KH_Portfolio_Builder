package com.portfolio.builder.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentQuizStatusDto {
    private Long id;                    // 회원 ID
    private String name;                // 이름
    private String githubUsername;      // GitHub 사용자명
    private String avatarUrl;           // 프로필 이미지

    private int totalQuizCount;         // 총 푼 문제 수
    private int correctCount;           // 맞은 문제 수
    private Double accuracy;            // 정답률 (%)
    private int currentStreak;          // 현재 연속 학습일

    private String strongCategory;      // 강점 카테고리 (정답률 가장 높은)
    private String weakCategory;        // 취약 카테고리 (정답률 가장 낮은)
    private LocalDate lastStudyDate;    // 마지막 학습일
}

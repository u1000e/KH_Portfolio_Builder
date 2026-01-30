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
public class StudentStatusDto {
    private Long id;                    // 회원 ID
    private String name;                // 이름 (없으면 '이름 없음')
    private String githubUsername;      // GitHub 사용자명
    private String avatarUrl;           // 프로필 이미지
    private boolean hasPortfolio;       // 포폴 작성 여부
    private Long portfolioId;           // 포트폴리오 ID (없으면 null)
    private String portfolioTitle;      // 포트폴리오 제목
    private Integer aiScore;            // AI 점수 (null이면 미평가)
    private long feedbackCount;         // 피드백 횟수
    private long unresolvedFeedbackCount; // 미반영 피드백 수
    private LocalDateTime lastUpdated;  // 최근 수정일
}

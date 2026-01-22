package com.portfolio.builder.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 내가 작성한 피드백 응답 DTO (운영팀/강사용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyFeedbackResponse {
    private Long id;
    private Long portfolioId;
    private String portfolioTitle;
    private String portfolioOwnerName;  // 수강생 이름
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

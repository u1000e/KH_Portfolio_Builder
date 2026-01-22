package com.portfolio.builder.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 내가 받은 피드백 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedFeedbackResponse {
    private Long id;
    private Long portfolioId;
    private String portfolioTitle;
    private String content;
    private String authorName;
    private String authorPosition;
    private LocalDateTime createdAt;
}

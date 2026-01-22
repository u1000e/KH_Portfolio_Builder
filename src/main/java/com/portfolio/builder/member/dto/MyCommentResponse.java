package com.portfolio.builder.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 내가 작성한 댓글 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCommentResponse {
    private Long id;
    private Long portfolioId;
    private String portfolioTitle;
    private String portfolioOwnerName;
    private String content;
    private LocalDateTime createdAt;
}

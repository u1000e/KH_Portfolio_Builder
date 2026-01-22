package com.portfolio.builder.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 좋아요한 포트폴리오 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikedPortfolioResponse {
    private Long id;
    private String title;
    private String templateType;
    private String ownerName;
    private String ownerAvatarUrl;
    private LocalDateTime likedAt;
}

package com.portfolio.builder.portfolio.dto;

import com.portfolio.builder.portfolio.domain.Portfolio;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {
    
    private Long id;
    private Long memberId;
    private String memberName;
    private String templateType;
    private String title;
    private String data;  // JSON string
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 좋아요 관련 필드
    private Integer likeCount;
    private Boolean isLiked;  // 현재 사용자의 좋아요 여부

    public static PortfolioResponse from(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .memberId(portfolio.getMember().getId())
                .memberName(portfolio.getMember().getName())
                .templateType(portfolio.getTemplateType())
                .title(portfolio.getTitle())
                .data(portfolio.getData())
                .isPublic(portfolio.getIsPublic())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .likeCount(0)
                .isLiked(false)
                .build();
    }

    public static PortfolioResponse from(Portfolio portfolio, int likeCount, boolean isLiked) {
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .memberId(portfolio.getMember().getId())
                .memberName(portfolio.getMember().getName())
                .templateType(portfolio.getTemplateType())
                .title(portfolio.getTitle())
                .data(portfolio.getData())
                .isPublic(portfolio.getIsPublic())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
}

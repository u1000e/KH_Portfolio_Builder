package com.portfolio.builder.comment.dto;

import com.portfolio.builder.comment.domain.Comment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReceivedCommentResponse {
    private Long id;
    private Long portfolioId;
    private String portfolioTitle;
    private String authorName;
    private String avatarUrl;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static ReceivedCommentResponse from(Comment comment) {
        return ReceivedCommentResponse.builder()
            .id(comment.getId())
            .portfolioId(comment.getPortfolio().getId())
            .portfolioTitle(comment.getPortfolio().getTitle())
            .authorName(comment.getMember().getName() != null
                ? comment.getMember().getName()
                : comment.getMember().getGithubUsername())
            .avatarUrl(comment.getMember().getAvatarUrl())
            .content(comment.getContent())
            .isRead(comment.getIsRead() != null ? comment.getIsRead() : false)
            .createdAt(comment.getCreatedAt())
            .build();
    }
}

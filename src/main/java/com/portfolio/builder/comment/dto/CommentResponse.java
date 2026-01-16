package com.portfolio.builder.comment.dto;

import com.portfolio.builder.comment.domain.Comment;
import com.portfolio.builder.member.domain.Member;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    
    // 작성자 정보
    private Long memberId;
    private String memberName;
    private String avatarUrl;
    
    // 뱃지 정보
    private String position;    // 직원, 강사, 수강생
    private String branch;      // 종로, 강남
    private boolean isOwner;    // 포트폴리오 작성자 여부
    
    // 관리자용 추가 정보
    private Long portfolioId;
    private String portfolioTitle;

    public static CommentResponse from(Comment comment, Long portfolioOwnerId) {
        Member member = comment.getMember();
        
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .memberId(member.getId())
                .memberName(member.getName() != null ? member.getName() : member.getGithubUsername())
                .avatarUrl(member.getAvatarUrl())
                .position(member.getPosition())
                .branch(member.getBranch())
                .isOwner(member.getId().equals(portfolioOwnerId))
                .portfolioId(comment.getPortfolio().getId())
                .portfolioTitle(comment.getPortfolio().getTitle())
                .build();
    }
    
    // 관리자용: 포트폴리오 정보 포함
    public static CommentResponse fromForAdmin(Comment comment) {
        Member member = comment.getMember();
        
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .memberId(member.getId())
                .memberName(member.getName() != null ? member.getName() : member.getGithubUsername())
                .avatarUrl(member.getAvatarUrl())
                .position(member.getPosition())
                .branch(member.getBranch())
                .isOwner(false)
                .portfolioId(comment.getPortfolio().getId())
                .portfolioTitle(comment.getPortfolio().getTitle())
                .build();
    }
}

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
    private String position;    // 운영팀, 강사, 수강생
    private String branch;      // 종로, 강남
    private String classroom;   // 강의실 (수강생)
    private String cohort;      // 기수(수강생) / 별칭(강사) / 부서(운영팀)
    private boolean isOwner;    // 포트폴리오 작성자 여부
    private Boolean isHidden;

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
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .isOwner(member.getId().equals(portfolioOwnerId))
                .isHidden(comment.getIsHidden())
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
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .isOwner(false)
                .isHidden(comment.getIsHidden())
                .portfolioId(comment.getPortfolio().getId())
                .portfolioTitle(comment.getPortfolio().getTitle())
                .build();
    }
}

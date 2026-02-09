package com.portfolio.builder.til.dto;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.til.domain.TIL;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class TILResponse {

    private Long id;
    private String title;
    private String difficulty;
    private String description;
    private String codeSnippet;
    private String codeLanguage;
    private List<String> tags;
    private String imageUrl;
    private String reflection;
    private Integer likeCount;
    private Boolean isLiked;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long memberId;
    private String memberName;
    private String avatarUrl;
    private String position;
    private String branch;
    private String classroom;

    private Boolean isOwner;

    public static TILResponse from(TIL til, Long currentMemberId, boolean isLiked) {
        Member member = til.getMember();
        List<String> tagList = til.getTags() != null && !til.getTags().isEmpty()
                ? Arrays.asList(til.getTags().split(","))
                : Collections.emptyList();

        return TILResponse.builder()
                .id(til.getId())
                .title(til.getTitle())
                .difficulty(til.getDifficulty())
                .description(til.getDescription())
                .codeSnippet(til.getCodeSnippet())
                .codeLanguage(til.getCodeLanguage())
                .tags(tagList)
                .imageUrl(til.getImageUrl())
                .reflection(til.getReflection())
                .likeCount(til.getLikeCount() != null ? til.getLikeCount() : 0)
                .isLiked(isLiked)
                .isPublic(til.getIsPublic())
                .createdAt(til.getCreatedAt())
                .updatedAt(til.getUpdatedAt())
                .memberId(member.getId())
                .memberName(member.getName() != null ? member.getName() : member.getGithubUsername())
                .avatarUrl(member.getAvatarUrl())
                .position(member.getPosition())
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .isOwner(member.getId().equals(currentMemberId))
                .build();
    }
}

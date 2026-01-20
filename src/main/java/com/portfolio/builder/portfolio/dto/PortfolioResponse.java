package com.portfolio.builder.portfolio.dto;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.portfolio.domain.Portfolio;
import jakarta.persistence.EntityNotFoundException;
import lombok.*;
import org.hibernate.Hibernate;

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
    private Boolean showContributionGraph;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 좋아요 관련 필드
    private Integer likeCount;
    private Boolean isLiked;  // 현재 사용자의 좋아요 여부

    /**
     * Lazy 로딩된 Member 프록시를 안전하게 초기화합니다.
     * Member가 없거나 DB에서 삭제된 경우 EntityNotFoundException을 방지합니다.
     */
    private static Member safeGetMember(Portfolio portfolio) {
        if (portfolio.getMember() == null) {
            return null;
        }
        try {
            // 프록시 초기화 시도 - 실제 DB에 존재하는지 확인
            Hibernate.initialize(portfolio.getMember());
            // 실제 값 접근으로 EntityNotFoundException 유도
            portfolio.getMember().getId();
            return portfolio.getMember();
        } catch (EntityNotFoundException e) {
            // Member가 DB에서 삭제된 경우 (고아 Portfolio)
            return null;
        }
    }

    public static PortfolioResponse from(Portfolio portfolio) {
        Member member = safeGetMember(portfolio);
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .memberId(member != null ? member.getId() : null)
                .memberName(member != null ? member.getName() : "Unknown (삭제된 사용자)")
                .templateType(portfolio.getTemplateType())
                .title(portfolio.getTitle())
                .data(portfolio.getData())
                .isPublic(portfolio.getIsPublic())
                .showContributionGraph(portfolio.getShowContributionGraph())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .likeCount(0)
                .isLiked(false)
                .build();
    }

    public static PortfolioResponse from(Portfolio portfolio, int likeCount, boolean isLiked) {
        Member member = safeGetMember(portfolio);
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .memberId(member != null ? member.getId() : null)
                .memberName(member != null ? member.getName() : "Unknown (삭제된 사용자)")
                .templateType(portfolio.getTemplateType())
                .title(portfolio.getTitle())
                .data(portfolio.getData())
                .isPublic(portfolio.getIsPublic())
                .showContributionGraph(portfolio.getShowContributionGraph())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
}

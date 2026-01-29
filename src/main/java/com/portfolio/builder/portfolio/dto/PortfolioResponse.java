package com.portfolio.builder.portfolio.dto;

import java.time.LocalDateTime;
import java.util.Optional;

import org.hibernate.Hibernate;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.portfolio.domain.Portfolio;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {
    
    private Long id;
    private Long memberId;
    private String memberName;
    private String memberBranch;  // 회원 소속
    private String memberClassroom;  // 회원 강의실
    private String memberCohort;  // 회원 기수
    private String templateType;
    private String title;
    private String data;  // JSON string
    private Boolean isPublic;
    private Boolean showContributionGraph;
    private String contributionGraphSnapshot;  // GitHub 잔디 스냅샷 JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 좋아요 관련 필드
    private Integer likeCount;
    private Boolean isLiked;  // 현재 사용자의 좋아요 여부
    
    // AI 평가 관련 필드
    private Integer aiScore;  // AI 평가 점수 (null = 미평가)
    private String aiGrade;   // AI 등급 (S, A, B, C, null)
    
    // 퀴즈 배지 관련 필드
    private Integer badgeCount;  // 총 배지 수
    private java.util.List<String> recentBadges;  // 최근 배지 아이콘 (4개)
    
    // 대표 배지 관련 필드
    private SelectedBadgeInfo selectedBadge;  // 선택된 대표 배지 정보
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedBadgeInfo {
        private String id;
        private String icon;
        private String name;
        private String description;
    }

    /**
     * Lazy 로딩된 Member 프록시를 안전하게 초기화합니다.
     * Member가 없거나 DB에서 삭제된 경우 EntityNotFoundException을 방지합니다.
     */
    private static Member safeGetMember(Portfolio portfolio) {
        return Optional.ofNullable(portfolio.getMember())
                .filter(member -> {
                    try {
                        Hibernate.initialize(member);
                        member.getId(); // 프록시 초기화 확인
                        return true;
                    } catch (EntityNotFoundException e) {
                        return false;
                    }
                })
                .orElse(null);
    }

    public static PortfolioResponse from(Portfolio portfolio) {
        Member member = safeGetMember(portfolio);
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .memberId(member != null ? member.getId() : null)
                .memberName(member != null ? member.getName() : "Unknown (삭제된 사용자)")
                .memberBranch(member != null ? member.getBranch() : null)
                .memberClassroom(member != null ? member.getClassroom() : null)
                .memberCohort(member != null ? member.getCohort() : null)
                .templateType(portfolio.getTemplateType())
                .title(portfolio.getTitle())
                .data(portfolio.getData())
                .isPublic(portfolio.getIsPublic())
                .showContributionGraph(portfolio.getShowContributionGraph())
                .contributionGraphSnapshot(portfolio.getContributionGraphSnapshot())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .likeCount(0)
                .isLiked(false)
                .aiScore(portfolio.getAiScore())
                .aiGrade(calculateGrade(portfolio.getAiScore()))
                .badgeCount(0)
                .recentBadges(java.util.List.of())
                .build();
    }

    public static PortfolioResponse from(Portfolio portfolio, int likeCount, boolean isLiked) {
        Member member = safeGetMember(portfolio);
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .memberId(member != null ? member.getId() : null)
                .memberName(member != null ? member.getName() : "Unknown (삭제된 사용자)")
                .memberBranch(member != null ? member.getBranch() : null)
                .memberClassroom(member != null ? member.getClassroom() : null)
                .memberCohort(member != null ? member.getCohort() : null)
                .templateType(portfolio.getTemplateType())
                .title(portfolio.getTitle())
                .data(portfolio.getData())
                .isPublic(portfolio.getIsPublic())
                .showContributionGraph(portfolio.getShowContributionGraph())
                .contributionGraphSnapshot(portfolio.getContributionGraphSnapshot())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .aiScore(portfolio.getAiScore())
                .aiGrade(calculateGrade(portfolio.getAiScore()))
                .badgeCount(0)
                .recentBadges(java.util.List.of())
                .build();
    }

    public static PortfolioResponse from(Portfolio portfolio, int likeCount, boolean isLiked, int badgeCount, java.util.List<String> recentBadges) {
        return from(portfolio, likeCount, isLiked, badgeCount, recentBadges, null);
    }
    
    public static PortfolioResponse from(Portfolio portfolio, int likeCount, boolean isLiked, int badgeCount, java.util.List<String> recentBadges, SelectedBadgeInfo selectedBadge) {
        Member member = safeGetMember(portfolio);
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .memberId(member != null ? member.getId() : null)
                .memberName(member != null ? member.getName() : "Unknown (삭제된 사용자)")
                .memberBranch(member != null ? member.getBranch() : null)
                .memberClassroom(member != null ? member.getClassroom() : null)
                .memberCohort(member != null ? member.getCohort() : null)
                .templateType(portfolio.getTemplateType())
                .title(portfolio.getTitle())
                .data(portfolio.getData())
                .isPublic(portfolio.getIsPublic())
                .showContributionGraph(portfolio.getShowContributionGraph())
                .contributionGraphSnapshot(portfolio.getContributionGraphSnapshot())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .aiScore(portfolio.getAiScore())
                .aiGrade(calculateGrade(portfolio.getAiScore()))
                .badgeCount(badgeCount)
                .recentBadges(recentBadges)
                .selectedBadge(selectedBadge)
                .build();
    }
    
    /**
     * AI 점수를 등급으로 변환 (130점 만점, 85% 기준)
     */
    private static String calculateGrade(Integer score) {
        if (score == null) return null;
        if (score >= 111) return "S";  // 85%+
        if (score >= 98) return "A";   // 75%+
        if (score >= 85) return "B";   // 65%+
        if (score >= 72) return "C";   // 55%+
        if (score >= 59) return "D";   // 45%+
        return "F";
    }
}

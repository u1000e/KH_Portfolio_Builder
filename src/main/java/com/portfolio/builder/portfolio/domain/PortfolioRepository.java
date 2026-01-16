package com.portfolio.builder.portfolio.domain;

import com.portfolio.builder.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByMemberOrderByCreatedAtDesc(Member member);

    List<Portfolio> findByMember(Member member);

    List<Portfolio> findByIsPublicTrueOrderByCreatedAtDesc();

    // 공개된 포트폴리오 좋아요 순 정렬
    @Query("SELECT p FROM Portfolio p LEFT JOIN PortfolioLike pl ON p.id = pl.portfolio.id " +
           "WHERE p.isPublic = true GROUP BY p ORDER BY COUNT(pl.id) DESC, p.createdAt DESC")
    List<Portfolio> findPublicPortfoliosOrderByLikes();

    @Query("SELECT p FROM Portfolio p WHERE p.isPublic = true AND p.member.branch = :branch ORDER BY p.createdAt DESC")
    List<Portfolio> findByMemberBranchAndIsPublicTrue(@Param("branch") String branch);

    // 특정 회원의 공개된 포트폴리오
    @Query("SELECT p FROM Portfolio p WHERE p.member.id = :memberId AND p.isPublic = true ORDER BY p.createdAt DESC")
    List<Portfolio> findPublicByMemberId(@Param("memberId") Long memberId);

    long countByMember(Member member);
}

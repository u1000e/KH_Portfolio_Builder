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

    // 공개 포트폴리오 - member가 존재하는 것만 (고아 데이터 제외)
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE p.isPublic = true ORDER BY p.createdAt DESC")
    List<Portfolio> findByIsPublicTrueOrderByCreatedAtDesc();

    // 공개된 포트폴리오 좋아요 순 정렬 - member가 존재하는 것만
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m LEFT JOIN PortfolioLike pl ON p.id = pl.portfolio.id " +
           "WHERE p.isPublic = true GROUP BY p, m ORDER BY COUNT(pl.id) DESC, p.createdAt DESC")
    List<Portfolio> findPublicPortfoliosOrderByLikes();

    // 지점별 공개 포트폴리오 - member가 존재하는 것만
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE p.isPublic = true AND m.branch = :branch ORDER BY p.createdAt DESC")
    List<Portfolio> findByMemberBranchAndIsPublicTrue(@Param("branch") String branch);

    // 특정 회원의 공개된 포트폴리오
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE m.id = :memberId AND p.isPublic = true ORDER BY p.createdAt DESC")
    List<Portfolio> findPublicByMemberId(@Param("memberId") Long memberId);

    // 전체 포트폴리오 (관리자용) - member가 존재하는 것만
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m ORDER BY p.createdAt DESC")
    List<Portfolio> findAllWithMember();

    long countByMember(Member member);
}

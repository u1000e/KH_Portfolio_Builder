package com.portfolio.builder.feedback.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /**
     * 특정 포트폴리오의 피드백 목록 (최신순)
     */
    @Query("SELECT f FROM Feedback f JOIN FETCH f.member WHERE f.portfolio.id = :portfolioId ORDER BY f.createdAt DESC")
    List<Feedback> findByPortfolioIdWithMember(@Param("portfolioId") Long portfolioId);

    /**
     * 특정 포트폴리오의 피드백 개수
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * 작성자의 특정 포트폴리오에 대한 피드백 존재 여부
     */
    boolean existsByPortfolioIdAndMemberId(Long portfolioId, Long memberId);
}

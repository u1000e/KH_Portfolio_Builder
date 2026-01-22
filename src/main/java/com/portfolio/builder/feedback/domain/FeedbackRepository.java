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

    /**
     * 특정 회원의 포트폴리오에 달린 피드백 목록 (받은 피드백)
     */
    @Query("SELECT f FROM Feedback f JOIN FETCH f.member JOIN FETCH f.portfolio WHERE f.portfolio.member.id = :memberId ORDER BY f.createdAt DESC")
    List<Feedback> findReceivedFeedbacksByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원이 작성한 피드백 목록 (내가 쓴 피드백 - 운영팀/강사용)
     */
    @Query("SELECT f FROM Feedback f JOIN FETCH f.portfolio p JOIN FETCH p.member WHERE f.member.id = :memberId ORDER BY f.createdAt DESC")
    List<Feedback> findWrittenFeedbacksByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원이 작성한 피드백 삭제 (회원 탈퇴용)
     */
    void deleteAllByMemberId(Long memberId);

    /**
     * 특정 포트폴리오의 피드백 삭제 (포트폴리오 삭제용)
     */
    void deleteAllByPortfolioId(Long portfolioId);
}

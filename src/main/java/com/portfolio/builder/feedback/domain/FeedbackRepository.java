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

    /**
     * 수강생이 받은 미읽음 피드백 개수
     */
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.portfolio.member.id = :memberId AND f.isRead = false")
    long countUnreadByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 포트폴리오의 미반영 피드백 개수
     */
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.portfolio.id = :portfolioId AND (f.isResolved = false OR f.isResolved IS NULL)")
    long countUnresolvedByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 수강생이 받은 미반영 피드백 개수
     */
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.portfolio.member.id = :memberId AND (f.isResolved = false OR f.isResolved IS NULL)")
    long countUnresolvedByMemberId(@Param("memberId") Long memberId);

    /**
     * 수강생이 반영 완료한 피드백 개수
     */
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.portfolio.member.id = :memberId AND f.isResolved = true")
    int countResolvedByMemberId(@Param("memberId") Long memberId);

    /**
     * 이번 주 반영왕 (주간 피드백 반영 수 TOP 1, 수강생 기준)
     */
    @Query("""
        SELECT f.portfolio.member.id, f.portfolio.member.name, f.portfolio.member.avatarUrl,
               COUNT(f) as cnt, f.portfolio.member.githubUsername
        FROM Feedback f
        WHERE f.isResolved = true AND f.resolvedAt >= :start AND f.resolvedAt < :end
        GROUP BY f.portfolio.member.id, f.portfolio.member.name, f.portfolio.member.avatarUrl,
                 f.portfolio.member.githubUsername
        ORDER BY cnt DESC, MIN(f.resolvedAt) ASC
        """)
    List<Object[]> findWeeklyTopFeedbackResolver(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);
}

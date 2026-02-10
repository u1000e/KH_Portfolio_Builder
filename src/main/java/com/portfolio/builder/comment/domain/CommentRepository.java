package com.portfolio.builder.comment.domain;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.portfolio.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPortfolioOrderByCreatedAtAsc(Portfolio portfolio);

    @Query("SELECT c FROM Comment c JOIN FETCH c.member WHERE c.portfolio = :portfolio AND (c.isHidden = false OR c.isHidden IS NULL) ORDER BY c.createdAt ASC")
    List<Comment> findByPortfolioWithMember(@Param("portfolio") Portfolio portfolio);

    @Query("SELECT c FROM Comment c JOIN FETCH c.member JOIN FETCH c.portfolio ORDER BY c.createdAt DESC")
    List<Comment> findAllWithMemberAndPortfolio();

    /**
     * 특정 회원이 작성한 댓글 목록 (최신순)
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.portfolio WHERE c.member.id = :memberId ORDER BY c.createdAt DESC")
    List<Comment> findByMemberIdWithPortfolio(@Param("memberId") Long memberId);

    long countByPortfolio(Portfolio portfolio);

    void deleteAllByPortfolio(Portfolio portfolio);

    void deleteAllByMember(Member member);

    /**
     * 특정 회원이 받은 총 댓글 수 (모든 포트폴리오 합산, 본인 댓글 제외)
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.portfolio.member.id = :memberId AND c.member.id != :memberId AND (c.isHidden = false OR c.isHidden IS NULL)")
    int countCommentsReceivedByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원이 작성한 총 댓글 수 (본인 포트폴리오 제외)
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.member.id = :memberId AND c.portfolio.member.id != :memberId AND (c.isHidden = false OR c.isHidden IS NULL)")
    int countCommentsGivenByMemberId(@Param("memberId") Long memberId);

    // ===== 랭킹용 쿼리 =====

    /**
     * 포트폴리오 댓글 작성 랭킹 (본인 포폴 제외, 동점 시 먼저 달성한 사람 우선)
     */
    @Query("""
        SELECT c.member.id, c.member.name, c.member.avatarUrl, COUNT(c) as cnt,
               c.member.position, c.member.branch, c.member.classroom, c.member.cohort
        FROM Comment c
        WHERE c.member.id != c.portfolio.member.id AND (c.isHidden = false OR c.isHidden IS NULL)
        GROUP BY c.member.id, c.member.name, c.member.avatarUrl,
                 c.member.position, c.member.branch, c.member.classroom, c.member.cohort
        ORDER BY cnt DESC, MIN(c.createdAt) ASC
        """)
    List<Object[]> findTopByCommentCount();

    /**
     * 주간 베스트 리뷰어 후보 조회 (본인 포트폴리오 제외, 최소 3개 이상)
     * 반환: [memberId, commentCount]
     */
    @Query("""
        SELECT c.member.id, COUNT(c) as cnt
        FROM Comment c
        WHERE c.createdAt >= :startTime AND c.createdAt < :endTime
        AND c.member.id != c.portfolio.member.id AND (c.isHidden = false OR c.isHidden IS NULL)
        GROUP BY c.member.id
        HAVING COUNT(c) >= 3
        ORDER BY cnt DESC
        """)
    List<Object[]> findWeeklyReviewerCandidates(
        @Param("startTime") java.time.LocalDateTime startTime,
        @Param("endTime") java.time.LocalDateTime endTime);

    // ===== 댓글 알림용 쿼리 =====

    /**
     * 회원이 받은 읽지 않은 댓글 수 (자기 댓글 제외)
     * NULL도 읽지 않은 것으로 처리
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.portfolio.member.id = :memberId AND c.member.id != :memberId AND (c.isHidden = false OR c.isHidden IS NULL) AND (c.isRead = false OR c.isRead IS NULL)")
    long countUnreadByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 포폴의 읽지 않은 댓글들 조회
     * NULL도 읽지 않은 것으로 처리
     */
    @Query("SELECT c FROM Comment c WHERE c.portfolio.id = :portfolioId AND (c.isHidden = false OR c.isHidden IS NULL) AND (c.isRead = false OR c.isRead IS NULL)")
    List<Comment> findUnreadByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 특정 포폴의 모든 댓글 일괄 읽음 처리
     * NULL도 읽지 않은 것으로 처리
     */
    @Modifying
    @Query("UPDATE Comment c SET c.isRead = true WHERE c.portfolio.id = :portfolioId AND (c.isHidden = false OR c.isHidden IS NULL) AND (c.isRead = false OR c.isRead IS NULL)")
    int markAllAsReadByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 회원이 받은 댓글 목록 (최신순, 자기 댓글 제외)
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.member JOIN FETCH c.portfolio WHERE c.portfolio.member.id = :memberId AND c.member.id != :memberId AND (c.isHidden = false OR c.isHidden IS NULL) ORDER BY c.createdAt DESC")
    List<Comment> findReceivedByMemberId(@Param("memberId") Long memberId);
}

package com.portfolio.builder.comment.domain;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.portfolio.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPortfolioOrderByCreatedAtAsc(Portfolio portfolio);

    @Query("SELECT c FROM Comment c JOIN FETCH c.member WHERE c.portfolio = :portfolio ORDER BY c.createdAt ASC")
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
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.portfolio.member.id = :memberId AND c.member.id != :memberId")
    int countCommentsReceivedByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원이 작성한 총 댓글 수 (본인 포트폴리오 제외)
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.member.id = :memberId AND c.portfolio.member.id != :memberId")
    int countCommentsGivenByMemberId(@Param("memberId") Long memberId);
}

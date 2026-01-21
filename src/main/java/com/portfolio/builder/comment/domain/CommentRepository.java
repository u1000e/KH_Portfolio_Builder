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

    long countByPortfolio(Portfolio portfolio);

    void deleteAllByPortfolio(Portfolio portfolio);

    void deleteAllByMember(Member member);
}

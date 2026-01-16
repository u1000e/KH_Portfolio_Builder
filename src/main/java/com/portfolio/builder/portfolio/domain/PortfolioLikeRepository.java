package com.portfolio.builder.portfolio.domain;

import com.portfolio.builder.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioLikeRepository extends JpaRepository<PortfolioLike, Long> {

    Optional<PortfolioLike> findByPortfolioAndMember(Portfolio portfolio, Member member);

    boolean existsByPortfolioAndMember(Portfolio portfolio, Member member);

    @Query("SELECT COUNT(pl) FROM PortfolioLike pl WHERE pl.portfolio.id = :portfolioId")
    int countByPortfolioId(@Param("portfolioId") Long portfolioId);

    void deleteByPortfolioAndMember(Portfolio portfolio, Member member);

    void deleteAllByPortfolio(Portfolio portfolio);

    void deleteAllByMember(Member member);
}

package com.portfolio.builder.portfolio.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TroubleshootingRepository extends JpaRepository<Troubleshooting, Long> {
    
    List<Troubleshooting> findByPortfolioIdOrderByCreatedAtDesc(Long portfolioId);
    
    int countByPortfolioId(Long portfolioId);
    
    @Query("SELECT t FROM Troubleshooting t JOIN FETCH t.portfolio WHERE t.portfolio.id = :portfolioId ORDER BY t.createdAt DESC")
    List<Troubleshooting> findByPortfolioIdWithPortfolio(@Param("portfolioId") Long portfolioId);
    
    void deleteAllByPortfolio(Portfolio portfolio);
}

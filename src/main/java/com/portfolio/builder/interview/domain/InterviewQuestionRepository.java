package com.portfolio.builder.interview.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    // 전체 조회 (최신순)
    List<InterviewQuestion> findAllByOrderByCreatedAtDesc();

    // 기간으로 조회
    List<InterviewQuestion> findByPeriodOrderByCreatedAtDesc(String period);

    // 카테고리로 조회
    List<InterviewQuestion> findByCategoryOrderByCreatedAtDesc(String category);

    // 기간 + 카테고리로 조회
    List<InterviewQuestion> findByPeriodAndCategoryOrderByCreatedAtDesc(String period, String category);

    // 여러 기간으로 조회
    List<InterviewQuestion> findByPeriodInOrderByCategoryAscCreatedAtDesc(List<String> periods);

    // 여러 카테고리로 조회
    List<InterviewQuestion> findByCategoryInOrderByCategoryAscCreatedAtDesc(List<String> categories);

    // 여러 기간 + 여러 카테고리로 조회
    List<InterviewQuestion> findByPeriodInAndCategoryInOrderByCategoryAscCreatedAtDesc(
            List<String> periods, List<String> categories);

    // 존재하는 기간 목록 조회 (중복 제거, 최신순)
    @Query("SELECT DISTINCT q.period FROM InterviewQuestion q ORDER BY q.period DESC")
    List<String> findDistinctPeriods();

    // 존재하는 카테고리 목록 조회 (중복 제거)
    @Query("SELECT DISTINCT q.category FROM InterviewQuestion q ORDER BY q.category ASC")
    List<String> findDistinctCategories();

    // 카테고리별 질문 수
    @Query("SELECT q.category, COUNT(q) FROM InterviewQuestion q WHERE q.period IN :periods GROUP BY q.category ORDER BY q.category")
    List<Object[]> countByCategoryAndPeriods(@Param("periods") List<String> periods);
}

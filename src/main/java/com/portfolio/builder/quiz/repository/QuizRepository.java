package com.portfolio.builder.quiz.repository;

import com.portfolio.builder.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // 카테고리별 문제 조회
    List<Quiz> findByCategory(String category);

    // 카테고리 목록 조회
    @Query("SELECT DISTINCT q.category FROM Quiz q ORDER BY q.category")
    List<String> findAllCategories();

    // 카테고리별 랜덤 문제 조회 (Oracle)
    @Query(value = "SELECT * FROM (SELECT * FROM TB_QUIZ WHERE category = :category ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= :limit", nativeQuery = true)
    List<Quiz> findRandomByCategory(@Param("category") String category, @Param("limit") int limit);

    // 사용자가 아직 풀지 않은 문제 중 랜덤 조회 (Oracle)
    @Query(value = """
        SELECT * FROM (
            SELECT q.* FROM TB_QUIZ q 
            WHERE q.category = :category 
            AND q.id NOT IN (
                SELECT qa.quiz_id FROM TB_QUIZ_ATTEMPT qa 
                WHERE qa.member_id = :memberId
            )
            ORDER BY DBMS_RANDOM.VALUE
        ) WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Quiz> findUnsolvedRandomByCategory(
            @Param("category") String category, 
            @Param("memberId") Long memberId, 
            @Param("limit") int limit);

    // 카테고리별 문제 수
    Long countByCategory(String category);
}

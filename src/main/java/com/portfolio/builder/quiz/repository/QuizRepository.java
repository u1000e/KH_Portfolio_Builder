package com.portfolio.builder.quiz.repository;

import com.portfolio.builder.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // 카테고리별 문제 조회
    List<Quiz> findByCategory(String category);

    // 카테고리별 + 퀴즈타입별 문제 조회
    List<Quiz> findByCategoryAndQuizType(String category, String quizType);

    // 카테고리 목록 조회 (면접 대비용 - 기본)
    @Query("SELECT DISTINCT q.category FROM Quiz q WHERE q.quizType = 'INTERVIEW' ORDER BY q.category")
    List<String> findAllCategories();

    // 카테고리 목록 조회 (퀴즈타입별)
    @Query("SELECT DISTINCT q.category FROM Quiz q WHERE q.quizType = :quizType ORDER BY q.category")
    List<String> findAllCategoriesByQuizType(@Param("quizType") String quizType);

    // 카테고리별 랜덤 문제 조회 (Oracle) - 면접 대비용
    @Query(value = "SELECT * FROM (SELECT * FROM TB_QUIZ WHERE category = :category AND quiz_type = 'INTERVIEW' ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= :limit", nativeQuery = true)
    List<Quiz> findRandomByCategory(@Param("category") String category, @Param("limit") int limit);

    // 카테고리별 랜덤 문제 조회 (Oracle) - 퀴즈타입별
    @Query(value = "SELECT * FROM (SELECT * FROM TB_QUIZ WHERE category = :category AND quiz_type = :quizType ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= :limit", nativeQuery = true)
    List<Quiz> findRandomByCategoryAndQuizType(@Param("category") String category, @Param("quizType") String quizType, @Param("limit") int limit);

    // 사용자가 아직 풀지 않은 문제 중 랜덤 조회 (Oracle) - 면접 대비용
    @Query(value = """
        SELECT * FROM (
            SELECT q.* FROM TB_QUIZ q 
            WHERE q.category = :category 
            AND q.quiz_type = 'INTERVIEW'
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

    // 사용자가 아직 풀지 않은 문제 중 랜덤 조회 (Oracle) - 수업 복습용
    @Query(value = """
        SELECT * FROM (
            SELECT q.* FROM TB_QUIZ q 
            WHERE q.category = :category 
            AND q.quiz_type = :quizType
            AND q.id NOT IN (
                SELECT qa.quiz_id FROM TB_QUIZ_ATTEMPT qa 
                WHERE qa.member_id = :memberId AND qa.quiz_type = :quizType
            )
            ORDER BY DBMS_RANDOM.VALUE
        ) WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Quiz> findUnsolvedRandomByCategoryAndQuizType(
            @Param("category") String category, 
            @Param("quizType") String quizType,
            @Param("memberId") Long memberId, 
            @Param("limit") int limit);

    // 카테고리별 문제 수 (면접 대비용)
    Long countByCategory(String category);

    // 카테고리별 + 퀴즈타입별 문제 수
    Long countByCategoryAndQuizType(String category, String quizType);
}

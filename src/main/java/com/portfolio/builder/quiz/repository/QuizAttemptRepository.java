package com.portfolio.builder.quiz.repository;

import com.portfolio.builder.quiz.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    // 특정 날짜에 사용자가 푼 문제 수 (전체)
    Long countByMemberIdAndAttemptDate(Long memberId, LocalDate attemptDate);
    
    // 특정 날짜에 사용자가 푼 문제 수 (복습 모드 제외 - 일일 제한용)
    Long countByMemberIdAndAttemptDateAndIsReviewModeFalse(Long memberId, LocalDate attemptDate);

    // 특정 날짜에 사용자가 푼 문제 목록
    List<QuizAttempt> findByMemberIdAndAttemptDate(Long memberId, LocalDate attemptDate);

    // 사용자가 특정 카테고리에서 푼 고유 문제 수 (복습 모드 제외)
    @Query("SELECT COUNT(DISTINCT qa.quiz.id) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.quiz.category = :category AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL)")
    Long countByMemberIdAndCategory(@Param("memberId") Long memberId, @Param("category") String category);

    // 사용자가 특정 카테고리에서 맞은 고유 문제 수 (복습 모드 제외)
    @Query("SELECT COUNT(DISTINCT qa.quiz.id) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.quiz.category = :category AND qa.isCorrect = true AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL)")
    Long countCorrectByMemberIdAndCategory(@Param("memberId") Long memberId, @Param("category") String category);

    // 사용자가 틀린 문제 목록 (오답 노트용)
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.isCorrect = false ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findWrongAnswersByMemberId(@Param("memberId") Long memberId);

    // 특정 날짜에 사용자가 푼 문제 ID 목록
    @Query("SELECT qa.quiz.id FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.attemptDate = :date")
    List<Long> findQuizIdsByMemberIdAndDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);

    // 카테고리별 오답 수 조회
    @Query("SELECT qa.quiz.category, COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.isCorrect = false GROUP BY qa.quiz.category")
    List<Object[]> countWrongByMemberIdGroupByCategory(@Param("memberId") Long memberId);

    // 특정 카테고리의 오답 목록
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.isCorrect = false AND qa.quiz.category = :category ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findWrongAnswersByMemberIdAndCategory(@Param("memberId") Long memberId, @Param("category") String category);

    // 사용자가 특정 퀴즈를 풀었는지 확인
    boolean existsByMemberIdAndQuizId(Long memberId, Long quizId);

    // 사용자가 푼 모든 문제 (복습용) - 카테고리 필터 가능
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findAllSolvedByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.quiz.category = :category ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findAllSolvedByMemberIdAndCategory(@Param("memberId") Long memberId, @Param("category") String category);

    // 사용자가 맞은 문제만 (복습용)
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.isCorrect = true ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findCorrectAnswersByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.isCorrect = true AND qa.quiz.category = :category ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findCorrectAnswersByMemberIdAndCategory(@Param("memberId") Long memberId, @Param("category") String category);

    // 카테고리별 푼 문제 수 조회 (복습 모드 UI용)
    @Query("SELECT qa.quiz.category, COUNT(DISTINCT qa.quiz.id) FROM QuizAttempt qa WHERE qa.member.id = :memberId GROUP BY qa.quiz.category")
    List<Object[]> countSolvedByMemberIdGroupByCategory(@Param("memberId") Long memberId);

    // 복습 모드로 푼 문제 수 (배지용)
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.isReviewMode = true")
    Long countReviewModeByMemberId(@Param("memberId") Long memberId);

    // 특정 날짜에 사용자가 맞은 문제 수 (복습 모드 제외 - 완벽한 하루 배지용)
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.attemptDate = :date AND qa.isCorrect = true AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL)")
    Long countTodayCorrectByMemberId(@Param("memberId") Long memberId, @Param("date") LocalDate date);
}

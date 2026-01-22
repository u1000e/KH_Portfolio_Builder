package com.portfolio.builder.quiz.repository;

import com.portfolio.builder.quiz.domain.QuizStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizStreakRepository extends JpaRepository<QuizStreak, Long> {

    Optional<QuizStreak> findByMemberId(Long memberId);

    // 스트릭 랭킹 (연속 학습일 기준) - 수강생만
    @Query("SELECT qs FROM QuizStreak qs JOIN FETCH qs.member m WHERE m.position = '수강생' ORDER BY qs.currentStreak DESC")
    List<QuizStreak> findTopByCurrentStreak();

    // 총 문제 수 랭킹 - 수강생만
    @Query("SELECT qs FROM QuizStreak qs JOIN FETCH qs.member m WHERE m.position = '수강생' ORDER BY qs.totalQuizCount DESC")
    List<QuizStreak> findTopByTotalQuizCount();

    // 정답률 랭킹 (최소 10문제 이상 푼 사람) - 수강생만
    @Query("""
        SELECT qs FROM QuizStreak qs JOIN FETCH qs.member m
        WHERE qs.totalQuizCount >= :minQuizCount AND m.position = '수강생'
        ORDER BY (qs.correctCount * 1.0 / qs.totalQuizCount) DESC
        """)
    List<QuizStreak> findTopByAccuracy(@Param("minQuizCount") int minQuizCount);
}

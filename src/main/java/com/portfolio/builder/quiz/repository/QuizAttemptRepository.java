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

    // 특정 날짜에 사용자가 푼 문제 수 (퀴즈타입별, 복습 모드 제외 - 수업 복습 일일 통계용)
    Long countByMemberIdAndAttemptDateAndQuizTypeAndIsReviewModeFalse(Long memberId, LocalDate attemptDate, String quizType);

    // 특정 날짜에 사용자가 푼 문제 목록
    List<QuizAttempt> findByMemberIdAndAttemptDate(Long memberId, LocalDate attemptDate);

    // 사용자가 특정 카테고리에서 푼 고유 문제 수 (복습 모드 제외)
    @Query("SELECT COUNT(DISTINCT qa.quiz.id) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.quiz.category = :category AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL)")
    Long countByMemberIdAndCategory(@Param("memberId") Long memberId, @Param("category") String category);

    // 사용자가 특정 카테고리에서 맞은 고유 문제 수 (복습 모드 제외)
    @Query("SELECT COUNT(DISTINCT qa.quiz.id) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.quiz.category = :category AND qa.isCorrect = true AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL)")
    Long countCorrectByMemberIdAndCategory(@Param("memberId") Long memberId, @Param("category") String category);

    // 사용자가 틀린 문제 목록 (오답 노트용 - 복습 모드 제외, 면접 대비용 기본)
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.isCorrect = false AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL) AND qa.quizType = 'INTERVIEW' ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findWrongAnswersByMemberId(@Param("memberId") Long memberId);

    // 사용자가 틀린 문제 목록 (오답 노트용 - 퀴즈타입별)
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.isCorrect = false AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL) AND qa.quizType = :quizType ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findWrongAnswersByMemberIdAndQuizType(@Param("memberId") Long memberId, @Param("quizType") String quizType);

    // 특정 날짜에 사용자가 푼 문제 ID 목록
    @Query("SELECT qa.quiz.id FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.attemptDate = :date")
    List<Long> findQuizIdsByMemberIdAndDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);

    // 카테고리별 오답 수 조회 (복습 모드 제외, 면접 대비용 기본)
    @Query("SELECT qa.quiz.category, COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.isCorrect = false AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL) AND qa.quizType = 'INTERVIEW' GROUP BY qa.quiz.category")
    List<Object[]> countWrongByMemberIdGroupByCategory(@Param("memberId") Long memberId);

    // 카테고리별 오답 수 조회 (퀴즈타입별)
    @Query("SELECT qa.quiz.category, COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.isCorrect = false AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL) AND qa.quizType = :quizType GROUP BY qa.quiz.category")
    List<Object[]> countWrongByMemberIdGroupByCategoryAndQuizType(@Param("memberId") Long memberId, @Param("quizType") String quizType);

    // 특정 카테고리의 오답 목록 (복습 모드 제외, 면접 대비용 기본)
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.isCorrect = false AND qa.quiz.category = :category AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL) AND qa.quizType = 'INTERVIEW' ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findWrongAnswersByMemberIdAndCategory(@Param("memberId") Long memberId, @Param("category") String category);

    // 특정 카테고리의 오답 목록 (퀴즈타입별)
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz WHERE qa.member.id = :memberId AND qa.isCorrect = false AND qa.quiz.category = :category AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL) AND qa.quizType = :quizType ORDER BY qa.createdAt DESC")
    List<QuizAttempt> findWrongAnswersByMemberIdAndCategoryAndQuizType(@Param("memberId") Long memberId, @Param("category") String category, @Param("quizType") String quizType);

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

    // 복습 모드로 푼 문제 수 (배지용) - 오답노트 복습
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.isReviewMode = true")
    Long countReviewModeByMemberId(@Param("memberId") Long memberId);
    
    // 수업 복습(PRACTICE) 모드로 푼 문제 수 (배지용)
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.quizType = 'PRACTICE' AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL)")
    Long countPracticeModeByMemberId(@Param("memberId") Long memberId);
    
    // 전체 복습 문제 수 (배지용) - 수업복습(PRACTICE) + 면접대비 복습모드(isReviewMode=true) 합산
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND (qa.quizType = 'PRACTICE' OR qa.isReviewMode = true)")
    Long countAllReviewByMemberId(@Param("memberId") Long memberId);

    // 복습 횟수 랜킹 (복습 횟수 내림차순)
    @Query("""
        SELECT qa.member.id, qa.member.name, qa.member.avatarUrl, COUNT(qa) as reviewCount,
               qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        FROM QuizAttempt qa
        WHERE qa.isReviewMode = true
        GROUP BY qa.member.id, qa.member.name, qa.member.avatarUrl,
                 qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        ORDER BY reviewCount DESC
        """)
    List<Object[]> findTopByReviewCount();

    // 특정 날짜에 사용자가 맞은 문제 수 (복습 모드 제외 - 완벽한 하루 배지용)
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.member.id = :memberId AND qa.attemptDate = :date AND qa.isCorrect = true AND (qa.isReviewMode = false OR qa.isReviewMode IS NULL)")
    Long countTodayCorrectByMemberId(@Param("memberId") Long memberId, @Param("date") LocalDate date);

    // ===== 랭킹용 쿼리 =====

    // 🌅 얼리버드 랭킹 (아침 6~9시 풀이 횟수)
    @Query("""
        SELECT qa.member.id, qa.member.name, qa.member.avatarUrl, COUNT(qa) as earlyCount,
               qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        FROM QuizAttempt qa
        WHERE EXTRACT(HOUR FROM qa.createdAt) >= 6 AND EXTRACT(HOUR FROM qa.createdAt) < 9
        GROUP BY qa.member.id, qa.member.name, qa.member.avatarUrl,
                 qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        ORDER BY earlyCount DESC
        """)
    List<Object[]> findTopByEarlyBird();

    // 🦉 올빼미 랭킹 (밤 22시~새벽 2시 풀이 횟수)
    @Query("""
        SELECT qa.member.id, qa.member.name, qa.member.avatarUrl, COUNT(qa) as nightCount,
               qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        FROM QuizAttempt qa
        WHERE (EXTRACT(HOUR FROM qa.createdAt) >= 22 OR EXTRACT(HOUR FROM qa.createdAt) < 2)
        GROUP BY qa.member.id, qa.member.name, qa.member.avatarUrl,
                 qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        ORDER BY nightCount DESC
        """)
    List<Object[]> findTopByNightOwl();

    // 🔥 오늘의 챔피언 (오늘 풀이 횟수 - 학습+복습)
    @Query("""
        SELECT qa.member.id, qa.member.name, qa.member.avatarUrl, COUNT(qa) as todayCount,
               qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        FROM QuizAttempt qa
        WHERE qa.attemptDate = :today
        GROUP BY qa.member.id, qa.member.name, qa.member.avatarUrl,
                 qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        ORDER BY todayCount DESC
        """)
    List<Object[]> findTopByToday(@Param("today") LocalDate today);

    // 📅 이번 주 MVP (이번 주 풀이 횟수 - 학습+복습)
    @Query("""
        SELECT qa.member.id, qa.member.name, qa.member.avatarUrl, COUNT(qa) as weekCount,
               qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        FROM QuizAttempt qa
        WHERE qa.attemptDate >= :weekStart AND qa.attemptDate <= :weekEnd
        GROUP BY qa.member.id, qa.member.name, qa.member.avatarUrl,
                 qa.member.position, qa.member.branch, qa.member.classroom, qa.member.cohort
        ORDER BY weekCount DESC
        """)
    List<Object[]> findTopByThisWeek(@Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);

    // 📆 학습 캘린더 히트맵용 - 날짜별 퀴즈 풀이 횟수
    @Query("""
        SELECT qa.attemptDate, COUNT(qa)
        FROM QuizAttempt qa
        WHERE qa.member.id = :memberId
        AND qa.attemptDate >= :startDate
        AND qa.attemptDate <= :endDate
        GROUP BY qa.attemptDate
        ORDER BY qa.attemptDate
        """)
    List<Object[]> findDailyCountsByMemberIdBetween(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}

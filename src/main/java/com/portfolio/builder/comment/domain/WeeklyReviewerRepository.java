package com.portfolio.builder.comment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReviewerRepository extends JpaRepository<WeeklyReviewer, Long> {

    /**
     * 특정 주간의 수상자 목록 조회 (등수 순)
     */
    List<WeeklyReviewer> findByWeekEndDateOrderByRankPositionAsc(LocalDate weekEndDate);

    /**
     * 가장 최근 주간 리뷰어 조회 (현재 주간 베스트 리뷰어 표시용)
     */
    @Query("""
        SELECT wr FROM WeeklyReviewer wr
        JOIN FETCH wr.member
        WHERE wr.weekEndDate = (SELECT MAX(wr2.weekEndDate) FROM WeeklyReviewer wr2)
        ORDER BY wr.rankPosition ASC
        """)
    List<WeeklyReviewer> findLatestWeeklyReviewers();

    /**
     * 특정 회원의 수상 이력 조회
     */
    List<WeeklyReviewer> findByMemberIdOrderByWeekEndDateDesc(Long memberId);

    /**
     * 특정 주간에 특정 회원이 이미 수상했는지 확인
     */
    boolean existsByMemberIdAndWeekEndDate(Long memberId, LocalDate weekEndDate);

    /**
     * 특정 회원이 특정 순위로 수상한 적이 있는지 확인
     */
    boolean existsByMemberIdAndRankPosition(Long memberId, int rankPosition);

    /**
     * 특정 주간 수상자 존재 여부 확인
     */
    boolean existsByWeekEndDate(LocalDate weekEndDate);
}

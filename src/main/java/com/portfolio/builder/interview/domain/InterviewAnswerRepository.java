package com.portfolio.builder.interview.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    /**
     * 특정 질문의 답변 목록 (좋아요순, 페이징)
     */
    @Query("SELECT a FROM InterviewAnswer a WHERE a.question.id = :questionId AND (a.isHidden = false OR a.isHidden IS NULL) ORDER BY a.likeCount DESC, a.createdAt DESC")
    Page<InterviewAnswer> findByQuestionIdOrderByLikeCountDescCreatedAtDesc(@Param("questionId") Long questionId, Pageable pageable);

    /**
     * 특정 질문의 답변 목록 (오래된순, 페이징)
     */
    @Query("SELECT a FROM InterviewAnswer a WHERE a.question.id = :questionId AND (a.isHidden = false OR a.isHidden IS NULL) ORDER BY a.createdAt ASC")
    Page<InterviewAnswer> findByQuestionIdOrderByCreatedAtAsc(@Param("questionId") Long questionId, Pageable pageable);

    /**
     * 특정 질문의 답변 목록 (특정 ID 제외, 오래된순, 페이징)
     */
    @Query("SELECT a FROM InterviewAnswer a WHERE a.question.id = :questionId AND a.id NOT IN :excludeIds AND (a.isHidden = false OR a.isHidden IS NULL) ORDER BY a.createdAt ASC")
    Page<InterviewAnswer> findByQuestionIdAndIdNotInOrderByCreatedAtAsc(@Param("questionId") Long questionId, @Param("excludeIds") List<Long> excludeIds, Pageable pageable);

    /**
     * 특정 질문의 답변 목록 (상위 3개 좋아요순 우선, 나머지 오래된순)
     */
    @Query("""
        SELECT a FROM InterviewAnswer a
        WHERE a.question.id = :questionId AND (a.isHidden = false OR a.isHidden IS NULL)
        ORDER BY
            CASE WHEN a.id IN :top3Ids THEN 0 ELSE 1 END,
            CASE WHEN a.id IN :top3Ids THEN a.likeCount ELSE 0 END DESC,
            CASE WHEN a.id NOT IN :top3Ids THEN a.createdAt END ASC
        """)
    Page<InterviewAnswer> findByQuestionIdWithTop3First(
            @Param("questionId") Long questionId,
            @Param("top3Ids") List<Long> top3Ids,
            Pageable pageable);

    /**
     * 특정 질문의 답변 개수
     */
    @Query("SELECT COUNT(a) FROM InterviewAnswer a WHERE a.question.id = :questionId AND (a.isHidden = false OR a.isHidden IS NULL)")
    long countByQuestionId(@Param("questionId") Long questionId);

    /**
     * 특정 회원의 답변 개수
     */
    @Query("SELECT COUNT(a) FROM InterviewAnswer a WHERE a.member.id = :memberId AND (a.isHidden = false OR a.isHidden IS NULL)")
    long countByMemberId(@Param("memberId") Long memberId);

    /**
     * 좋아요 10개 이상 답변 중 상위 3개 (베스트 답변자 선정용)
     * 반환: [answerId, memberId, likeCount]
     */
    @Query("""
        SELECT a.id, a.member.id, a.likeCount
        FROM InterviewAnswer a
        WHERE a.likeCount >= 10 AND (a.isHidden = false OR a.isHidden IS NULL)
        ORDER BY a.likeCount DESC
        """)
    List<Object[]> findTopAnswersByLikeCount();

    /**
     * 특정 회원이 특정 질문에 이미 답변했는지 확인
     */
    boolean existsByQuestionIdAndMemberId(Long questionId, Long memberId);

    /**
     * 특정 질문의 좋아요 상위 3개 답변 ID 조회
     */
    @Query("""
        SELECT a.id FROM InterviewAnswer a
        WHERE a.question.id = :questionId AND (a.isHidden = false OR a.isHidden IS NULL)
        ORDER BY a.likeCount DESC, a.createdAt ASC
        """)
    List<Long> findTop3AnswerIdsByQuestionId(@Param("questionId") Long questionId, Pageable pageable);

    /**
     * 일주일 내 답변이 많이 달린 질문 조회 (핫한 토론)
     * 반환: [questionId, answerCount]
     */
    @Query("""
        SELECT a.question.id, COUNT(a) as cnt
        FROM InterviewAnswer a
        WHERE a.createdAt >= :since AND (a.isHidden = false OR a.isHidden IS NULL)
        GROUP BY a.question.id
        ORDER BY cnt DESC
        """)
    List<Object[]> findHotQuestionIds(@Param("since") java.time.LocalDateTime since, Pageable pageable);

    /**
     * 특정 회원이 작성한 답변 목록 (최신순, 페이징)
     */
    Page<InterviewAnswer> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 특정 회원이 작성한 답변 목록 (카테고리/키워드 필터, 최신순, 페이징)
     */
    @Query("""
        SELECT a FROM InterviewAnswer a
        WHERE a.member.id = :memberId
        AND (:category IS NULL OR a.question.category = :category)
        AND (:keyword IS NULL OR a.content LIKE %:keyword% OR a.question.question LIKE %:keyword%)
        ORDER BY a.createdAt DESC
        """)
    Page<InterviewAnswer> findByMemberIdWithFilters(
            @Param("memberId") Long memberId,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 특정 회원이 답변한 질문 ID 목록
     */
    @Query("SELECT DISTINCT a.question.id FROM InterviewAnswer a WHERE a.member.id = :memberId")
    List<Long> findAnsweredQuestionIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원이 작성한 답변 ID 목록 (회원 삭제용)
     */
    @Query("SELECT a.id FROM InterviewAnswer a WHERE a.member.id = :memberId")
    List<Long> findIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원 삭제용
     */
    void deleteAllByMemberId(Long memberId);

    /**
     * 특정 회원이 받은 총 좋아요 수 (모든 답변의 likeCount 합계)
     */
    @Query("SELECT COALESCE(SUM(a.likeCount), 0) FROM InterviewAnswer a WHERE a.member.id = :memberId AND (a.isHidden = false OR a.isHidden IS NULL)")
    int sumLikeCountByMemberId(@Param("memberId") Long memberId);

    // ===== 랭킹용 쿼리 =====

    /**
     * 답변 작성횟수 랭킹 (동점 시 먼저 달성한 사람 우선)
     */
    @Query("""
        SELECT a.member.id, a.member.name, a.member.avatarUrl, COUNT(a) as cnt,
               a.member.position, a.member.branch, a.member.classroom, a.member.cohort
        FROM InterviewAnswer a
        WHERE (a.isHidden = false OR a.isHidden IS NULL)
        GROUP BY a.member.id, a.member.name, a.member.avatarUrl,
                 a.member.position, a.member.branch, a.member.classroom, a.member.cohort
        ORDER BY cnt DESC, MIN(a.createdAt) ASC
        """)
    List<Object[]> findTopByAnswerCount();

    /**
     * 좋아요 받은 총 횟수 랭킹 (동점 시 먼저 달성한 사람 우선)
     */
    @Query("""
        SELECT a.member.id, a.member.name, a.member.avatarUrl, SUM(a.likeCount) as totalLikes,
               a.member.position, a.member.branch, a.member.classroom, a.member.cohort
        FROM InterviewAnswer a
        WHERE (a.isHidden = false OR a.isHidden IS NULL)
        GROUP BY a.member.id, a.member.name, a.member.avatarUrl,
                 a.member.position, a.member.branch, a.member.classroom, a.member.cohort
        HAVING SUM(a.likeCount) > 0
        ORDER BY totalLikes DESC, MIN(a.createdAt) ASC
        """)
    List<Object[]> findTopByTotalLikes();

    /**
     * 이번 주 토론왕 (주간 답변 작성 수 TOP 1)
     */
    @Query("""
        SELECT a.member.id, a.member.name, a.member.avatarUrl, COUNT(a) as cnt,
               a.member.githubUsername
        FROM InterviewAnswer a
        WHERE a.createdAt >= :start AND a.createdAt < :end AND (a.isHidden = false OR a.isHidden IS NULL)
        GROUP BY a.member.id, a.member.name, a.member.avatarUrl, a.member.githubUsername
        ORDER BY cnt DESC, MIN(a.createdAt) ASC
        """)
    List<Object[]> findWeeklyTopAnswerer(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);

    /**
     * 답변이 가장 많이 달린 질문 조회 (전체 기간)
     * 반환: [questionId, answerCount]
     */
    @Query("""
        SELECT a.question.id, COUNT(a) as cnt
        FROM InterviewAnswer a
        WHERE (a.isHidden = false OR a.isHidden IS NULL)
        GROUP BY a.question.id
        ORDER BY cnt DESC
        """)
    List<Object[]> findTopQuestionsByAnswerCount(Pageable pageable);

    // 관리자용: 전체 면접답변 조회
    @Query("SELECT a FROM InterviewAnswer a JOIN FETCH a.member JOIN FETCH a.question ORDER BY a.createdAt DESC")
    List<InterviewAnswer> findAllWithMemberAndQuestion();

    // 강사용: 반별 면접토론 통계
    @Query("SELECT m.id, m.name, m.avatarUrl, m.githubUsername, " +
           "COUNT(a) as answerCount, COALESCE(SUM(a.likeCount), 0) as totalLikes, " +
           "MAX(a.createdAt) as lastAnswerDate " +
           "FROM InterviewAnswer a JOIN a.member m " +
           "WHERE (a.isHidden = false OR a.isHidden IS NULL) " +
           "AND (:branch IS NULL OR m.branch = :branch) " +
           "AND (:classroom IS NULL OR m.classroom = :classroom) " +
           "AND (:cohort IS NULL OR m.cohort = :cohort) " +
           "GROUP BY m.id, m.name, m.avatarUrl, m.githubUsername " +
           "ORDER BY answerCount DESC")
    List<Object[]> findInterviewStatsByClass(@Param("branch") String branch,
            @Param("classroom") String classroom, @Param("cohort") String cohort);
}

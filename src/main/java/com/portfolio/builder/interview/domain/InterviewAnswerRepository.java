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
    Page<InterviewAnswer> findByQuestionIdOrderByLikeCountDescCreatedAtDesc(Long questionId, Pageable pageable);

    /**
     * 특정 질문의 답변 개수
     */
    long countByQuestionId(Long questionId);

    /**
     * 특정 회원의 답변 개수
     */
    long countByMemberId(Long memberId);

    /**
     * 좋아요 10개 이상 답변 중 상위 3개 (베스트 답변자 선정용)
     * 반환: [answerId, memberId, likeCount]
     */
    @Query("""
        SELECT a.id, a.member.id, a.likeCount
        FROM InterviewAnswer a
        WHERE a.likeCount >= 10
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
        WHERE a.question.id = :questionId
        ORDER BY a.likeCount DESC, a.createdAt ASC
        """)
    List<Long> findTop3AnswerIdsByQuestionId(@Param("questionId") Long questionId, Pageable pageable);

    /**
     * 24시간 내 답변이 많이 달린 질문 조회 (핫한 토론)
     * 반환: [questionId, answerCount]
     */
    @Query("""
        SELECT a.question.id, COUNT(a) as cnt
        FROM InterviewAnswer a
        WHERE a.createdAt >= :since
        GROUP BY a.question.id
        ORDER BY cnt DESC
        """)
    List<Object[]> findHotQuestionIds(@Param("since") java.time.LocalDateTime since, Pageable pageable);
}

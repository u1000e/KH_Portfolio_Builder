package com.portfolio.builder.interview.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewAnswerLikeRepository extends JpaRepository<InterviewAnswerLike, Long> {

    /**
     * 특정 답변에 특정 회원이 좋아요 했는지 확인
     */
    boolean existsByAnswerIdAndMemberId(Long answerId, Long memberId);

    /**
     * 특정 답변의 특정 회원 좋아요 조회
     */
    Optional<InterviewAnswerLike> findByAnswerIdAndMemberId(Long answerId, Long memberId);

    /**
     * 특정 답변의 좋아요 삭제
     */
    void deleteByAnswerIdAndMemberId(Long answerId, Long memberId);

    /**
     * 특정 답변의 모든 좋아요 삭제
     */
    void deleteAllByAnswerId(Long answerId);

    /**
     * 특정 회원이 좋아요한 답변 목록 (최신순, 페이징)
     */
    Page<InterviewAnswerLike> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 특정 회원이 좋아요한 답변 목록 (카테고리/키워드 필터, 최신순, 페이징)
     */
    @Query("""
        SELECT l FROM InterviewAnswerLike l
        WHERE l.member.id = :memberId
        AND (:category IS NULL OR l.answer.question.category = :category)
        AND (:keyword IS NULL OR l.answer.content LIKE %:keyword% OR l.answer.question.question LIKE %:keyword%)
        ORDER BY l.createdAt DESC
        """)
    Page<InterviewAnswerLike> findByMemberIdWithFilters(
            @Param("memberId") Long memberId,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 회원 삭제용 - 해당 회원이 누른 좋아요 삭제
     */
    void deleteAllByMemberId(Long memberId);

    /**
     * 회원 삭제용 - 해당 회원의 답변에 달린 좋아요 삭제
     */
    void deleteAllByAnswerIdIn(List<Long> answerIds);
}

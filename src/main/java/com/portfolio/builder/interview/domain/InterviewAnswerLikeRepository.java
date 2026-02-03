package com.portfolio.builder.interview.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

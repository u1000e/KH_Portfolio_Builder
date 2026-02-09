package com.portfolio.builder.til.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TILLikeRepository extends JpaRepository<TILLike, Long> {

    Optional<TILLike> findByTilIdAndMemberId(Long tilId, Long memberId);

    boolean existsByTilIdAndMemberId(Long tilId, Long memberId);

    void deleteByTilIdAndMemberId(Long tilId, Long memberId);

    void deleteAllByTilId(Long tilId);
}

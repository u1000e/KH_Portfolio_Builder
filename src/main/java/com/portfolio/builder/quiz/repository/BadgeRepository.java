package com.portfolio.builder.quiz.repository;

import com.portfolio.builder.quiz.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    // 사용자의 모든 배지 조회
    List<Badge> findByMemberIdOrderByEarnedAtDesc(Long memberId);

    // 특정 배지 보유 여부
    boolean existsByMemberIdAndBadgeId(Long memberId, String badgeId);

    // 특정 배지 조회
    Optional<Badge> findByMemberIdAndBadgeId(Long memberId, String badgeId);

    // 최근 획득 배지 (N개)
    List<Badge> findTop5ByMemberIdOrderByEarnedAtDesc(Long memberId);
    
    // 최근 획득 배지 (4개 - 갤러리용)
    List<Badge> findTop4ByMemberIdOrderByEarnedAtDesc(Long memberId);

    // 사용자의 배지 개수
    long countByMemberId(Long memberId);
}

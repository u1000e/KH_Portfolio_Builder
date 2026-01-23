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

    // 👑 배지 컬렉터 랭킹 (배지 개수 내림차순)
    @Query("""
        SELECT b.member.id, b.member.name, b.member.avatarUrl, COUNT(b) as badgeCount,
               b.member.position, b.member.branch, b.member.classroom, b.member.cohort
        FROM Badge b
        GROUP BY b.member.id, b.member.name, b.member.avatarUrl,
                 b.member.position, b.member.branch, b.member.classroom, b.member.cohort
        ORDER BY badgeCount DESC
        """)
    List<Object[]> findTopByBadgeCount();

    // 배지별 획득자 수 (희귀 배지 계산용)
    @Query("SELECT b.badgeId, COUNT(b) FROM Badge b GROUP BY b.badgeId")
    List<Object[]> countByBadgeIdGrouped();

    // 특정 배지 보유자 목록 (희귀 배지 랭킹용)
    @Query("""
        SELECT b.member.id, b.member.name, b.member.avatarUrl, b.badgeId,
               b.member.position, b.member.branch, b.member.classroom, b.member.cohort
        FROM Badge b
        WHERE b.badgeId IN :rareBadgeIds
        ORDER BY b.earnedAt ASC
        """)
    List<Object[]> findMembersWithRareBadges(@Param("rareBadgeIds") List<String> rareBadgeIds);
}

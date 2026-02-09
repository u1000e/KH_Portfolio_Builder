package com.portfolio.builder.til.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TILRepository extends JpaRepository<TIL, Long> {

    @Query("SELECT t FROM TIL t JOIN FETCH t.member WHERE t.isPublic = true ORDER BY t.createdAt DESC")
    List<TIL> findAllPublicOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT t FROM TIL t JOIN FETCH t.member WHERE t.isPublic = true ORDER BY t.likeCount DESC, t.createdAt DESC")
    List<TIL> findAllPublicOrderByLikeCountDesc(Pageable pageable);

    @Query("SELECT t FROM TIL t JOIN FETCH t.member WHERE t.isPublic = true AND t.tags LIKE %:tag% ORDER BY t.createdAt DESC")
    List<TIL> findByTagContaining(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT t FROM TIL t WHERE t.member.id = :memberId ORDER BY t.createdAt DESC")
    List<TIL> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    @Query("SELECT t FROM TIL t WHERE t.member.id = :memberId AND FUNCTION('DATE', t.createdAt) = :date")
    List<TIL> findByMemberIdAndDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT FUNCTION('DATE', t.createdAt) FROM TIL t WHERE t.member.id = :memberId AND YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month")
    List<LocalDate> findWrittenDatesByMemberAndMonth(@Param("memberId") Long memberId, @Param("year") int year, @Param("month") int month);

    long countByMemberId(Long memberId);

    @Query("SELECT t FROM TIL t WHERE t.member.id = :memberId AND t.isPublic = true ORDER BY t.createdAt DESC")
    List<TIL> findRecentByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT t FROM TIL t JOIN FETCH t.member WHERE t.member.id = :memberId AND t.isPublic = true ORDER BY t.createdAt DESC")
    List<TIL> findAllPublicByMemberId(@Param("memberId") Long memberId);

    void deleteAllByMemberId(Long memberId);

    // ===== 랭킹용 쿼리 =====

    /**
     * TIL 작성 개수 랭킹 (동점 시 먼저 달성한 사람 우선)
     */
    @Query("""
        SELECT t.member.id, t.member.name, t.member.avatarUrl, COUNT(t) as cnt,
               t.member.position, t.member.branch, t.member.classroom, t.member.cohort
        FROM TIL t
        GROUP BY t.member.id, t.member.name, t.member.avatarUrl,
                 t.member.position, t.member.branch, t.member.classroom, t.member.cohort
        ORDER BY cnt DESC, MIN(t.createdAt) ASC
        """)
    List<Object[]> findTopByTilCount();

    /**
     * TIL 좋아요 받은 총 횟수 랭킹 (동점 시 먼저 달성한 사람 우선)
     */
    @Query("""
        SELECT t.member.id, t.member.name, t.member.avatarUrl, SUM(t.likeCount) as totalLikes,
               t.member.position, t.member.branch, t.member.classroom, t.member.cohort
        FROM TIL t
        GROUP BY t.member.id, t.member.name, t.member.avatarUrl,
                 t.member.position, t.member.branch, t.member.classroom, t.member.cohort
        HAVING SUM(t.likeCount) > 0
        ORDER BY totalLikes DESC, MIN(t.createdAt) ASC
        """)
    List<Object[]> findTopByTotalLikes();

    // 같은 반 필터링 (최신순)
    @Query("SELECT t FROM TIL t JOIN FETCH t.member m WHERE t.isPublic = true AND m.branch = :branch AND m.classroom = :classroom AND m.cohort = :cohort ORDER BY t.createdAt DESC")
    List<TIL> findAllPublicByClassOrderByCreatedAtDesc(@Param("branch") String branch, @Param("classroom") String classroom, @Param("cohort") String cohort, Pageable pageable);

    // 같은 반 필터링 (인기순)
    @Query("SELECT t FROM TIL t JOIN FETCH t.member m WHERE t.isPublic = true AND m.branch = :branch AND m.classroom = :classroom AND m.cohort = :cohort ORDER BY t.likeCount DESC, t.createdAt DESC")
    List<TIL> findAllPublicByClassOrderByLikeCountDesc(@Param("branch") String branch, @Param("classroom") String classroom, @Param("cohort") String cohort, Pageable pageable);

    // 같은 반 + 태그 필터링
    @Query("SELECT t FROM TIL t JOIN FETCH t.member m WHERE t.isPublic = true AND m.branch = :branch AND m.classroom = :classroom AND m.cohort = :cohort AND t.tags LIKE %:tag% ORDER BY t.createdAt DESC")
    List<TIL> findByClassAndTagContaining(@Param("branch") String branch, @Param("classroom") String classroom, @Param("cohort") String cohort, @Param("tag") String tag, Pageable pageable);

    // 강사용: 반별 TIL 전체 조회 (비공개 포함)
    @Query("SELECT t FROM TIL t JOIN FETCH t.member m WHERE m.branch = :branch AND m.classroom = :classroom AND m.cohort = :cohort ORDER BY t.createdAt DESC")
    List<TIL> findAllByClassOrderByCreatedAtDesc(@Param("branch") String branch, @Param("classroom") String classroom, @Param("cohort") String cohort);

    // 강사용: 반별 특정 날짜 TIL
    @Query("SELECT t FROM TIL t JOIN FETCH t.member m WHERE m.branch = :branch AND m.classroom = :classroom AND m.cohort = :cohort AND FUNCTION('DATE', t.createdAt) = :date ORDER BY t.createdAt DESC")
    List<TIL> findAllByClassAndDate(@Param("branch") String branch, @Param("classroom") String classroom, @Param("cohort") String cohort, @Param("date") LocalDate date);

    // 강사용: 반별 TIL 작성 통계
    @Query("SELECT m.id, m.name, m.avatarUrl, m.githubUsername, COUNT(t) as tilCount, MAX(t.createdAt) as lastTilDate FROM TIL t JOIN t.member m WHERE m.branch = :branch AND m.classroom = :classroom AND m.cohort = :cohort GROUP BY m.id, m.name, m.avatarUrl, m.githubUsername ORDER BY tilCount DESC")
    List<Object[]> findTilStatsByClass(@Param("branch") String branch, @Param("classroom") String classroom, @Param("cohort") String cohort);
}

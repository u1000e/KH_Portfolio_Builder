package com.portfolio.builder.portfolio.domain;

import com.portfolio.builder.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByMemberOrderByCreatedAtDesc(Member member);

    List<Portfolio> findByMember(Member member);

    // 공개 포트폴리오 - member가 존재하는 것만 (고아 데이터 제외)
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE p.isPublic = true ORDER BY p.createdAt DESC")
    List<Portfolio> findByIsPublicTrueOrderByCreatedAtDesc();

    // 공개된 포트폴리오 좋아요 순 정렬 - 서브쿼리로 ID만 가져온 후 정렬
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE p.isPublic = true AND p.member IS NOT NULL " +
           "ORDER BY (SELECT COUNT(pl) FROM PortfolioLike pl WHERE pl.portfolio = p) DESC, p.createdAt DESC")
    List<Portfolio> findPublicPortfoliosOrderByLikes();

    // 지점별 공개 포트폴리오 - member가 존재하는 것만
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE p.isPublic = true AND m.branch = :branch ORDER BY p.createdAt DESC")
    List<Portfolio> findByMemberBranchAndIsPublicTrue(@Param("branch") String branch);
    
    // 지점 + 강의실별 공개 포트폴리오
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE p.isPublic = true AND m.branch = :branch AND m.classroom = :classroom ORDER BY p.createdAt DESC")
    List<Portfolio> findByMemberBranchAndClassroomAndIsPublicTrue(@Param("branch") String branch, @Param("classroom") String classroom);
    
    // 지점 + 강의실 + 기수별 공개 포트폴리오
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE p.isPublic = true AND m.branch = :branch AND m.classroom = :classroom AND m.cohort = :cohort ORDER BY p.createdAt DESC")
    List<Portfolio> findByMemberBranchAndClassroomAndCohortAndIsPublicTrue(@Param("branch") String branch, @Param("classroom") String classroom, @Param("cohort") String cohort);
    
    // 전체 공개 포트폴리오 (직원/강사용)
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE p.isPublic = true ORDER BY p.createdAt DESC")
    List<Portfolio> findAllPublicPortfolios();
    
    // === 직원/강사용: 비공개 포함 전체 조회 ===
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE m.branch = :branch ORDER BY p.createdAt DESC")
    List<Portfolio> findByMemberBranch(@Param("branch") String branch);
    
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE m.branch = :branch AND m.classroom = :classroom ORDER BY p.createdAt DESC")
    List<Portfolio> findByMemberBranchAndClassroom(@Param("branch") String branch, @Param("classroom") String classroom);
    
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE m.branch = :branch AND m.classroom = :classroom AND m.cohort = :cohort ORDER BY p.createdAt DESC")
    List<Portfolio> findByMemberBranchAndClassroomAndCohort(@Param("branch") String branch, @Param("classroom") String classroom, @Param("cohort") String cohort);

    // 특정 회원의 공개된 포트폴리오
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m WHERE m.id = :memberId AND p.isPublic = true ORDER BY p.createdAt DESC")
    List<Portfolio> findPublicByMemberId(@Param("memberId") Long memberId);

    // 전체 포트폴리오 (관리자용) - member가 존재하는 것만
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.member m ORDER BY p.createdAt DESC")
    List<Portfolio> findAllWithMember();

    long countByMember(Member member);
}

package com.portfolio.builder.member.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByGithubId(String githubId);

    Optional<Member> findByGithubUsername(String githubUsername);

    List<Member> findByStatus(Member.Status status);

    List<Member> findByPosition(String position);

    List<Member> findByBranch(String branch);
    
    List<Member> findByPendingPositionIsNotNull();
    
    // 기수 목록 조회 (중복 제거)
    @Query("SELECT DISTINCT m.cohort FROM Member m WHERE m.cohort IS NOT NULL ORDER BY m.cohort DESC")
    List<String> findDistinctCohorts();
    
    // 강의실 목록 조회 (특정 지점의 강의실만)
    @Query("SELECT DISTINCT m.classroom FROM Member m WHERE m.branch = :branch AND m.classroom IS NOT NULL ORDER BY m.classroom")
    List<String> findDistinctClassroomsByBranch(@Param("branch") String branch);
    
    // 기수 목록 조회 (특정 지점의 기수만 - 강의실 무관)
    @Query("SELECT DISTINCT m.cohort FROM Member m WHERE m.branch = :branch AND m.cohort IS NOT NULL ORDER BY m.cohort DESC")
    List<String> findDistinctCohortsByBranch(@Param("branch") String branch);
    
    // 기수 목록 조회 (특정 지점 + 강의실의 기수만)
    @Query("SELECT DISTINCT m.cohort FROM Member m WHERE m.branch = :branch AND m.classroom = :classroom AND m.cohort IS NOT NULL ORDER BY m.cohort DESC")
    List<String> findDistinctCohortsByBranchAndClassroom(@Param("branch") String branch, @Param("classroom") String classroom);
    
    // 특정 직급의 회원 수 (희귀 배지 랭킹용)
    long countByPosition(String position);
}

package com.portfolio.builder.activity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityFeedRepository extends JpaRepository<ActivityFeed, Long> {

    // 전체 최신 활동 조회
    List<ActivityFeed> findTop20ByOrderByCreatedAtDesc();

    // 특정 반 활동 조회
    @Query("SELECT a FROM ActivityFeed a WHERE " +
           "(:branch IS NULL OR a.branch = :branch) AND " +
           "(:classroom IS NULL OR a.classroom = :classroom) AND " +
           "(:cohort IS NULL OR a.cohort = :cohort) " +
           "ORDER BY a.createdAt DESC")
    List<ActivityFeed> findByFilters(
            @Param("branch") String branch,
            @Param("classroom") String classroom,
            @Param("cohort") String cohort);

    // 최근 N개 조회 (같은 반)
    @Query("SELECT a FROM ActivityFeed a WHERE " +
           "(:branch IS NULL OR a.branch = :branch) AND " +
           "(:classroom IS NULL OR a.classroom = :classroom) " +
           "ORDER BY a.createdAt DESC " +
           "LIMIT 10")
    List<ActivityFeed> findRecentByBranchAndClassroom(
            @Param("branch") String branch,
            @Param("classroom") String classroom);

    // 최근 N개 조회 (같은 반 + 같은 기수)
    @Query("SELECT a FROM ActivityFeed a WHERE " +
           "a.branch = :branch AND " +
           "a.classroom = :classroom AND " +
           "a.cohort = :cohort " +
           "ORDER BY a.createdAt DESC " +
           "LIMIT 20")
    List<ActivityFeed> findRecentByBranchAndClassroomAndCohort(
            @Param("branch") String branch,
            @Param("classroom") String classroom,
            @Param("cohort") String cohort);
}

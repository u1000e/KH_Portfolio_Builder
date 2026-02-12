package com.portfolio.builder.instructor.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.builder.instructor.application.InstructorService;
import com.portfolio.builder.instructor.dto.ClassDashboardResponse;
import com.portfolio.builder.instructor.dto.InterviewDashboardResponse;
import com.portfolio.builder.instructor.dto.OverviewDashboardResponse;
import com.portfolio.builder.instructor.dto.QuizDashboardResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 
 * 강사/운영팀 전용 API
 */
@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
@Slf4j
public class InstructorController {

    private final InstructorService instructorService;

    /**
     * 반별 현황 대시보드 조회
     * GET /api/instructor/class-dashboard
     */ 
    @GetMapping("/class-dashboard")
    public ResponseEntity<ClassDashboardResponse> getClassDashboard(
            @RequestParam(required = false, name="branch") String branch,
            @RequestParam(required = false, name="classroom") String classroom,
            @RequestParam(required = false, name="cohort") String cohort,
            @AuthenticationPrincipal Long memberId) {

        log.info("Class dashboard requested - branch: {}, classroom: {}, cohort: {}, memberId: {}",
                branch, classroom, cohort, memberId);

        // 강사/운영팀 권한 확인
        instructorService.validateInstructor(memberId);

        ClassDashboardResponse response = instructorService.getClassDashboard(branch, classroom, cohort);
        return ResponseEntity.ok(response);
    }

    /**
     * 반별 퀴즈 현황 대시보드 조회
     * GET /api/instructor/quiz-dashboard
     */
    @GetMapping("/quiz-dashboard")
    public ResponseEntity<QuizDashboardResponse> getQuizDashboard(
            @RequestParam(required = false, name = "branch") String branch,
            @RequestParam(required = false, name = "classroom") String classroom,
            @RequestParam(required = false, name = "cohort") String cohort,
            @AuthenticationPrincipal Long memberId) {

        log.info("Quiz dashboard requested - branch: {}, classroom: {}, cohort: {}, memberId: {}",
                branch, classroom, cohort, memberId);

        instructorService.validateInstructor(memberId);

        QuizDashboardResponse response = instructorService.getQuizDashboard(branch, classroom, cohort);
        return ResponseEntity.ok(response);
    }

    /**
     * 반별 총괄 현황 대시보드 조회
     * GET /api/instructor/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<OverviewDashboardResponse> getClassOverview(
            @RequestParam(required = false, name = "branch") String branch,
            @RequestParam(required = false, name = "classroom") String classroom,
            @RequestParam(required = false, name = "cohort") String cohort,
            @AuthenticationPrincipal Long memberId) {

        log.info("Overview dashboard requested - branch: {}, classroom: {}, cohort: {}, memberId: {}",
                branch, classroom, cohort, memberId);

        instructorService.validateInstructor(memberId);

        OverviewDashboardResponse response = instructorService.getClassOverview(branch, classroom, cohort);
        return ResponseEntity.ok(response);
    }

    /**
     * 반별 면접토론 현황 대시보드 조회
     * GET /api/instructor/interview-dashboard
     */
    @GetMapping("/interview-dashboard")
    public ResponseEntity<InterviewDashboardResponse> getInterviewDashboard(
            @RequestParam(required = false, name = "branch") String branch,
            @RequestParam(required = false, name = "classroom") String classroom,
            @RequestParam(required = false, name = "cohort") String cohort,
            @AuthenticationPrincipal Long memberId) {

        log.info("Interview dashboard requested - branch: {}, classroom: {}, cohort: {}, memberId: {}",
                branch, classroom, cohort, memberId);

        instructorService.validateInstructor(memberId);

        InterviewDashboardResponse response = instructorService.getInterviewDashboard(branch, classroom, cohort);
        return ResponseEntity.ok(response);
    }
}

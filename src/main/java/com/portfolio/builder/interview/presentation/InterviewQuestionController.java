package com.portfolio.builder.interview.presentation;

import com.portfolio.builder.interview.application.InterviewQuestionService;
import com.portfolio.builder.interview.dto.InterviewQuestionRequest;
import com.portfolio.builder.interview.dto.InterviewQuestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interview-questions")
@RequiredArgsConstructor
public class InterviewQuestionController {

    private final InterviewQuestionService interviewQuestionService;

    /**
     * 질문 목록 조회 (필터 적용)
     */
    @GetMapping
    public ResponseEntity<List<InterviewQuestionResponse>> getQuestions(
            @RequestParam(name = "periods", required = false) List<String> periods,
            @RequestParam(name = "categories", required = false) List<String> categories
    ) {
        return ResponseEntity.ok(interviewQuestionService.getQuestions(periods, categories));
    }

    /**
     * 카테고리별 그룹핑된 질문 조회
     */
    @GetMapping("/grouped")
    public ResponseEntity<Map<String, List<InterviewQuestionResponse>>> getQuestionsGrouped(
            @RequestParam(name = "periods", required = false) List<String> periods,
            @RequestParam(name = "categories", required = false) List<String> categories
    ) {
        return ResponseEntity.ok(interviewQuestionService.getQuestionsGroupedByCategory(periods, categories));
    }

    /**
     * 존재하는 기간 목록 조회
     */
    @GetMapping("/periods")
    public ResponseEntity<List<String>> getAvailablePeriods() {
        return ResponseEntity.ok(interviewQuestionService.getAvailablePeriods());
    }

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(interviewQuestionService.getCategories());
    }

    /**
     * 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(name = "periods", required = false) List<String> periods
    ) {
        return ResponseEntity.ok(interviewQuestionService.getStatistics(periods));
    }

    /**
     * 질문 추가 (강사/운영팀만)
     */
    @PostMapping
    public ResponseEntity<InterviewQuestionResponse> addQuestion(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody InterviewQuestionRequest request
    ) {
        return ResponseEntity.ok(interviewQuestionService.addQuestion(memberId, request));
    }

    /**
     * 질문 삭제 (작성자 또는 관리자)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable("id") Long id
    ) {
        interviewQuestionService.deleteQuestion(memberId, id);
        return ResponseEntity.ok().build();
    }
}

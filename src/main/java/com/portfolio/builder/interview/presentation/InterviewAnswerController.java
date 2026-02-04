package com.portfolio.builder.interview.presentation;

import com.portfolio.builder.interview.application.InterviewAnswerService;
import com.portfolio.builder.interview.dto.InterviewAnswerRequest;
import com.portfolio.builder.interview.dto.InterviewAnswerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview-answers")
@RequiredArgsConstructor
public class InterviewAnswerController {

    private final InterviewAnswerService answerService;

    /**
     * 특정 질문의 답변 목록 조회 (좋아요순, 페이징)
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<Page<InterviewAnswerResponse>> getAnswersByQuestion(
            @PathVariable(name = "questionId") Long questionId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal Long memberId) {

        Page<InterviewAnswerResponse> answers = answerService.getAnswersByQuestion(questionId, memberId, page, size);
        return ResponseEntity.ok(answers);
    }

    /**
     * 특정 질문의 답변 개수 조회
     */
    @GetMapping("/question/{questionId}/count")
    public ResponseEntity<Long> getAnswerCount(@PathVariable(name = "questionId") Long questionId) {
        return ResponseEntity.ok(answerService.getAnswerCount(questionId));
    }

    /**
     * 핫한 토론 조회 (일주일 내 답변이 많이 달린 질문 상위 5개)
     */
    @GetMapping("/hot")
    public ResponseEntity<java.util.List<InterviewAnswerService.HotQuestionResponse>> getHotQuestions(
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(answerService.getHotQuestions(limit));
    }

    /**
     * 답변이 가장 많이 달린 질문 조회 (전체 기간)
     */
    @GetMapping("/top-questions")
    public ResponseEntity<java.util.List<InterviewAnswerService.TopQuestionResponse>> getTopQuestionsByAnswerCount(
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(answerService.getTopQuestionsByAnswerCount(limit));
    }

    /**
     * 답변 작성
     */
    @PostMapping("/question/{questionId}")
    public ResponseEntity<InterviewAnswerResponse> createAnswer(
            @PathVariable(name = "questionId") Long questionId,
            @RequestBody InterviewAnswerRequest request,
            @AuthenticationPrincipal Long memberId) {

        InterviewAnswerResponse response = answerService.createAnswer(questionId, memberId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 답변 수정 (본인만)
     */
    @PutMapping("/{answerId}")
    public ResponseEntity<InterviewAnswerResponse> updateAnswer(
            @PathVariable(name = "answerId") Long answerId,
            @RequestBody InterviewAnswerRequest request,
            @AuthenticationPrincipal Long memberId) {

        InterviewAnswerResponse response = answerService.updateAnswer(answerId, memberId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 답변 삭제 (본인만)
     */
    @DeleteMapping("/{answerId}")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable(name = "answerId") Long answerId,
            @AuthenticationPrincipal Long memberId) {

        answerService.deleteAnswer(answerId, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 좋아요 토글
     */
    @PostMapping("/{answerId}/like")
    public ResponseEntity<InterviewAnswerResponse> toggleLike(
            @PathVariable(name = "answerId") Long answerId,
            @AuthenticationPrincipal Long memberId) {

        InterviewAnswerResponse response = answerService.toggleLike(answerId, memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내가 작성한 답변 목록 조회 (카테고리/키워드 필터 지원)
     */
    @GetMapping("/my-answers")
    public ResponseEntity<org.springframework.data.domain.Page<InterviewAnswerService.MyAnswerResponse>> getMyAnswers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "keyword", required = false) String keyword,
            @AuthenticationPrincipal Long memberId) {

        return ResponseEntity.ok(answerService.getMyAnswers(memberId, category, keyword, page, size));
    }

    /**
     * 내가 좋아요한 답변 목록 조회 (카테고리/키워드 필터 지원)
     */
    @GetMapping("/my-likes")
    public ResponseEntity<org.springframework.data.domain.Page<InterviewAnswerService.LikedAnswerResponse>> getMyLikedAnswers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "keyword", required = false) String keyword,
            @AuthenticationPrincipal Long memberId) {

        return ResponseEntity.ok(answerService.getMyLikedAnswers(memberId, category, keyword, page, size));
    }

    /**
     * 내가 답변한 질문 ID 목록 조회
     */
    @GetMapping("/my-answered-questions")
    public ResponseEntity<java.util.List<Long>> getMyAnsweredQuestionIds(
            @AuthenticationPrincipal Long memberId) {

        return ResponseEntity.ok(answerService.getMyAnsweredQuestionIds(memberId));
    }
}

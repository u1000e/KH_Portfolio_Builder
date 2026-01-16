package com.portfolio.builder.comment.presentation;

import com.portfolio.builder.comment.application.CommentService;
import com.portfolio.builder.comment.dto.CommentRequest;
import com.portfolio.builder.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성 (갤러리에서 접근 시에만 가능)
    @PostMapping("/portfolio/{portfolioId}")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable("portfolioId") Long portfolioId,
            @RequestAttribute(name = "memberId") Long memberId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.createComment(portfolioId, memberId, request));
    }

    // 특정 포트폴리오의 댓글 목록 (갤러리에서 접근 시에만)
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable("portfolioId") Long portfolioId) {
        return ResponseEntity.ok(commentService.getCommentsByPortfolio(portfolioId));
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("commentId") Long commentId,
            @RequestAttribute(name = "memberId") Long memberId) {
        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.noContent().build();
    }
}

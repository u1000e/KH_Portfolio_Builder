package com.portfolio.builder.member.presentation;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.builder.member.application.MyPageService;
import com.portfolio.builder.member.dto.LikedPortfolioResponse;
import com.portfolio.builder.member.dto.MyCommentResponse;
import com.portfolio.builder.member.dto.MyFeedbackResponse;
import com.portfolio.builder.member.dto.ReceivedFeedbackResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 마이페이지 API 컨트롤러
 */
@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
@Slf4j
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * 좋아요한 포트폴리오 목록
     * GET /api/members/me/likes
     */
    @GetMapping("/likes")
    public ResponseEntity<List<LikedPortfolioResponse>> getLikedPortfolios(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(myPageService.getLikedPortfolios(memberId));
    }

    /**
     * 받은 피드백 목록
     * GET /api/members/me/feedbacks
     */
    @GetMapping("/feedbacks")
    public ResponseEntity<List<ReceivedFeedbackResponse>> getReceivedFeedbacks(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(myPageService.getReceivedFeedbacks(memberId));
    }

    /**
     * 내가 작성한 댓글 목록
     * GET /api/members/me/comments
     */
    @GetMapping("/comments")
    public ResponseEntity<List<MyCommentResponse>> getMyComments(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(myPageService.getMyComments(memberId));
    }

    /**
     * 내가 작성한 피드백 목록 (운영팀/강사용)
     * GET /api/members/me/written-feedbacks
     */
    @GetMapping("/written-feedbacks")
    public ResponseEntity<List<MyFeedbackResponse>> getMyFeedbacks(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(myPageService.getMyFeedbacks(memberId));
    }

    /**
     * 회원 탈퇴
     * DELETE /api/members/me
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> withdrawMember(
            @AuthenticationPrincipal Long memberId,
            @RequestBody(required = false) Map<String, String> body) {
        
        // 확인 문구 검증 (선택적)
        if (body != null && body.containsKey("confirmation")) {
            String confirmation = body.get("confirmation");
            if (!"탈퇴합니다".equals(confirmation)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "'탈퇴합니다'를 정확히 입력해주세요"));
            }
        }

        myPageService.withdrawMember(memberId);
        
        return ResponseEntity.ok(Map.of(
                "message", "회원 탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사합니다."
        ));
    }
}

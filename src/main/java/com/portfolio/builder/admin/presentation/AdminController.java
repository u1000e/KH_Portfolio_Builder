package com.portfolio.builder.admin.presentation;

import com.portfolio.builder.admin.application.AdminService;
import com.portfolio.builder.comment.dto.CommentResponse;
import com.portfolio.builder.member.dto.MemberResponse;
import com.portfolio.builder.portfolio.dto.PortfolioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    // === 회원 관리 ===
    @GetMapping("/members")
    public ResponseEntity<List<MemberResponse>> getAllMembers(
            @RequestAttribute(name = "memberId") Long memberId) {
        adminService.validateAdmin(memberId);
        return ResponseEntity.ok(adminService.getAllMembers());
    }

    @GetMapping("/members/{id}")
    public ResponseEntity<MemberResponse> getMember(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id) {
        adminService.validateAdmin(memberId);
        return ResponseEntity.ok(adminService.getMember(id));
    }

    @PutMapping("/members/{id}/role")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> request) {
        adminService.validateAdmin(memberId);
        return ResponseEntity.ok(adminService.updateMemberRole(id, request.get("role")));
    }

    @PutMapping("/members/{id}/status")
    public ResponseEntity<MemberResponse> updateMemberStatus(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> request) {
        adminService.validateAdmin(memberId);
        return ResponseEntity.ok(adminService.updateMemberStatus(id, request.get("status")));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> deleteMember(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id) {
        adminService.validateAdmin(memberId);
        adminService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    // === 포트폴리오 관리 ===
    @GetMapping("/portfolios")
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios(
            @RequestAttribute(name = "memberId") Long memberId) {
        adminService.validateAdmin(memberId);
        return ResponseEntity.ok(adminService.getAllPortfolios());
    }

    @DeleteMapping("/portfolios/{id}")
    public ResponseEntity<Void> deletePortfolio(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id) {
        adminService.validateAdmin(memberId);
        adminService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/portfolios/{id}/visibility")
    public ResponseEntity<PortfolioResponse> togglePortfolioVisibility(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id) {
        adminService.validateAdmin(memberId);
        return ResponseEntity.ok(adminService.togglePortfolioVisibility(id));
    }

    // === 댓글 관리 ===
    @GetMapping("/comments")
    public ResponseEntity<List<CommentResponse>> getAllComments(
            @RequestAttribute(name = "memberId") Long memberId) {
        adminService.validateAdmin(memberId);
        return ResponseEntity.ok(adminService.getAllComments());
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id) {
        adminService.validateAdmin(memberId);
        adminService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    // === 통계 ===
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestAttribute(name = "memberId") Long memberId) {
        adminService.validateAdmin(memberId);
        return ResponseEntity.ok(adminService.getStatistics());
    }
}

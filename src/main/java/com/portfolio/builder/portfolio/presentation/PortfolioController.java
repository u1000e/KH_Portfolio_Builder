package com.portfolio.builder.portfolio.presentation;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.builder.portfolio.application.PortfolioLikeService;
import com.portfolio.builder.portfolio.application.PortfolioService;
import com.portfolio.builder.portfolio.dto.PortfolioRequest;
import com.portfolio.builder.portfolio.dto.PortfolioResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioLikeService portfolioLikeService;

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @RequestAttribute(name = "memberId") Long memberId,
            @RequestBody PortfolioRequest request) {
        return ResponseEntity.ok(portfolioService.createPortfolio(memberId, request));
    }

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getMyPortfolios(
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(portfolioService.getMyPortfolios(memberId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(portfolioService.getPortfolio(memberId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id,
            @RequestBody PortfolioRequest request) {
        return ResponseEntity.ok(portfolioService.updatePortfolio(memberId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("id") Long id) {
        portfolioService.deletePortfolio(memberId, id);
        return ResponseEntity.noContent().build();
    }

    // 공개 포트폴리오 목록 (갤러리) - 인증 필요
    @GetMapping("/public")
    public ResponseEntity<List<PortfolioResponse>> getPublicPortfolios(
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(portfolioService.getPublicPortfolios(memberId));
    }

    // 좋아요 순 정렬
    @GetMapping("/public/popular")
    public ResponseEntity<List<PortfolioResponse>> getPopularPortfolios(
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(portfolioService.getPublicPortfoliosByLikes(memberId));
    }

    // 특정 소속(지점)의 공개 포트폴리오
    @GetMapping("/public/branch/{branch}")
    public ResponseEntity<List<PortfolioResponse>> getPortfoliosByBranch(
            @PathVariable("branch") String branch,
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(portfolioService.getPortfoliosByBranch(branch, memberId));
    }

    // 공개 포트폴리오 상세 (갤러리에서 접근)
    @GetMapping("/public/{id}")
    public ResponseEntity<PortfolioResponse> getPublicPortfolio(
            @PathVariable("id") Long id,
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(portfolioService.getPublicPortfolio(id, memberId));
    }

    // 좋아요 토글
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable("id") Long id,
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(portfolioLikeService.toggleLike(id, memberId));
    }
}

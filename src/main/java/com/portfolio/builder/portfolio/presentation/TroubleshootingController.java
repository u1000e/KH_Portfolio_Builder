package com.portfolio.builder.portfolio.presentation;

import com.portfolio.builder.portfolio.application.TroubleshootingService;
import com.portfolio.builder.portfolio.dto.TroubleshootingRequest;
import com.portfolio.builder.portfolio.dto.TroubleshootingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/troubleshootings")
@RequiredArgsConstructor
@Slf4j
public class TroubleshootingController {

    private final TroubleshootingService troubleshootingService;

    // 트러블슈팅 목록 조회
    @GetMapping
    public ResponseEntity<List<TroubleshootingResponse>> getTroubleshootings(
            @PathVariable("portfolioId") Long portfolioId) {
        return ResponseEntity.ok(troubleshootingService.getTroubleshootings(portfolioId));
    }

    // 트러블슈팅 추가
    @PostMapping
    public ResponseEntity<TroubleshootingResponse> createTroubleshooting(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("portfolioId") Long portfolioId,
            @Valid @RequestBody TroubleshootingRequest request) {
        return ResponseEntity.ok(troubleshootingService.createTroubleshooting(memberId, portfolioId, request));
    }

    // 트러블슈팅 수정
    @PutMapping("/{troubleshootingId}")
    public ResponseEntity<TroubleshootingResponse> updateTroubleshooting(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("portfolioId") Long portfolioId,
            @PathVariable("troubleshootingId") Long troubleshootingId,
            @Valid @RequestBody TroubleshootingRequest request) {
        return ResponseEntity.ok(troubleshootingService.updateTroubleshooting(memberId, troubleshootingId, request));
    }

    // 트러블슈팅 삭제
    @DeleteMapping("/{troubleshootingId}")
    public ResponseEntity<Void> deleteTroubleshooting(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("portfolioId") Long portfolioId,
            @PathVariable("troubleshootingId") Long troubleshootingId) {
        troubleshootingService.deleteTroubleshooting(memberId, troubleshootingId);
        return ResponseEntity.noContent().build();
    }
}

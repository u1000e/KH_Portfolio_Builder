package com.portfolio.builder.portfolio.presentation;

import com.portfolio.builder.portfolio.application.PortfolioService;
import com.portfolio.builder.portfolio.application.TroubleshootingService;
import com.portfolio.builder.portfolio.dto.PortfolioResponse;
import com.portfolio.builder.portfolio.dto.TroubleshootingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final PortfolioService portfolioService;
    private final TroubleshootingService troubleshootingService;

    // 공개 포트폴리오 목록 (인증 없이 접근 가능 - 공유 링크용)
    @GetMapping("/portfolios")
    public ResponseEntity<List<PortfolioResponse>> getPublicPortfolios() {
        return ResponseEntity.ok(portfolioService.getPublicPortfolios(null));
    }

    // 공개 포트폴리오 상세 (인증 없이 접근 가능 - 공유 링크용)
    @GetMapping("/portfolios/{id}")
    public ResponseEntity<PortfolioResponse> getPublicPortfolio(@PathVariable("id") Long id) {
        return ResponseEntity.ok(portfolioService.getPublicPortfolio(id, null));
    }

    // 공개 포트폴리오의 트러블슈팅 목록 (인증 없이 접근 가능)
    @GetMapping("/portfolios/{id}/troubleshootings")
    public ResponseEntity<List<TroubleshootingResponse>> getPublicTroubleshootings(
            @PathVariable("id") Long id) {
        // 공개 포트폴리오인지 먼저 확인
        portfolioService.getPublicPortfolio(id, null);
        return ResponseEntity.ok(troubleshootingService.getTroubleshootings(id));
    }
}

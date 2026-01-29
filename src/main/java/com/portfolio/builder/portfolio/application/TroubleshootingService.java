package com.portfolio.builder.portfolio.application;

import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.portfolio.domain.Troubleshooting;
import com.portfolio.builder.portfolio.domain.TroubleshootingRepository;
import com.portfolio.builder.portfolio.dto.TroubleshootingRequest;
import com.portfolio.builder.portfolio.dto.TroubleshootingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TroubleshootingService {

    private final TroubleshootingRepository troubleshootingRepository;
    private final PortfolioRepository portfolioRepository;
    
    private static final int MAX_TROUBLESHOOTINGS = 3;

    // 트러블슈팅 목록 조회
    @Transactional(readOnly = true)
    public List<TroubleshootingResponse> getTroubleshootings(Long portfolioId) {
        return troubleshootingRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId)
                .stream()
                .map(TroubleshootingResponse::from)
                .collect(Collectors.toList());
    }

    // 트러블슈팅 추가
    public TroubleshootingResponse createTroubleshooting(Long memberId, Long portfolioId, TroubleshootingRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // 본인 포트폴리오만 수정 가능
        if (!portfolio.getMember().getId().equals(memberId)) {
            throw new RuntimeException("Access denied");
        }

        // 최대 3개 제한 체크
        int currentCount = troubleshootingRepository.countByPortfolioId(portfolioId);
        if (currentCount >= MAX_TROUBLESHOOTINGS) {
            throw new RuntimeException("트러블슈팅은 최대 " + MAX_TROUBLESHOOTINGS + "개까지만 추가할 수 있습니다.");
        }

        Troubleshooting.Category category;
        try {
            category = Troubleshooting.Category.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid category: " + request.getCategory());
        }

        Troubleshooting troubleshooting = Troubleshooting.builder()
                .portfolio(portfolio)
                .category(category)
                .problem(request.getProblem())
                .cause(request.getCause())
                .solution(request.getSolution())
                .lesson(request.getLesson())
                .causeCode(request.getCauseCode())
                .solutionCode(request.getSolutionCode())
                .codeLanguage(request.getCodeLanguage())
                .build();

        Troubleshooting saved = troubleshootingRepository.save(troubleshooting);
        log.info("Troubleshooting created: portfolioId={}, troubleshootingId={}", portfolioId, saved.getId());
        return TroubleshootingResponse.from(saved);
    }

    // 트러블슈팅 수정
    public TroubleshootingResponse updateTroubleshooting(Long memberId, Long troubleshootingId, TroubleshootingRequest request) {
        Troubleshooting troubleshooting = troubleshootingRepository.findById(troubleshootingId)
                .orElseThrow(() -> new RuntimeException("Troubleshooting not found"));

        // 본인 포트폴리오만 수정 가능
        if (!troubleshooting.getPortfolio().getMember().getId().equals(memberId)) {
            throw new RuntimeException("Access denied");
        }

        if (request.getCategory() != null) {
            try {
                Troubleshooting.Category category = Troubleshooting.Category.valueOf(request.getCategory().toUpperCase());
                troubleshooting.setCategory(category);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid category: " + request.getCategory());
            }
        }
        if (request.getProblem() != null) {
            troubleshooting.setProblem(request.getProblem());
        }
        if (request.getCause() != null) {
            troubleshooting.setCause(request.getCause());
        }
        if (request.getSolution() != null) {
            troubleshooting.setSolution(request.getSolution());
        }
        if (request.getLesson() != null) {
            troubleshooting.setLesson(request.getLesson());
        }
        // 코드 스니펫은 null도 허용 (삭제 가능)
        troubleshooting.setCauseCode(request.getCauseCode());
        troubleshooting.setSolutionCode(request.getSolutionCode());
        troubleshooting.setCodeLanguage(request.getCodeLanguage());

        Troubleshooting updated = troubleshootingRepository.save(troubleshooting);
        log.info("Troubleshooting updated: id={}", troubleshootingId);
        return TroubleshootingResponse.from(updated);
    }

    // 트러블슈팅 삭제
    public void deleteTroubleshooting(Long memberId, Long troubleshootingId) {
        Troubleshooting troubleshooting = troubleshootingRepository.findById(troubleshootingId)
                .orElseThrow(() -> new RuntimeException("Troubleshooting not found"));

        // 본인 포트폴리오만 삭제 가능
        if (!troubleshooting.getPortfolio().getMember().getId().equals(memberId)) {
            throw new RuntimeException("Access denied");
        }

        troubleshootingRepository.delete(troubleshooting);
        log.info("Troubleshooting deleted: id={}", troubleshootingId);
    }

    // 단일 트러블슈팅 조회
    @Transactional(readOnly = true)
    public TroubleshootingResponse getTroubleshooting(Long troubleshootingId) {
        Troubleshooting troubleshooting = troubleshootingRepository.findById(troubleshootingId)
                .orElseThrow(() -> new RuntimeException("Troubleshooting not found"));
        return TroubleshootingResponse.from(troubleshooting);
    }
}

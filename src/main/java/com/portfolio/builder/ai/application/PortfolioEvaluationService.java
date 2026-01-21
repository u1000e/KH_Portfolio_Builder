package com.portfolio.builder.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.builder.ai.dto.*;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.portfolio.domain.Troubleshooting;
import com.portfolio.builder.portfolio.domain.TroubleshootingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 포트폴리오 평가 통합 서비스
 * 규칙 기반 점수 계산 + AI 표현력 평가 + AI 피드백 생성
 * 총 130점 만점 (완성도30 + 기술력30 + 트러블슈팅25 + 표현력20 + 활동성25)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioEvaluationService {
    
    private final RuleBasedScorer ruleBasedScorer;
    private final AiExpressionEvaluator aiExpressionEvaluator;
    private final AiFeedbackGenerator aiFeedbackGenerator;
    private final PortfolioRepository portfolioRepository;
    private final TroubleshootingRepository troubleshootingRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 포트폴리오 평가 실행
     * @param portfolioId 포트폴리오 ID
     * @param memberId 요청자 회원 ID (본인 확인용)
     * @return 평가 결과
     */
    public EvaluationResponse evaluate(Long portfolioId, Long memberId) {
        // 1. 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new RuntimeException("포트폴리오를 찾을 수 없습니다"));
        
        // 권한 체크 (본인 포트폴리오만 평가 가능)
        if (!portfolio.getMember().getId().equals(memberId)) {
            throw new RuntimeException("본인의 포트폴리오만 평가할 수 있습니다");
        }
        
        // 데이터 파싱
        PortfolioData data = parseData(portfolio.getData());
        List<Troubleshooting> troubleshootings = troubleshootingRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId);
        
        // 2. 규칙 기반 점수 계산 (130점 만점)
        ScoreResult completeness = ruleBasedScorer.calculateCompleteness(data);
        ScoreResult technical = ruleBasedScorer.calculateTechnical(data);
        ScoreResult troubleshooting = ruleBasedScorer.calculateTroubleshooting(troubleshootings);
        ScoreResult activity = ruleBasedScorer.calculateActivity(
            portfolio.getShowContributionGraph(),
            portfolio.getContributionGraphSnapshot(),
            data
        );
        
        // 3. AI 표현력 평가 (규칙 기반 + AI 보정)
        ScoreResult ruleExpression = ruleBasedScorer.calculateExpression(data);
        AiExpressionEvaluator.ExpressionResult aiExpression = aiExpressionEvaluator.evaluate(data, troubleshootings);
        
        // AI 점수와 규칙 점수 혼합 (AI 70%, 규칙 30%)
        int expressionScore = (int) Math.round(aiExpression.score() * 0.7 + ruleExpression.getScore() * 0.3);
        expressionScore = Math.min(20, Math.max(0, expressionScore)); // 0~20 범위 보장
        
        // AI 피드백 추가
        List<String> expressionDetails = new java.util.ArrayList<>(ruleExpression.getDetails());
        if (aiExpression.feedback() != null && !aiExpression.feedback().isBlank()) {
            expressionDetails.add(0, "🤖 " + aiExpression.feedback());
        }
        ScoreResult expression = new ScoreResult(expressionScore, 20, expressionDetails);
        
        int totalScore = completeness.getScore() + technical.getScore() + 
                        troubleshooting.getScore() + expression.getScore() + activity.getScore();
        
        // 3. EvaluationScores 객체 생성
        EvaluationScores scores = EvaluationScores.builder()
            .total(totalScore)
            .completeness(completeness.getScore())
            .completenessDetails(completeness.getDetails())
            .technical(technical.getScore())
            .technicalDetails(technical.getDetails())
            .troubleshooting(troubleshooting.getScore())
            .troubleshootingDetails(troubleshooting.getDetails())
            .expression(expression.getScore())
            .expressionDetails(expression.getDetails())
            .activity(activity.getScore())
            .activityDetails(activity.getDetails())
            .build();
        
        // 4. AI 피드백 생성
        PortfolioSummary summary = PortfolioSummary.builder()
            .name(data.getName())
            .skills(data.getSkillNames())
            .projectCount(data.getProjects() != null ? data.getProjects().size() : 0)
            .troubleshootingCount(troubleshootings.size())
            .build();
        
        AiFeedback aiFeedback = aiFeedbackGenerator.generateFeedback(scores, summary);
        
        // 5. AI 점수 저장
        portfolio.setAiScore(totalScore);
        portfolioRepository.save(portfolio);
        log.info("Portfolio {} AI score saved: {}", portfolioId, totalScore);
        
        // 6. 최종 응답 조합
        return EvaluationResponse.builder()
            .totalScore(totalScore)
            .breakdown(ScoreBreakdown.builder()
                .completeness(ScoreDetail.of(completeness))
                .technical(ScoreDetail.of(technical))
                .troubleshooting(ScoreDetail.of(troubleshooting))
                .expression(ScoreDetail.of(expression))
                .activity(ScoreDetail.of(activity))
                .build())
            .overallFeedback(aiFeedback.getOverallFeedback())
            .tips(aiFeedback.getTips())
            .evaluatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 포트폴리오 JSON 데이터 파싱
     */
    private PortfolioData parseData(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            log.warn("Portfolio data is null or blank");
            return new PortfolioData();
        }
        try {
            log.debug("Parsing portfolio data: {}", dataJson.substring(0, Math.min(200, dataJson.length())));
            PortfolioData data = objectMapper.readValue(dataJson, PortfolioData.class);
            log.info("Parsed portfolio data - name: {}, skills: {}, projects: {}", 
                data.getName(), 
                data.getSkillNames().size(),
                data.getProjects() != null ? data.getProjects().size() : 0);
            return data;
        } catch (Exception e) {
            log.error("Failed to parse portfolio data: {} - JSON preview: {}", 
                e.getMessage(), 
                dataJson.substring(0, Math.min(500, dataJson.length())));
            return new PortfolioData();
        }
    }
}

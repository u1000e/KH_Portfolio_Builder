package com.portfolio.builder.ai.application;

import com.portfolio.builder.ai.dto.AiFeedback;
import com.portfolio.builder.ai.dto.EvaluationScores;
import com.portfolio.builder.ai.dto.PortfolioSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 기반 피드백 생성기
 * 규칙 기반으로 계산된 점수를 바탕으로 자연스러운 피드백 문장 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiFeedbackGenerator {
    
    private final ChatClient chatClient;
    
    /**
     * 점수와 포트폴리오 요약을 바탕으로 AI 피드백 생성
     */
    public AiFeedback generateFeedback(EvaluationScores scores, PortfolioSummary summary) {
        String prompt = buildPrompt(scores, summary);
        
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(AiFeedback.class);
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패, 기본 피드백 반환", e);
            return createDefaultFeedback(scores);
        }
    }
    
    private String buildPrompt(EvaluationScores scores, PortfolioSummary summary) {
        return """
            당신은 개발자 포트폴리오 멘토입니다.
            다음 평가 결과를 바탕으로 건설적인 피드백을 작성해주세요.
            
            ## 평가 점수
            - 총점: %d/100
            - 완성도: %d/25 %s
            - 기술력: %d/25 %s
            - 트러블슈팅: %d/25 %s
            - 표현력: %d/15 %s
            - 활동성: %d/10 %s
            
            ## 포트폴리오 요약
            - 이름: %s
            - 스킬: %s
            - 프로젝트 수: %d개
            - 트러블슈팅 수: %d개
            
            위 정보를 바탕으로 JSON 형식으로 응답해주세요:
            {
              "overallFeedback": "전체적인 피드백 (2-3문장, 격려하는 톤)",
              "tips": ["구체적인 개선 팁 1", "구체적인 개선 팁 2", "구체적인 개선 팁 3"]
            }
            
            중요: 반드시 위 JSON 형식으로만 응답하세요.
            """.formatted(
                scores.getTotal(),
                scores.getCompleteness(), formatDetails(scores.getCompletenessDetails()),
                scores.getTechnical(), formatDetails(scores.getTechnicalDetails()),
                scores.getTroubleshooting(), formatDetails(scores.getTroubleshootingDetails()),
                scores.getExpression(), formatDetails(scores.getExpressionDetails()),
                scores.getActivity(), formatDetails(scores.getActivityDetails()),
                summary.getName() != null ? summary.getName() : "미입력",
                summary.getSkills() != null && !summary.getSkills().isEmpty() 
                    ? String.join(", ", summary.getSkills().subList(0, Math.min(5, summary.getSkills().size()))) 
                    : "미입력",
                summary.getProjectCount(),
                summary.getTroubleshootingCount()
            );
    }
    
    private String formatDetails(List<String> details) {
        if (details == null || details.isEmpty()) {
            return "(양호)";
        }
        return "- 개선필요: " + String.join(", ", details);
    }
    
    /**
     * AI 호출 실패 시 기본 피드백 생성
     */
    private AiFeedback createDefaultFeedback(EvaluationScores scores) {
        String feedback;
        if (scores.getTotal() >= 80) {
            feedback = "훌륭한 포트폴리오입니다! 전반적으로 잘 구성되어 있어요.";
        } else if (scores.getTotal() >= 60) {
            feedback = "좋은 포트폴리오입니다. 조금만 더 보완하면 더욱 좋아질 거예요!";
        } else if (scores.getTotal() >= 40) {
            feedback = "포트폴리오의 기본 틀이 잘 잡혀 있습니다. 세부 내용을 보강해보세요.";
        } else {
            feedback = "포트폴리오 작성을 시작하셨군요! 하나씩 채워나가면 멋진 포트폴리오가 될 거예요.";
        }
        
        List<String> tips = List.of(
            "프로젝트 설명에 본인의 역할과 기여도를 명시해보세요",
            "트러블슈팅 사례를 추가하면 문제 해결 능력을 어필할 수 있습니다",
            "자기소개를 더 구체적으로 작성해보세요"
        );
        
        return new AiFeedback(feedback, tips);
    }
}

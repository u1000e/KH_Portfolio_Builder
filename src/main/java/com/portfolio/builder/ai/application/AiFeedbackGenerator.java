package com.portfolio.builder.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.builder.ai.dto.AiFeedback;
import com.portfolio.builder.ai.dto.EvaluationScores;
import com.portfolio.builder.ai.dto.PortfolioSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
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
    private final ObjectMapper objectMapper;
    
    /**
     * 점수와 포트폴리오 요약을 바탕으로 AI 피드백 생성
     */
    public AiFeedback generateFeedback(EvaluationScores scores, PortfolioSummary summary) {
        String promptText = buildPrompt(scores, summary);
        
        try {
            // PromptTemplate을 우회하기 위해 직접 Prompt 생성
            Prompt prompt = new Prompt(new UserMessage(promptText));
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();
            
            log.debug("AI Response: {}", response);
            return parseResponse(response, scores);
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패, 기본 피드백 반환", e);
            return createDefaultFeedback(scores);
        }
    }
    
    /**
     * AI 응답 파싱
     */
    private AiFeedback parseResponse(String response, EvaluationScores scores) {
        try {
            // JSON 부분 추출
            String json = extractJson(response);
            return objectMapper.readValue(json, AiFeedback.class);
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패, 기본 피드백 반환: {}", e.getMessage());
            return createDefaultFeedback(scores);
        }
    }
    
    /**
     * 응답에서 JSON 추출
     */
    private String extractJson(String response) {
        // ```json ... ``` 블록 추출
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        
        // ``` ... ``` 블록 추출
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        
        // JSON 객체 직접 추출
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return response;
    }
    
    private String buildPrompt(EvaluationScores scores, PortfolioSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 개발자 포트폴리오 멘토입니다.\n");
        sb.append("다음 평가 결과를 바탕으로 건설적인 피드백을 작성해주세요.\n\n");
        
        sb.append("## 중요 제약사항\n");
        sb.append("- 이 서비스는 템플릿 기반 포트폴리오 빌더입니다\n");
        sb.append("- 디자인, 레이아웃, 색상, 폰트 등 시각적 요소는 템플릿으로 제공되므로 언급하지 마세요\n");
        sb.append("- 오직 '콘텐츠' 관련 피드백만 제공하세요 (자기소개, 프로젝트 설명, 기술스택, 트러블슈팅 등)\n\n");
        
        sb.append("## 평가 점수\n");
        sb.append(String.format("- 총점: %d/100\n", scores.getTotal()));
        sb.append(String.format("- 완성도: %d/25 %s\n", scores.getCompleteness(), formatDetails(scores.getCompletenessDetails())));
        sb.append(String.format("- 기술력: %d/25 %s\n", scores.getTechnical(), formatDetails(scores.getTechnicalDetails())));
        sb.append(String.format("- 트러블슈팅: %d/25 %s\n", scores.getTroubleshooting(), formatDetails(scores.getTroubleshootingDetails())));
        sb.append(String.format("- 표현력: %d/15 %s\n", scores.getExpression(), formatDetails(scores.getExpressionDetails())));
        sb.append(String.format("- 활동성: %d/10 %s\n\n", scores.getActivity(), formatDetails(scores.getActivityDetails())));
        
        sb.append("## 포트폴리오 요약\n");
        sb.append(String.format("- 이름: %s\n", summary.getName() != null ? summary.getName() : "미입력"));
        String skillsStr = summary.getSkills() != null && !summary.getSkills().isEmpty()
            ? String.join(", ", summary.getSkills().subList(0, Math.min(5, summary.getSkills().size())))
            : "미입력";
        sb.append(String.format("- 스킬: %s\n", skillsStr));
        sb.append(String.format("- 프로젝트 수: %d개\n", summary.getProjectCount()));
        sb.append(String.format("- 트러블슈팅 수: %d개\n\n", summary.getTroubleshootingCount()));
        
        sb.append("응답 형식: JSON으로 응답해주세요.\n");
        sb.append("필드: overallFeedback (전체 피드백 2-3문장), tips (개선 팁 3개 배열)\n");
        
        return sb.toString();
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

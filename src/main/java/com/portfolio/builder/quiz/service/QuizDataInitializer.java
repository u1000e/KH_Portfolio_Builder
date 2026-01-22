package com.portfolio.builder.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.builder.quiz.domain.Quiz;
import com.portfolio.builder.quiz.repository.QuizRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizDataInitializer {

    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper;

    /**
     * 애플리케이션 시작 시 퀴즈 데이터 로드
     * DB에 데이터가 없을 때만 실행
     */
    @PostConstruct
    @Transactional
    public void initQuizData() {
        long count = quizRepository.count();
        if (count > 0) {
            log.info("Quiz data already exists. Count: {}", count);
            return;
        }

        try {
            log.info("Loading quiz data from JSON file...");
            ClassPathResource resource = new ClassPathResource("data/interview_quiz.json");
            InputStream inputStream = resource.getInputStream();
            
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode quizzesNode = root.get("quizzes");
            
            if (quizzesNode == null || !quizzesNode.isArray()) {
                log.error("Invalid JSON format: 'quizzes' array not found");
                return;
            }

            int savedCount = 0;
            for (JsonNode quizNode : quizzesNode) {
                Quiz quiz = parseQuizNode(quizNode);
                if (quiz != null) {
                    quizRepository.save(quiz);
                    savedCount++;
                }
            }

            log.info("Quiz data loaded successfully. Total: {} questions", savedCount);
        } catch (Exception e) {
            log.error("Failed to load quiz data", e);
        }
    }

    private Quiz parseQuizNode(JsonNode node) {
        try {
            String category = node.get("category").asText();
            String type = node.get("type").asText();
            String question = node.get("question").asText();
            String explanation = node.get("explanation").asText();

            Integer answer;
            String options = null;

            if ("OX".equals(type)) {
                // O/X 문제: true → 1, false → 0
                answer = node.get("answer").asBoolean() ? 1 : 0;
            } else {
                // 객관식: 정답 인덱스
                answer = node.get("answer").asInt();
                // options 배열을 JSON 문자열로 저장
                JsonNode optionsNode = node.get("options");
                if (optionsNode != null && optionsNode.isArray()) {
                    options = objectMapper.writeValueAsString(optionsNode);
                }
            }

            return Quiz.builder()
                    .category(category)
                    .type(type)
                    .question(question)
                    .options(options)
                    .answer(answer)
                    .explanation(explanation)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse quiz node: {}", node, e);
            return null;
        }
    }
}

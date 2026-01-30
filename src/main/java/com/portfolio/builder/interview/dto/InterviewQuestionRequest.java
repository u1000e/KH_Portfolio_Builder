package com.portfolio.builder.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionRequest {
    private String period;      // "2026_1H", "2026_2H" 형식
    private String category;    // "기술/Java", "인성" 등
    private String question;    // 질문 내용
    private String company;     // 회사명 (선택)
}

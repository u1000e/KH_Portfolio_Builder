package com.portfolio.builder.til.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TILBoosterData {
    private List<String> supplements;        // 보충 설명 2개
    private List<String> selfCheckQuestions;  // 셀프 체크 질문 2개
    private List<String> coreKeywords;       // 핵심 키워드 3개
    private List<String> relatedKeywords;    // 연관 키워드 3개
}

package com.portfolio.builder.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedback {
    private String overallFeedback;  // 전체 피드백
    private List<String> tips;       // 개선 팁 목록
}

package com.portfolio.builder.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdown {
    private ScoreDetail completeness;    // 완성도 (25점)
    private ScoreDetail technical;       // 기술력 (25점)
    private ScoreDetail troubleshooting; // 트러블슈팅 (25점)
    private ScoreDetail expression;      // 표현력 (15점)
    private ScoreDetail activity;        // 활동성 (10점)
}

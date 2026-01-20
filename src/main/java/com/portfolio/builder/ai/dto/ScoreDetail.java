package com.portfolio.builder.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreDetail {
    private int score;
    private int maxScore;
    private String feedback;
    private List<String> details;
    
    public static ScoreDetail of(ScoreResult result) {
        return ScoreDetail.builder()
                .score(result.getScore())
                .maxScore(result.getMaxScore())
                .details(result.getDetails())
                .build();
    }
}

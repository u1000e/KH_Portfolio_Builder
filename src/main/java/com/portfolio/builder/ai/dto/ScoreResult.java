package com.portfolio.builder.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResult {
    private int score;
    private int maxScore;
    private List<String> details = new ArrayList<>();  // 개선 필요 사항
    
    public ScoreResult(int score, int maxScore) {
        this.score = score;
        this.maxScore = maxScore;
        this.details = new ArrayList<>();
    }
}

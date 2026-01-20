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
public class EvaluationScores {
    private int total;
    
    private int completeness;
    private List<String> completenessDetails;
    
    private int technical;
    private List<String> technicalDetails;
    
    private int troubleshooting;
    private List<String> troubleshootingDetails;
    
    private int expression;
    private List<String> expressionDetails;
    
    private int activity;
    private List<String> activityDetails;
}

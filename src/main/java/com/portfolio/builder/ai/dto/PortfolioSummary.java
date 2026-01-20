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
public class PortfolioSummary {
    private String name;
    private List<String> skills;
    private int projectCount;
    private int troubleshootingCount;
}

package com.portfolio.builder.portfolio.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioRequest {
    
    private String templateType;
    private String title;
    private String data;  // JSON string
    private Boolean isPublic;
    private Boolean showContributionGraph;
    private String contributionGraphSnapshot;  // GitHub 잔디 스냅샷 JSON
}

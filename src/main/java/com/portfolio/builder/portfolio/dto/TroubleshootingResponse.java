package com.portfolio.builder.portfolio.dto;

import com.portfolio.builder.portfolio.domain.Troubleshooting;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TroubleshootingResponse {
    
    private Long id;
    private Long portfolioId;
    private String category;
    private String categoryDisplayName;
    private String problem;
    private String cause;
    private String solution;
    private String lesson;
    private String causeCode;
    private String solutionCode;
    private String codeLanguage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static TroubleshootingResponse from(Troubleshooting troubleshooting) {
        return TroubleshootingResponse.builder()
                .id(troubleshooting.getId())
                .portfolioId(troubleshooting.getPortfolio().getId())
                .category(troubleshooting.getCategory().name())
                .categoryDisplayName(troubleshooting.getCategory().getDisplayName())
                .problem(troubleshooting.getProblem())
                .cause(troubleshooting.getCause())
                .solution(troubleshooting.getSolution())
                .lesson(troubleshooting.getLesson())
                .causeCode(troubleshooting.getCauseCode())
                .solutionCode(troubleshooting.getSolutionCode())
                .codeLanguage(troubleshooting.getCodeLanguage())
                .createdAt(troubleshooting.getCreatedAt())
                .updatedAt(troubleshooting.getUpdatedAt())
                .build();
    }
}

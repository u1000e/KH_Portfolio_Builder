package com.portfolio.builder.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TroubleshootingRequest {
    
    @NotNull(message = "카테고리는 필수입니다")
    private String category;  // CODE, BUG, PERFORMANCE, DEPLOY, SECURITY
    
    @NotBlank(message = "문제 상황은 필수입니다")
    @Size(max = 500, message = "문제 상황은 500자 이내로 작성해주세요")
    private String problem;
    
    @NotBlank(message = "원인 분석은 필수입니다")
    @Size(max = 1000, message = "원인 분석은 1000자 이내로 작성해주세요")
    private String cause;
    
    @NotBlank(message = "해결 방법은 필수입니다")
    @Size(max = 1000, message = "해결 방법은 1000자 이내로 작성해주세요")
    private String solution;
    
    @NotBlank(message = "배운 점은 필수입니다")
    @Size(max = 500, message = "배운 점은 500자 이내로 작성해주세요")
    private String lesson;

    // 코드 스니펫 (선택)
    @Size(max = 2000, message = "코드는 2000자 이내로 작성해주세요")
    private String causeCode;

    @Size(max = 2000, message = "코드는 2000자 이내로 작성해주세요")
    private String solutionCode;

    @Size(max = 30, message = "언어는 30자 이내로 작성해주세요")
    private String codeLanguage;
}

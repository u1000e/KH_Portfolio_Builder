package com.portfolio.builder.til.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TILRequest {

    @NotBlank(message = "오늘 배운 것을 입력해주세요.")
    @Size(max = 200, message = "200자 이내로 작성해주세요.")
    private String title;

    @NotBlank(message = "어려웠던 것을 입력해주세요.")
    @Size(max = 500, message = "500자 이내로 작성해주세요.")
    private String difficulty;

    @Size(max = 2000, message = "메모는 2000자 이내로 작성해주세요.")
    private String description;

    private String codeSnippet;

    @Size(max = 50)
    private String codeLanguage;

    @Size(max = 200)
    private String tags;

    @Size(max = 500)
    private String imageUrl;

    private Boolean isPublic;
}

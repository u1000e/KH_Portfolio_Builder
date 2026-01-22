package com.portfolio.builder.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "피드백 내용을 입력해주세요")
    @Size(min = 10, max = 2000, message = "피드백은 10자 이상 2000자 이하로 작성해주세요")
    private String content;
}

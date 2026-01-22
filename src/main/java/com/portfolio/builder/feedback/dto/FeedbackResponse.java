package com.portfolio.builder.feedback.dto;

import com.portfolio.builder.feedback.domain.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private Long id;
    private Long portfolioId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 작성자 정보
    private Long authorId;
    private String authorName;
    private String authorPosition;  // 강사, 직원

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .portfolioId(feedback.getPortfolio().getId())
                .content(feedback.getContent())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .authorId(feedback.getMember().getId())
                .authorName(feedback.getMember().getName())
                .authorPosition(feedback.getMember().getPosition())
                .build();
    }
}

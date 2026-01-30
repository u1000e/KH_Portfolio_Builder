package com.portfolio.builder.interview.dto;

import com.portfolio.builder.interview.domain.InterviewQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionResponse {
    private Long id;
    private String period;          // "2026_1H"
    private String periodDisplay;   // "2026 상반기"
    private String category;
    private String question;
    private String company;
    private String createdByName;
    private String createdAt;

    public static InterviewQuestionResponse from(InterviewQuestion entity) {
        return InterviewQuestionResponse.builder()
                .id(entity.getId())
                .period(entity.getPeriod())
                .periodDisplay(formatPeriod(entity.getPeriod()))
                .category(entity.getCategory())
                .question(entity.getQuestion())
                .company(entity.getCompany())
                .createdByName(entity.getCreatedBy().getName() != null
                        ? entity.getCreatedBy().getName()
                        : entity.getCreatedBy().getGithubUsername())
                .createdAt(entity.getCreatedAt().toString())
                .build();
    }

    private static String formatPeriod(String period) {
        // "2026_1H" -> "2026 상반기"
        if (period == null || period.length() < 6) return period;
        String year = period.substring(0, 4);
        String half = period.endsWith("1H") ? "상반기" : "하반기";
        return year + " " + half;
    }
}

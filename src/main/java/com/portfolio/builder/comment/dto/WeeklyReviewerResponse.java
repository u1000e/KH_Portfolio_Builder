package com.portfolio.builder.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReviewerResponse {
    private Long memberId;
    private String nickname;
    private String avatarUrl;
    private int rank;           // 1, 2, 3
    private int commentCount;   // 해당 주간 댓글 수
    private String emoji;       // 순위별 이모지
    private String titleName;   // 순위별 칭호명
    private LocalDate weekStartDate;  // 집계 시작일
    private LocalDate weekEndDate;    // 집계 종료일
    private LocalDateTime awardedAt;  // 수상 시각
}

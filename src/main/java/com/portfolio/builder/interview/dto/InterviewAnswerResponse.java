package com.portfolio.builder.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnswerResponse {
    private Long id;
    private Long questionId;
    private Long memberId;
    private String memberName;
    private String memberAvatarUrl;
    private String content;
    private int likeCount;
    private boolean isLiked;      // 현재 사용자가 좋아요 했는지
    private boolean isOwner;      // 현재 사용자의 답변인지
    private int rank;             // 좋아요 순위 (1, 2, 3위만 설정, 나머지 0)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

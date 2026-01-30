package com.portfolio.builder.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFeedDto {
    private Long id;
    private String activityType;
    private String message;
    private String memberName;
    private String memberAvatarUrl;
    private String extraData;
    private LocalDateTime createdAt;
    private String icon;
}

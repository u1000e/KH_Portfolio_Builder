package com.portfolio.builder.global.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitExceededResponse {
    private String message;
    private int dailyLimit;
    private int used;
    private int remaining;
}

package com.portfolio.builder.member.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    
    private String accessToken;
    private String tokenType;
    private MemberResponse member;

    public static TokenResponse of(String accessToken, MemberResponse member) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .member(member)
                .build();
    }
}

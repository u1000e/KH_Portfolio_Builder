package com.portfolio.builder.member.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {
    
    private String position;   // 직급: 직원, 강사, 수강생
    private String branch;     // 소속: 종로, 강남
    private String classroom;  // 강의실: 301, 302, A, B 등 (수강생만)
}

package com.portfolio.builder.member.dto;

import com.portfolio.builder.member.domain.Member;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    
    private Long id;
    private String email;
    private String name;
    private String avatarUrl;
    private String githubUsername;
    private String role;
    private String status;
    private String position;        // 직급: 직원/강사/수강생
    private String branch;          // 소속: 종로/강남
    private String classroom;       // 강의실 (수강생만)
    private String cohort;          // 기수 (예: "1기", "2기")
    private String pendingPosition; // 승인 대기 중인 직급
    private boolean profileCompleted;  // 프로필 설정 완료 여부

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .avatarUrl(member.getAvatarUrl())
                .githubUsername(member.getGithubUsername())
                .role(member.getRole().name())
                .status(member.getStatus().name())
                .position(member.getPosition())
                .branch(member.getBranch())
                .classroom(member.getClassroom())
                .cohort(member.getCohort())
                .pendingPosition(member.getPendingPosition())
                .profileCompleted(member.isProfileCompleted())
                .build();
    }
}

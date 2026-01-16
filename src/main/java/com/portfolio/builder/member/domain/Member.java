package com.portfolio.builder.member.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_MEMBER_PF")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String githubId;

    private String email;
    private String name;
    private String avatarUrl;
    private String githubUsername;
    private String accessToken;

    // 소속 정보
    private String position;   // 직급: 직원, 강사, 수강생
    private String branch;     // 소속: 종로, 강남
    private String classroom;  // 강의실 (수강생만)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Role {
        USER, ADMIN
    }

    public enum Status {
        PENDING,    // 프로필 설정 대기
        ACTIVE,     // 활성 상태
        SUSPENDED   // 정지 상태
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 프로필 설정 완료 여부 체크
    public boolean isProfileCompleted() {
        return position != null && branch != null;
    }
    
    // 역할 기반 뱃지 반환
    public String getBadge() {
        if (position == null) return null;
        
        if ("수강생".equals(position) && branch != null) {
            return "수강생-" + branch;
        }
        return position;  // 강사, 직원
    }
}

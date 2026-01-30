package com.portfolio.builder.activity.domain;

import com.portfolio.builder.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ACTIVITY_FEED")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String activityType;  // S_GRADE, PORTFOLIO_PUBLIC, STREAK_7, HIDDEN_BADGE

    @Column(length = 200)
    private String message;  // 표시 메시지

    @Column(length = 100)
    private String extraData;  // 추가 데이터 (배지ID, 포폴ID 등)

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 필터링용 필드
    @Column(length = 20)
    private String branch;

    @Column(length = 20)
    private String classroom;

    @Column(length = 20)
    private String cohort;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

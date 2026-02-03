package com.portfolio.builder.comment.domain;

import com.portfolio.builder.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_WEEKLY_REVIEWER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReviewer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;  // 1, 2, 3

    @Column(nullable = false)
    private int commentCount;  // 해당 주간 댓글 수

    @Column(nullable = false)
    private LocalDate weekStartDate;  // 집계 시작일 (전주 금요일)

    @Column(nullable = false)
    private LocalDate weekEndDate;    // 집계 종료일 (이번주 금요일)

    @Column(nullable = false)
    private LocalDateTime awardedAt;  // 보상 지급 시간

    @Column(length = 50)
    private String awardedTitleId;    // 부여된 칭호 ID

    @Column(length = 50)
    private String awardedBadgeId;    // 부여된 배지 ID

    @PrePersist
    protected void onCreate() {
        if (this.awardedAt == null) {
            this.awardedAt = LocalDateTime.now();
        }
    }
}

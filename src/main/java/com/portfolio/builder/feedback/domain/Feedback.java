package com.portfolio.builder.feedback.domain;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.portfolio.domain.Portfolio;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 강사/운영팀의 포트폴리오 피드백 엔티티
 * 수강생 포트폴리오에 대한 개선 피드백을 저장
 */
@Entity
@Table(name = "TB_FEEDBACK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;  // 피드백 작성자 (강사/운영팀)

    @Column(length = 2000, nullable = false)
    private String content;

    @Builder.Default
    private Boolean isRead = false;  // 수강생이 읽었는지

    @Builder.Default
    private Boolean isResolved = false;  // 수강생이 반영 완료했는지

    private LocalDateTime resolvedAt;  // 반영 완료 시간

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 반영 완료 처리
     */
    public void markAsResolved() {
        this.isResolved = true;
        this.resolvedAt = LocalDateTime.now();
    }
}

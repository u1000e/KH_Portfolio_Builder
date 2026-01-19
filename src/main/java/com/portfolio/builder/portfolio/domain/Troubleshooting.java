package com.portfolio.builder.portfolio.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_TROUBLESHOOTING_PF")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Troubleshooting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    // 카테고리: CODE, BUG, PERFORMANCE, DEPLOY, SECURITY
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // 문제 상황 (필수)
    @Column(nullable = false, length = 500)
    private String problem;

    // 원인 분석 (필수)
    @Column(nullable = false, length = 1000)
    private String cause;

    // 해결 방법 (필수)
    @Column(nullable = false, length = 1000)
    private String solution;

    // 배운 점 (필수)
    @Column(nullable = false, length = 500)
    private String lesson;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Category {
        CODE("코드"),
        BUG("버그"),
        PERFORMANCE("성능"),
        DEPLOY("배포"),
        SECURITY("보안");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

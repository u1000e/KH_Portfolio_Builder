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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "troubleshooting_seq")
    @SequenceGenerator(name = "troubleshooting_seq", sequenceName = "SEQ_TROUBLESHOOTING_PF", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    // 카테고리: CODE, BUG, PERFORMANCE, DEPLOY, SECURITY
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // 문제 상황 (필수) - 한글 500자 지원 (CHAR 단위)
    @Column(nullable = false, columnDefinition = "VARCHAR2(500 CHAR)")
    private String problem;

    // 원인 분석 (필수) - 한글 1000자 지원 (CHAR 단위)
    @Column(nullable = false, columnDefinition = "VARCHAR2(1000 CHAR)")
    private String cause;

    // 해결 방법 (필수) - 한글 1000자 지원 (CHAR 단위)
    @Column(nullable = false, columnDefinition = "VARCHAR2(1000 CHAR)")
    private String solution;

    // 배운 점 (필수) - 한글 500자 지원 (CHAR 단위)
    @Column(nullable = false, columnDefinition = "VARCHAR2(500 CHAR)")
    private String lesson;

    // 원인 분석 코드 스니펫 (선택) - 코드 2000자 지원
    @Column(columnDefinition = "VARCHAR2(2000 CHAR)")
    private String causeCode;

    // 해결 방법 코드 스니펫 (선택) - 코드 2000자 지원
    @Column(columnDefinition = "VARCHAR2(2000 CHAR)")
    private String solutionCode;

    // 코드 언어 (선택) - Java, JavaScript, SQL 등
    @Column(length = 30)
    private String codeLanguage;

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

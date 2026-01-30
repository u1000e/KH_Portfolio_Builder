package com.portfolio.builder.interview.domain;

import com.portfolio.builder.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_INTERVIEW_QUESTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String period;  // "2026_1H", "2026_2H" 형식

    @Column(nullable = false, length = 50)
    private String category;  // "기술/Java", "기술/Spring", "인성" 등

    @Column(nullable = false, columnDefinition = "VARCHAR2(2000 CHAR)")
    private String question;

    @Column(length = 100)
    private String company;  // 회사명 (선택)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private Member createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

package com.portfolio.builder.quiz.domain;

import com.portfolio.builder.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_QUIZ_ATTEMPT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private Integer userAnswer;  // 사용자가 선택한 답

    @Column(nullable = false)
    private Boolean isCorrect;  // 정답 여부

    @Column(nullable = false)
    private LocalDate attemptDate;  // 시도 날짜 (하루 5문제 제한용)

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isReviewMode = false;  // 복습 모드로 푼 문제인지

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String quizType = "INTERVIEW";  // INTERVIEW: 면접 대비, PRACTICE: 수업 복습

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.attemptDate == null) {
            this.attemptDate = LocalDate.now();
        }
    }
}

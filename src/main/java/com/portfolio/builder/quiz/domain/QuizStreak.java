package com.portfolio.builder.quiz.domain;

import com.portfolio.builder.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "TB_QUIZ_STREAK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false)
    private Integer currentStreak;  // 현재 연속 학습일

    @Column(nullable = false)
    private Integer maxStreak;  // 최대 연속 학습일

    @Column(nullable = false)
    private LocalDate lastStudyDate;  // 마지막 학습 날짜

    @Column(nullable = false)
    private Integer totalQuizCount;  // 총 푼 문제 수

    @Column(nullable = false)
    private Integer correctCount;  // 총 맞은 문제 수

    @PrePersist
    protected void onCreate() {
        if (this.currentStreak == null) this.currentStreak = 0;
        if (this.maxStreak == null) this.maxStreak = 0;
        if (this.totalQuizCount == null) this.totalQuizCount = 0;
        if (this.correctCount == null) this.correctCount = 0;
    }
}

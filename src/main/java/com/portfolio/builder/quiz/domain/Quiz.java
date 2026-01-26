package com.portfolio.builder.quiz.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_QUIZ")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;  // HTML/CSS, JavaScript, React, Spring, Database, Network, CS 기초, Java

    @Column(nullable = false, length = 20)
    private String type;  // OX, MULTIPLE

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String quizType = "INTERVIEW";  // INTERVIEW: 면접 대비, PRACTICE: 수업 복습

    @Column(nullable = false, length = 1000)
    private String question;

    // 객관식 보기 (JSON 배열 형태로 저장, OX는 null)
    @Column(length = 1000)
    private String options;

    // OX: true/false를 0/1로, 객관식: 정답 인덱스 (0~3)
    @Column(nullable = false)
    private Integer answer;

    @Column(nullable = false, length = 2000)
    private String explanation;
}

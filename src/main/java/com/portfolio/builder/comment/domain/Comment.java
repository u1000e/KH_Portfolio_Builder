package com.portfolio.builder.comment.domain;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.portfolio.domain.Portfolio;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_COMMENT_PF")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 1000, nullable = false)
    private String content;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

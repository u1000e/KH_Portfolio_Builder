package com.portfolio.builder.til.domain;

import com.portfolio.builder.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_TIL_LIKE_PF",
       uniqueConstraints = @UniqueConstraint(columnNames = {"til_id", "member_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TILLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "til_id", nullable = false)
    private TIL til;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

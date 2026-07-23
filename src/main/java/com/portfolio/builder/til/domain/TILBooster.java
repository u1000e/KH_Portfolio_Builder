package com.portfolio.builder.til.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_TIL_BOOSTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TILBooster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "til_id", nullable = false, unique = true)
    private TIL til;

    @Lob
    @Column(columnDefinition = "text")
    private String feedbackJson;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

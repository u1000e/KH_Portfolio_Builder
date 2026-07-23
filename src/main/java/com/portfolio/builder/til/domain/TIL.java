package com.portfolio.builder.til.domain;

import com.portfolio.builder.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_TIL_PF")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TIL {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String difficulty;

    @Column(length = 2000)
    private String description;

    @Lob
    @Column(columnDefinition = "text")
    private String codeSnippet;

    @Column(length = 50)
    private String codeLanguage;

    @Column(length = 200)
    private String tags;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 200)
    private String reflection;

    @Builder.Default
    private Integer likeCount = 0;

    @Builder.Default
    private Boolean isPublic = true;

    @Builder.Default
    private Boolean isHidden = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
}

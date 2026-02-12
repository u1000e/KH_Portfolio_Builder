package com.portfolio.builder.portfolio.application;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioLike;
import com.portfolio.builder.portfolio.domain.PortfolioLikeRepository;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.global.exception.ForbiddenException;
import com.portfolio.builder.global.exception.NotFoundException;
import com.portfolio.builder.quiz.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PortfolioLikeService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioLikeRepository portfolioLikeRepository;
    private final MemberRepository memberRepository;
    private final BadgeService badgeService;

    public Map<String, Object> toggleLike(Long portfolioId, Long memberId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new NotFoundException("Portfolio not found"));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        // 공개된 포트폴리오만 좋아요 가능
        if (!portfolio.getIsPublic()) {
            throw new ForbiddenException("Cannot like a private portfolio");
        }

        boolean isLiked;
        
        if (portfolioLikeRepository.existsByPortfolioAndMember(portfolio, member)) {
            // 이미 좋아요 한 경우 - 취소
            portfolioLikeRepository.deleteByPortfolioAndMember(portfolio, member);
            isLiked = false;
            log.info("Member {} unliked portfolio {}", memberId, portfolioId);
        } else {
            // 좋아요 추가
            PortfolioLike like = PortfolioLike.builder()
                    .portfolio(portfolio)
                    .member(member)
                    .build();
            portfolioLikeRepository.save(like);
            isLiked = true;
            log.info("Member {} liked portfolio {}", memberId, portfolioId);

            // 히든 배지 체크
            // 1. 포트폴리오 주인이 총 5개 이상 좋아요 받으면 "인기스타" 배지
            Long ownerId = portfolio.getMember().getId();
            int totalLikesReceived = portfolioLikeRepository.countLikesReceivedByMemberId(ownerId);
            if (totalLikesReceived >= 5) {
                badgeService.awardHiddenBadge(ownerId, "hidden_popular");
            }

            // 2. 좋아요 누른 사람이 총 10개 이상 눌렀으면 "서포터" 배지
            int totalLikesGiven = portfolioLikeRepository.countLikesGivenByMemberId(memberId);
            if (totalLikesGiven >= 10) {
                badgeService.awardHiddenBadge(memberId, "hidden_supporter");
            }
        }

        int likeCount = portfolioLikeRepository.countByPortfolioId(portfolioId);
        
        return Map.of(
            "isLiked", isLiked,
            "likeCount", likeCount
        );
    }

    @Transactional(readOnly = true)
    public int getLikeCount(Long portfolioId) {
        return portfolioLikeRepository.countByPortfolioId(portfolioId);
    }

    @Transactional(readOnly = true)
    public boolean isLikedByMember(Long portfolioId, Long memberId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
        Member member = memberRepository.findById(memberId).orElse(null);
        
        if (portfolio == null || member == null) {
            return false;
        }
        
        return portfolioLikeRepository.existsByPortfolioAndMember(portfolio, member);
    }
}

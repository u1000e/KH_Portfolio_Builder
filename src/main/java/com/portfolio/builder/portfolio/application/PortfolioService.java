package com.portfolio.builder.portfolio.application;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioLikeRepository;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.portfolio.dto.PortfolioRequest;
import com.portfolio.builder.portfolio.dto.PortfolioResponse;
import com.portfolio.builder.quiz.domain.Badge;
import com.portfolio.builder.quiz.repository.BadgeRepository;
import com.portfolio.builder.quiz.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioLikeRepository portfolioLikeRepository;
    private final MemberRepository memberRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeService badgeService;

    public PortfolioResponse createPortfolio(Long memberId, PortfolioRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        Portfolio portfolio = Portfolio.builder()
                .member(member)
                .templateType(request.getTemplateType())
                .title(request.getTitle())
                .data(request.getData())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .showContributionGraph(request.getShowContributionGraph() != null ? request.getShowContributionGraph() : true)
                .contributionGraphSnapshot(request.getContributionGraphSnapshot())
                .build();

        Portfolio saved = portfolioRepository.save(portfolio);
        return PortfolioResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PortfolioResponse> getMyPortfolios(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        return portfolioRepository.findByMemberOrderByCreatedAtDesc(member)
                .stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long memberId, Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // 본인 포트폴리오이거나 공개된 포트폴리오만 조회 가능
        if (!portfolio.getMember().getId().equals(memberId) && !portfolio.getIsPublic()) {
            throw new RuntimeException("Access denied");
        }

        int likeCount = portfolioLikeRepository.countByPortfolioId(portfolioId);
        Member currentMember = memberRepository.findById(memberId).orElse(null);
        boolean isLiked = currentMember != null && 
                         portfolioLikeRepository.existsByPortfolioAndMember(portfolio, currentMember);

        return PortfolioResponse.from(portfolio, likeCount, isLiked);
    }

    public PortfolioResponse updatePortfolio(Long memberId, Long portfolioId, PortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // 본인 포트폴리오만 수정 가능
        if (!portfolio.getMember().getId().equals(memberId)) {
            throw new RuntimeException("Access denied");
        }

        if (request.getTemplateType() != null) {
            portfolio.setTemplateType(request.getTemplateType());
        }
        if (request.getTitle() != null) {
            portfolio.setTitle(request.getTitle());
        }
        if (request.getData() != null) {
            portfolio.setData(request.getData());
        }
        if (request.getIsPublic() != null) {
            portfolio.setIsPublic(request.getIsPublic());
        }
        if (request.getShowContributionGraph() != null) {
            portfolio.setShowContributionGraph(request.getShowContributionGraph());
        }
        if (request.getContributionGraphSnapshot() != null) {
            portfolio.setContributionGraphSnapshot(request.getContributionGraphSnapshot());
        }

        Portfolio updated = portfolioRepository.save(portfolio);
        return PortfolioResponse.from(updated);
    }

    public void deletePortfolio(Long memberId, Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        // 본인 포트폴리오만 삭제 가능
        if (!portfolio.getMember().getId().equals(memberId)) {
            throw new RuntimeException("Access denied");
        }

        // 관련 좋아요 삭제
        portfolioLikeRepository.deleteAllByPortfolio(portfolio);
        
        portfolioRepository.delete(portfolio);
    }

    // 공개된 모든 포트폴리오 조회 (갤러리)
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getPublicPortfolios(Long currentMemberId) {
        Member currentMember = currentMemberId != null ? 
                memberRepository.findById(currentMemberId).orElse(null) : null;
        
        return portfolioRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(portfolio -> {
                    int likeCount = portfolioLikeRepository.countByPortfolioId(portfolio.getId());
                    boolean isLiked = currentMember != null && 
                                     portfolioLikeRepository.existsByPortfolioAndMember(portfolio, currentMember);
                    
                    // 배지 정보 조회
                    Long ownerId = portfolio.getMember() != null ? portfolio.getMember().getId() : null;
                    int badgeCount = 0;
                    List<String> recentBadges = List.of();
                    if (ownerId != null) {
                        badgeCount = (int) badgeRepository.countByMemberId(ownerId);
                        recentBadges = badgeRepository.findTop4ByMemberIdOrderByEarnedAtDesc(ownerId)
                                .stream()
                                .map(badge -> badgeService.getBadgeIcon(badge.getBadgeId()))
                                .collect(Collectors.toList());
                    }
                    
                    return PortfolioResponse.from(portfolio, likeCount, isLiked, badgeCount, recentBadges);
                })
                .collect(Collectors.toList());
    }

    // 좋아요 순 공개 포트폴리오
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getPublicPortfoliosByLikes(Long currentMemberId) {
        Member currentMember = currentMemberId != null ? 
                memberRepository.findById(currentMemberId).orElse(null) : null;
        
        return portfolioRepository.findPublicPortfoliosOrderByLikes()
                .stream()
                .map(portfolio -> {
                    int likeCount = portfolioLikeRepository.countByPortfolioId(portfolio.getId());
                    boolean isLiked = currentMember != null && 
                                     portfolioLikeRepository.existsByPortfolioAndMember(portfolio, currentMember);
                    
                    // 배지 정보 조회
                    Long ownerId = portfolio.getMember() != null ? portfolio.getMember().getId() : null;
                    int badgeCount = 0;
                    List<String> recentBadges = List.of();
                    if (ownerId != null) {
                        badgeCount = (int) badgeRepository.countByMemberId(ownerId);
                        recentBadges = badgeRepository.findTop4ByMemberIdOrderByEarnedAtDesc(ownerId)
                                .stream()
                                .map(badge -> badgeService.getBadgeIcon(badge.getBadgeId()))
                                .collect(Collectors.toList());
                    }
                    
                    return PortfolioResponse.from(portfolio, likeCount, isLiked, badgeCount, recentBadges);
                })
                .collect(Collectors.toList());
    }

    // 특정 소속(지점)의 공개 포트폴리오 조회
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getPortfoliosByBranch(String branch, Long currentMemberId) {
        Member currentMember = currentMemberId != null ? 
                memberRepository.findById(currentMemberId).orElse(null) : null;
        
        return portfolioRepository.findByMemberBranchAndIsPublicTrue(branch)
                .stream()
                .map(portfolio -> {
                    int likeCount = portfolioLikeRepository.countByPortfolioId(portfolio.getId());
                    boolean isLiked = currentMember != null && 
                                     portfolioLikeRepository.existsByPortfolioAndMember(portfolio, currentMember);
                    return PortfolioResponse.from(portfolio, likeCount, isLiked);
                })
                .collect(Collectors.toList());
    }
    
    // 필터링된 포트폴리오 조회 (운영팀/강사는 비공개 포함, 일반은 공개만)
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getFilteredPortfolios(String branch, String classroom, String cohort, Long currentMemberId) {
        Member currentMember = currentMemberId != null ? 
                memberRepository.findById(currentMemberId).orElse(null) : null;
        
        // 운영팀/강사 여부 확인
        boolean isStaff = currentMember != null && 
                ("운영팀".equals(currentMember.getPosition()) || "강사".equals(currentMember.getPosition()));
        
        List<Portfolio> portfolios;
        
        boolean hasBranch = branch != null && !branch.isEmpty();
        boolean hasClassroom = classroom != null && !classroom.isEmpty();
        boolean hasCohort = cohort != null && !cohort.isEmpty();
        
        if (isStaff) {
            // 운영팀/강사: 비공개 포함 전체 조회
            if (hasBranch && hasClassroom && hasCohort) {
                portfolios = portfolioRepository.findByMemberBranchAndClassroomAndCohort(branch, classroom, cohort);
            } else if (hasBranch && hasClassroom) {
                portfolios = portfolioRepository.findByMemberBranchAndClassroom(branch, classroom);
            } else if (hasBranch) {
                portfolios = portfolioRepository.findByMemberBranch(branch);
            } else {
                portfolios = portfolioRepository.findAllWithMember();
            }
        } else {
            // 일반 사용자: 공개 포트폴리오만
            if (hasBranch && hasClassroom && hasCohort) {
                portfolios = portfolioRepository.findByMemberBranchAndClassroomAndCohortAndIsPublicTrue(branch, classroom, cohort);
            } else if (hasBranch && hasClassroom) {
                portfolios = portfolioRepository.findByMemberBranchAndClassroomAndIsPublicTrue(branch, classroom);
            } else if (hasBranch) {
                portfolios = portfolioRepository.findByMemberBranchAndIsPublicTrue(branch);
            } else {
                portfolios = portfolioRepository.findAllPublicPortfolios();
            }
        }
        
        return portfolios.stream()
                .map(portfolio -> {
                    int likeCount = portfolioLikeRepository.countByPortfolioId(portfolio.getId());
                    boolean isLiked = currentMember != null && 
                                     portfolioLikeRepository.existsByPortfolioAndMember(portfolio, currentMember);
                    return PortfolioResponse.from(portfolio, likeCount, isLiked);
                })
                .collect(Collectors.toList());
    }

    // 포트폴리오 상세 조회 (갤러리에서 접근 - 운영팀/강사는 비공개도 조회 가능)
    @Transactional(readOnly = true)
    public PortfolioResponse getPublicPortfolio(Long portfolioId, Long currentMemberId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        Member currentMember = currentMemberId != null ? 
                memberRepository.findById(currentMemberId).orElse(null) : null;
        
        // 운영팀/강사 여부 확인
        boolean isStaff = currentMember != null && 
                ("운영팀".equals(currentMember.getPosition()) || "강사".equals(currentMember.getPosition()));
        
        // 비공개 포트폴리오는 운영팀/강사만 조회 가능
        if (!portfolio.getIsPublic() && !isStaff) {
            throw new RuntimeException("This portfolio is not public");
        }

        int likeCount = portfolioLikeRepository.countByPortfolioId(portfolioId);
        boolean isLiked = currentMember != null && 
                         portfolioLikeRepository.existsByPortfolioAndMember(portfolio, currentMember);

        return PortfolioResponse.from(portfolio, likeCount, isLiked);
    }
}

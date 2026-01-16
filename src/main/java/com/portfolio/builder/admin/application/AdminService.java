package com.portfolio.builder.admin.application;

import com.portfolio.builder.comment.application.CommentService;
import com.portfolio.builder.comment.domain.Comment;
import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.comment.dto.CommentResponse;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.member.dto.MemberResponse;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioLikeRepository;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.portfolio.dto.PortfolioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final MemberRepository memberRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioLikeRepository portfolioLikeRepository;
    private final CommentRepository commentRepository;

    // 관리자 권한 확인
    public void validateAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (member.getRole() != Member.Role.ADMIN) {
            throw new RuntimeException("Admin access required");
        }
    }

    // === 회원 관리 ===
    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMembers() {
        return memberRepository.findAll()
                .stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        return MemberResponse.from(member);
    }

    public MemberResponse updateMemberRole(Long targetMemberId, String role) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        try {
            member.setRole(Member.Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + role);
        }
        
        Member updated = memberRepository.save(member);
        log.info("Member {} role updated to {}", targetMemberId, role);
        return MemberResponse.from(updated);
    }

    public MemberResponse updateMemberStatus(Long targetMemberId, String status) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        try {
            member.setStatus(Member.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
        
        Member updated = memberRepository.save(member);
        log.info("Member {} status updated to {}", targetMemberId, status);
        return MemberResponse.from(updated);
    }

    public void deleteMember(Long targetMemberId) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        // 관련 데이터 삭제
        List<Portfolio> portfolios = portfolioRepository.findByMember(member);
        for (Portfolio portfolio : portfolios) {
            portfolioLikeRepository.deleteAllByPortfolio(portfolio);
            commentRepository.deleteAllByPortfolio(portfolio);
        }
        portfolioRepository.deleteAll(portfolios);
        portfolioLikeRepository.deleteAllByMember(member);
        commentRepository.deleteAllByMember(member);
        
        memberRepository.delete(member);
        log.info("Member {} deleted", targetMemberId);
    }

    // === 포트폴리오 관리 ===
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getAllPortfolios() {
        return portfolioRepository.findAll()
                .stream()
                .map(portfolio -> {
                    int likeCount = portfolioLikeRepository.countByPortfolioId(portfolio.getId());
                    return PortfolioResponse.from(portfolio, likeCount, false);
                })
                .collect(Collectors.toList());
    }

    public void deletePortfolio(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        
        portfolioLikeRepository.deleteAllByPortfolio(portfolio);
        commentRepository.deleteAllByPortfolio(portfolio);
        portfolioRepository.delete(portfolio);
        log.info("Portfolio {} deleted by admin", portfolioId);
    }

    public PortfolioResponse togglePortfolioVisibility(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        
        portfolio.setIsPublic(!portfolio.getIsPublic());
        Portfolio updated = portfolioRepository.save(portfolio);
        
        int likeCount = portfolioLikeRepository.countByPortfolioId(portfolioId);
        log.info("Portfolio {} visibility toggled to {}", portfolioId, updated.getIsPublic());
        return PortfolioResponse.from(updated, likeCount, false);
    }

    // === 댓글 관리 ===
    @Transactional(readOnly = true)
    public List<CommentResponse> getAllComments() {
        return commentRepository.findAllWithMemberAndPortfolio()
                .stream()
                .map(CommentResponse::fromForAdmin)
                .collect(Collectors.toList());
    }

    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        commentRepository.delete(comment);
        log.info("Comment {} deleted by admin", commentId);
    }

    // === 통계 ===
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalMembers", memberRepository.count());
        stats.put("totalPortfolios", portfolioRepository.count());
        stats.put("totalComments", commentRepository.count());
        
        // 상태별 회원 수
        stats.put("activeMembers", memberRepository.findByStatus(Member.Status.ACTIVE).size());
        stats.put("pendingMembers", memberRepository.findByStatus(Member.Status.PENDING).size());
        stats.put("suspendedMembers", memberRepository.findByStatus(Member.Status.SUSPENDED).size());
        
        return stats;
    }
}

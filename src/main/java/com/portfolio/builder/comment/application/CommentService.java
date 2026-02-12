package com.portfolio.builder.comment.application;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.builder.comment.domain.Comment;
import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.comment.dto.CommentRequest;
import com.portfolio.builder.comment.dto.CommentResponse;
import com.portfolio.builder.comment.dto.ReceivedCommentResponse;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.quiz.service.BadgeService;
import com.portfolio.builder.global.exception.NotFoundException;
import com.portfolio.builder.global.exception.ForbiddenException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PortfolioRepository portfolioRepository;
    private final MemberRepository memberRepository;
    private final ProfanityFilterService profanityFilterService;
    private final BadgeService badgeService;

    public CommentResponse createComment(Long portfolioId, Long memberId, CommentRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new NotFoundException("Portfolio not found"));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        // 공개된 포트폴리오에만 댓글 작성 가능
        if (!portfolio.getIsPublic()) {
            throw new ForbiddenException("Cannot comment on a private portfolio");
        }

        // 욕설 필터링 체크
        if (profanityFilterService.containsProfanity(request.getContent())) {
            throw new IllegalArgumentException("부적절한 표현이 포함되어 있습니다. 댓글을 수정해주세요.");
        }

        Comment comment = Comment.builder()
                .portfolio(portfolio)
                .member(member)
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Comment created: {} by member {} on portfolio {}",
                 saved.getId(), memberId, portfolioId);

        // 히든 배지 체크
        Long ownerId = portfolio.getMember().getId();

        // 1. 포트폴리오 주인이 총 5개 이상 댓글 받으면 "소통왕" 배지 (본인 댓글 제외)
        if (!ownerId.equals(memberId)) {
            int totalCommentsReceived = commentRepository.countCommentsReceivedByMemberId(ownerId);
            if (totalCommentsReceived >= 5) {
                badgeService.awardHiddenBadge(ownerId, "hidden_social");
            }
        }

        // 2. 댓글 작성자가 남의 포폴에 총 5개 이상 댓글 달았으면 "응원단" 배지
        if (!ownerId.equals(memberId)) {
            int totalCommentsGiven = commentRepository.countCommentsGivenByMemberId(memberId);
            if (totalCommentsGiven >= 5) {
                badgeService.awardHiddenBadge(memberId, "hidden_cheerleader");
            }
        }

        return CommentResponse.from(saved, portfolio.getMember().getId());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPortfolio(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new NotFoundException("Portfolio not found"));

        Long portfolioOwnerId = portfolio.getMember().getId();
        
        return commentRepository.findByPortfolioWithMember(portfolio)
                .stream()
                .map(comment -> CommentResponse.from(comment, portfolioOwnerId))
                .collect(Collectors.toList());
    }

    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        // 본인 댓글이거나 관리자만 삭제 가능
        if (!comment.getMember().getId().equals(memberId) && 
            member.getRole() != Member.Role.ADMIN) {
            throw new ForbiddenException("Access denied");
        }

        commentRepository.delete(comment);
        log.info("Comment deleted: {} by member {}", commentId, memberId);
    }

    // 관리자용: 모든 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentResponse> getAllComments() {
        return commentRepository.findAllWithMemberAndPortfolio()
                .stream()
                .map(CommentResponse::fromForAdmin)
                .collect(Collectors.toList());
    }

    // 관리자용: 댓글 삭제
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        commentRepository.delete(comment);
        log.info("Comment deleted by admin: {}", commentId);
    }

    // 읽지 않은 댓글 수 조회
    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return commentRepository.countUnreadByMemberId(memberId);
    }

    // 포폴의 모든 댓글 읽음 처리
    public void markAllAsRead(Long portfolioId, Long memberId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new NotFoundException("Portfolio not found"));

        // 포폴 주인만 처리 가능
        if (!portfolio.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("권한이 없습니다");
        }

        int updated = commentRepository.markAllAsReadByPortfolioId(portfolioId);
        log.info("Marked {} comments as read for portfolio {}", updated, portfolioId);
    }

    // 받은 댓글 목록 조회
    @Transactional(readOnly = true)
    public List<ReceivedCommentResponse> getReceivedComments(Long memberId) {
        return commentRepository.findReceivedByMemberId(memberId)
                .stream()
                .map(ReceivedCommentResponse::from)
                .collect(Collectors.toList());
    }
}

package com.portfolio.builder.comment.application;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.builder.comment.domain.Comment;
import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.comment.dto.CommentRequest;
import com.portfolio.builder.comment.dto.CommentResponse;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;

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

    public CommentResponse createComment(Long portfolioId, Long memberId, CommentRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 공개된 포트폴리오에만 댓글 작성 가능
        if (!portfolio.getIsPublic()) {
            throw new RuntimeException("Cannot comment on a private portfolio");
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
        
        return CommentResponse.from(saved, portfolio.getMember().getId());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPortfolio(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        Long portfolioOwnerId = portfolio.getMember().getId();
        
        return commentRepository.findByPortfolioWithMember(portfolio)
                .stream()
                .map(comment -> CommentResponse.from(comment, portfolioOwnerId))
                .collect(Collectors.toList());
    }

    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 본인 댓글이거나 관리자만 삭제 가능
        if (!comment.getMember().getId().equals(memberId) && 
            member.getRole() != Member.Role.ADMIN) {
            throw new RuntimeException("Access denied");
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
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        commentRepository.delete(comment);
        log.info("Comment deleted by admin: {}", commentId);
    }
}

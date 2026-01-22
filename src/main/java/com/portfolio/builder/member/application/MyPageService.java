package com.portfolio.builder.member.application;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.builder.comment.domain.Comment;
import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.feedback.domain.Feedback;
import com.portfolio.builder.feedback.domain.FeedbackRepository;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.member.dto.LikedPortfolioResponse;
import com.portfolio.builder.member.dto.MyCommentResponse;
import com.portfolio.builder.member.dto.MyFeedbackResponse;
import com.portfolio.builder.member.dto.ReceivedFeedbackResponse;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioLikeRepository;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.portfolio.domain.TroubleshootingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 마이페이지 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioLikeRepository portfolioLikeRepository;
    private final CommentRepository commentRepository;
    private final FeedbackRepository feedbackRepository;
    private final TroubleshootingRepository troubleshootingRepository;

    /**
     * 좋아요한 포트폴리오 목록 조회
     */
    public List<LikedPortfolioResponse> getLikedPortfolios(Long memberId) {
        List<Portfolio> portfolios = portfolioLikeRepository.findLikedPortfoliosByMemberId(memberId);
        
        return portfolios.stream()
                .map(p -> {
                    String avatarUrl = null;
                    try {
                        // data JSON에서 avatarUrl 추출
                        if (p.getData() != null && p.getData().contains("avatarUrl")) {
                            int start = p.getData().indexOf("\"avatarUrl\":\"") + 13;
                            int end = p.getData().indexOf("\"", start);
                            if (start > 12 && end > start) {
                                avatarUrl = p.getData().substring(start, end);
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    
                    return LikedPortfolioResponse.builder()
                            .id(p.getId())
                            .title(p.getTitle())
                            .templateType(p.getTemplateType())
                            .ownerName(p.getMember().getName())
                            .ownerAvatarUrl(avatarUrl)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 받은 피드백 목록 조회
     */
    public List<ReceivedFeedbackResponse> getReceivedFeedbacks(Long memberId) {
        List<Feedback> feedbacks = feedbackRepository.findReceivedFeedbacksByMemberId(memberId);
        
        return feedbacks.stream()
                .map(f -> ReceivedFeedbackResponse.builder()
                        .id(f.getId())
                        .portfolioId(f.getPortfolio().getId())
                        .portfolioTitle(f.getPortfolio().getTitle())
                        .content(f.getContent())
                        .authorName(f.getMember().getName())
                        .authorPosition(f.getMember().getPosition())
                        .createdAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 내가 작성한 피드백 목록 조회 (운영팀/강사용)
     */
    public List<MyFeedbackResponse> getMyFeedbacks(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        // 운영팀/강사만 조회 가능
        String position = member.getPosition();
        if (!"운영팀".equals(position) && !"강사".equals(position)) {
            return List.of(); // 빈 목록 반환
        }

        List<Feedback> feedbacks = feedbackRepository.findWrittenFeedbacksByMemberId(memberId);
        
        return feedbacks.stream()
                .map(f -> MyFeedbackResponse.builder()
                        .id(f.getId())
                        .portfolioId(f.getPortfolio().getId())
                        .portfolioTitle(f.getPortfolio().getTitle())
                        .portfolioOwnerName(f.getPortfolio().getMember().getName())
                        .content(f.getContent())
                        .createdAt(f.getCreatedAt())
                        .updatedAt(f.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 내가 작성한 댓글 목록 조회
     */
    public List<MyCommentResponse> getMyComments(Long memberId) {
        List<Comment> comments = commentRepository.findByMemberIdWithPortfolio(memberId);
        
        return comments.stream()
                .map(c -> MyCommentResponse.builder()
                        .id(c.getId())
                        .portfolioId(c.getPortfolio().getId())
                        .portfolioTitle(c.getPortfolio().getTitle())
                        .portfolioOwnerName(c.getPortfolio().getMember().getName())
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 회원 탈퇴 (실제 삭제)
     * 순서: 피드백 → 댓글 → 좋아요 → 트러블슈팅 → 포트폴리오 → 회원
     */
    @Transactional
    public void withdrawMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        log.info("회원 탈퇴 시작 - memberId: {}, name: {}", memberId, member.getName());

        // 1. 내가 작성한 피드백 삭제
        feedbackRepository.deleteAllByMemberId(memberId);
        log.info("피드백 삭제 완료");

        // 2. 내가 작성한 댓글 삭제
        commentRepository.deleteAllByMember(member);
        log.info("댓글 삭제 완료");

        // 3. 내가 누른 좋아요 삭제
        portfolioLikeRepository.deleteAllByMember(member);
        log.info("좋아요 삭제 완료"); 

        // 4. 내 포트폴리오들 삭제 (연관 데이터 포함)
        List<Portfolio> myPortfolios = portfolioRepository.findByMemberId(memberId);
        for (Portfolio portfolio : myPortfolios) {
            // 포트폴리오에 달린 피드백 삭제
            feedbackRepository.deleteAllByPortfolioId(portfolio.getId());
            // 포트폴리오에 달린 댓글 삭제
            commentRepository.deleteAllByPortfolio(portfolio);
            // 포트폴리오에 달린 좋아요 삭제
            portfolioLikeRepository.deleteAllByPortfolio(portfolio);
            // 트러블슈팅 삭제
            troubleshootingRepository.deleteAllByPortfolio(portfolio);
            // 포트폴리오 삭제
            portfolioRepository.delete(portfolio);
        }
        log.info("포트폴리오 {} 개 삭제 완료", myPortfolios.size());

        // 5. 회원 삭제
        memberRepository.delete(member);
        log.info("회원 탈퇴 완료 - memberId: {}", memberId);
    }
}

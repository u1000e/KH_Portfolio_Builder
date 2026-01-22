package com.portfolio.builder.feedback.application;

import com.portfolio.builder.feedback.domain.Feedback;
import com.portfolio.builder.feedback.domain.FeedbackRepository;
import com.portfolio.builder.feedback.dto.FeedbackRequest;
import com.portfolio.builder.feedback.dto.FeedbackResponse;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final PortfolioRepository portfolioRepository;
    private final MemberRepository memberRepository;

    /**
     * 피드백 작성 (운영팀/강사만 가능)
     */
    @Transactional
    public FeedbackResponse createFeedback(Long portfolioId, Long memberId, FeedbackRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        // 권한 체크: 운영팀/강사만 작성 가능
        if (!isStaffOrInstructor(member)) {
            throw new IllegalStateException("피드백은 강사 또는 운영팀만 작성할 수 있습니다");
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다"));

        Feedback feedback = Feedback.builder()
                .portfolio(portfolio)
                .member(member)
                .content(request.getContent())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Feedback created - portfolioId: {}, authorId: {}, authorPosition: {}", 
                portfolioId, memberId, member.getPosition());

        return FeedbackResponse.from(saved);
    }

    /**
     * 피드백 목록 조회
     * - 포트폴리오 소유자: 자기 포트폴리오만 조회 가능
     * - 운영팀/강사/관리자: 모든 포트폴리오 조회 가능
     */
    public List<FeedbackResponse> getFeedbacks(Long portfolioId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다"));

        // 권한 체크: 소유자 또는 운영팀/강사/관리자만 조회 가능
        boolean isOwner = portfolio.getMember().getId().equals(memberId);
        boolean hasPrivilege = isStaffOrInstructor(member) || isAdmin(member);

        if (!isOwner && !hasPrivilege) {
            throw new IllegalStateException("피드백 조회 권한이 없습니다");
        }

        return feedbackRepository.findByPortfolioIdWithMember(portfolioId)
                .stream()
                .map(FeedbackResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 포트폴리오의 피드백 개수 조회 (권한 체크 없음 - 카운트용)
     */
    public long getFeedbackCount(Long portfolioId) {
        return feedbackRepository.countByPortfolioId(portfolioId);
    }

    /**
     * 피드백 수정 (작성자 본인만)
     */
    @Transactional
    public FeedbackResponse updateFeedback(Long feedbackId, Long memberId, FeedbackRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다"));

        if (!feedback.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("본인이 작성한 피드백만 수정할 수 있습니다");
        }

        feedback.setContent(request.getContent());
        log.info("Feedback updated - feedbackId: {}, memberId: {}", feedbackId, memberId);

        return FeedbackResponse.from(feedback);
    }

    /**
     * 피드백 삭제 (작성자 본인 또는 관리자)
     */
    @Transactional
    public void deleteFeedback(Long feedbackId, Long memberId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        boolean isAuthor = feedback.getMember().getId().equals(memberId);
        boolean isAdminUser = isAdmin(member);

        if (!isAuthor && !isAdminUser) {
            throw new IllegalStateException("피드백 삭제 권한이 없습니다");
        }

        feedbackRepository.delete(feedback);
        log.info("Feedback deleted - feedbackId: {}, deletedBy: {}", feedbackId, memberId);
    }

    private boolean isStaffOrInstructor(Member member) {
        String position = member.getPosition();
        return "운영팀".equals(position) || "강사".equals(position);
    }

    private boolean isAdmin(Member member) {
        return "ADMIN".equals(member.getRole());
    }
}

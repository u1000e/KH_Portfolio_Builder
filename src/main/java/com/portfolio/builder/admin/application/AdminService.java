package com.portfolio.builder.admin.application;

import com.portfolio.builder.activity.domain.ActivityFeedRepository;
import com.portfolio.builder.comment.domain.Comment;
import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.comment.domain.WeeklyReviewerRepository;
import com.portfolio.builder.comment.dto.CommentResponse;
import com.portfolio.builder.feedback.domain.FeedbackRepository;
import com.portfolio.builder.interview.domain.InterviewAnswer;
import com.portfolio.builder.interview.domain.InterviewAnswerLikeRepository;
import com.portfolio.builder.interview.domain.InterviewAnswerRepository;
import com.portfolio.builder.interview.dto.InterviewAnswerResponse;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.member.dto.MemberResponse;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioLikeRepository;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.portfolio.domain.TroubleshootingRepository;
import com.portfolio.builder.portfolio.dto.PortfolioResponse;
import com.portfolio.builder.quiz.repository.BadgeRepository;
import com.portfolio.builder.quiz.repository.QuizAttemptRepository;
import com.portfolio.builder.quiz.repository.QuizStreakRepository;
import com.portfolio.builder.til.domain.TIL;
import com.portfolio.builder.til.domain.TILRepository;
import com.portfolio.builder.til.dto.TILResponse;
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
    private final TroubleshootingRepository troubleshootingRepository;
    private final FeedbackRepository feedbackRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizStreakRepository quizStreakRepository;
    private final BadgeRepository badgeRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewAnswerLikeRepository interviewAnswerLikeRepository;
    private final WeeklyReviewerRepository weeklyReviewerRepository;
    private final ActivityFeedRepository activityFeedRepository;
    private final TILRepository tilRepository;

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

    /**
     * 회원 하드 삭제 - 모든 관련 데이터 삭제
     */
    public void deleteMember(Long targetMemberId) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        log.info("Starting hard delete for member {} ({})", targetMemberId, member.getName());

        // 1. 면접 토론 관련 삭제
        // 1-1. 해당 회원의 답변에 달린 좋아요 삭제
        List<Long> memberAnswerIds = interviewAnswerRepository.findIdsByMemberId(targetMemberId);
        if (!memberAnswerIds.isEmpty()) {
            interviewAnswerLikeRepository.deleteAllByAnswerIdIn(memberAnswerIds);
            log.info("Deleted likes on member's {} interview answers", memberAnswerIds.size());
        }
        // 1-2. 해당 회원이 누른 좋아요 삭제
        interviewAnswerLikeRepository.deleteAllByMemberId(targetMemberId);
        log.info("Deleted member's interview answer likes");
        // 1-3. 해당 회원의 답변 삭제
        interviewAnswerRepository.deleteAllByMemberId(targetMemberId);
        log.info("Deleted member's interview answers");

        // 2. 주간 베스트 리뷰어 기록 삭제
        weeklyReviewerRepository.deleteAllByMemberId(targetMemberId);
        log.info("Deleted weekly reviewer records");

        // 3. 퀴즈 관련 삭제
        badgeRepository.deleteAllByMemberId(targetMemberId);
        log.info("Deleted badges");
        quizAttemptRepository.deleteAllByMemberId(targetMemberId);
        log.info("Deleted quiz attempts");
        quizStreakRepository.deleteByMemberId(targetMemberId);
        log.info("Deleted quiz streak");

        // 4. 피드백 삭제 (해당 회원이 작성한 피드백)
        feedbackRepository.deleteAllByMemberId(targetMemberId);
        log.info("Deleted written feedbacks");

        // 5. 포트폴리오 관련 삭제
        List<Portfolio> portfolios = portfolioRepository.findByMember(member);
        for (Portfolio portfolio : portfolios) {
            // 포트폴리오에 달린 피드백 삭제
            feedbackRepository.deleteAllByPortfolioId(portfolio.getId());
            // 포트폴리오에 달린 좋아요 삭제
            portfolioLikeRepository.deleteAllByPortfolio(portfolio);
            // 포트폴리오에 달린 댓글 삭제
            commentRepository.deleteAllByPortfolio(portfolio);
            // 트러블슈팅 삭제
            troubleshootingRepository.deleteAllByPortfolio(portfolio);
        }
        portfolioRepository.deleteAll(portfolios);
        log.info("Deleted {} portfolios and related data", portfolios.size());

        // 6. 회원이 누른 좋아요 삭제 (다른 사람 포트폴리오)
        portfolioLikeRepository.deleteAllByMember(member);
        log.info("Deleted portfolio likes given");

        // 7. 회원이 작성한 댓글 삭제 (다른 사람 포트폴리오)
        commentRepository.deleteAllByMember(member);
        log.info("Deleted comments written");

        // 8. 활동 피드 삭제
        activityFeedRepository.deleteAllByMemberId(targetMemberId);
        log.info("Deleted activity feeds");

        // 9. 최종적으로 회원 삭제
        memberRepository.delete(member);
        log.info("Member {} ({}) hard deleted successfully", targetMemberId, member.getName());
    }

    // === 포트폴리오 관리 ===
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getAllPortfolios() {
        return portfolioRepository.findAllWithMember()
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

    // === TIL 관리 ===
    @Transactional(readOnly = true)
    public List<TILResponse> getAllTILs() {
        return tilRepository.findAllWithMember()
                .stream()
                .map(til -> TILResponse.from(til, null, false))
                .collect(Collectors.toList());
    }

    public void toggleTILVisibility(Long tilId) {
        TIL til = tilRepository.findById(tilId)
                .orElseThrow(() -> new RuntimeException("TIL not found"));
        til.setIsHidden(!Boolean.TRUE.equals(til.getIsHidden()));
        tilRepository.save(til);
        log.info("TIL {} hidden toggled to {}", tilId, til.getIsHidden());
    }

    // === 댓글 숨김 토글 ===
    public void toggleCommentVisibility(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setIsHidden(!Boolean.TRUE.equals(comment.getIsHidden()));
        commentRepository.save(comment);
        log.info("Comment {} hidden toggled to {}", commentId, comment.getIsHidden());
    }

    // === 면접답변 관리 ===
    @Transactional(readOnly = true)
    public List<InterviewAnswerResponse> getAllInterviewAnswers() {
        return interviewAnswerRepository.findAllWithMemberAndQuestion()
                .stream()
                .map(answer -> {
                    com.portfolio.builder.member.domain.Member member = answer.getMember();
                    String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();
                    return InterviewAnswerResponse.builder()
                            .id(answer.getId())
                            .questionId(answer.getQuestion().getId())
                            .memberId(member.getId())
                            .memberName(displayName)
                            .memberAvatarUrl(member.getAvatarUrl())
                            .content(answer.getContent())
                            .likeCount(answer.getLikeCount())
                            .isHidden(Boolean.TRUE.equals(answer.getIsHidden()))
                            .createdAt(answer.getCreatedAt())
                            .updatedAt(answer.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public void toggleInterviewAnswerVisibility(Long answerId) {
        InterviewAnswer answer = interviewAnswerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Interview answer not found"));
        answer.setIsHidden(!Boolean.TRUE.equals(answer.getIsHidden()));
        interviewAnswerRepository.save(answer);
        log.info("Interview answer {} hidden toggled to {}", answerId, answer.getIsHidden());
    }

    // === 직급 신청 관리 ===
    
    // 대기 중인 직급 신청 목록 조회
    @Transactional(readOnly = true)
    public List<MemberResponse> getPendingPositionRequests() {
        return memberRepository.findByPendingPositionIsNotNull()
                .stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
    }
    
    // 직급 신청 승인
    public MemberResponse approvePositionRequest(Long targetMemberId) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (member.getPendingPosition() == null) {
            throw new RuntimeException("No pending position request");
        }
        
        String approvedPosition = member.getPendingPosition();
        member.setPosition(approvedPosition);
        member.setPendingPosition(null);
        member.setClassroom(null);  // 강사/운영팀은 강의실 불필요
        
        Member updated = memberRepository.save(member);
        log.info("Member {} position approved to {}", targetMemberId, approvedPosition);
        return MemberResponse.from(updated);
    }
    
    // 직급 신청 거절
    public MemberResponse rejectPositionRequest(Long targetMemberId) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (member.getPendingPosition() == null) {
            throw new RuntimeException("No pending position request");
        }
        
        String rejectedPosition = member.getPendingPosition();
        member.setPendingPosition(null);  // 대기 중인 신청 제거
        
        Member updated = memberRepository.save(member);
        log.info("Member {} position request rejected (was: {})", targetMemberId, rejectedPosition);
        return MemberResponse.from(updated);
    }
    
    // 회원 직급 직접 변경 (관리자)
    public MemberResponse updateMemberPosition(Long targetMemberId, String position) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (!isValidPosition(position)) {
            throw new RuntimeException("Invalid position: " + position);
        }
        
        member.setPosition(position);
        member.setPendingPosition(null);
        
        // 수강생이 아니면 강의실 제거
        if (!"수강생".equals(position)) {
            member.setClassroom(null);
        }
        
        Member updated = memberRepository.save(member);
        log.info("Member {} position updated to {} by admin", targetMemberId, position);
        return MemberResponse.from(updated);
    }
    
    private boolean isValidPosition(String position) {
        return position != null && 
               (position.equals("운영팀") || position.equals("강사") || position.equals("수강생"));
    }
    
    // 회원 소속(branch) 변경 (관리자)
    public MemberResponse updateMemberBranch(Long targetMemberId, String branch, String classroom) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (!isValidBranch(branch)) {
            throw new RuntimeException("Invalid branch: " + branch);
        }
        
        member.setBranch(branch);
        
        // 강의실 설정 (수강생만)
        if ("수강생".equals(member.getPosition()) && classroom != null && !classroom.isEmpty()) {
            if (!isValidClassroom(branch, classroom)) {
                throw new RuntimeException("Invalid classroom: " + classroom + " for branch: " + branch);
            }
            member.setClassroom(classroom);
        } else {
            member.setClassroom(null);
        }
        
        Member updated = memberRepository.save(member);
        log.info("Member {} branch updated to {} (classroom: {}) by admin", targetMemberId, branch, classroom);
        return MemberResponse.from(updated);
    }
    
    // 회원 기수(cohort) 변경 (관리자)
    public MemberResponse updateMemberCohort(Long targetMemberId, String cohort) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        member.setCohort(cohort);
        
        Member updated = memberRepository.save(member);
        log.info("Member {} cohort updated to {} by admin", targetMemberId, cohort);
        return MemberResponse.from(updated);
    }
    
    private boolean isValidBranch(String branch) {
        return branch != null && (branch.equals("종로") || branch.equals("강남"));
    }
    
    private boolean isValidClassroom(String branch, String classroom) {
        if ("종로".equals(branch)) {
            return classroom.matches("^(301|302|501|502)$");
        } else if ("강남".equals(branch)) {
            return classroom.matches("^[A-DGHIQRSTU]$");
        }
        return false;
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
        
        // 직급 신청 대기 수
        stats.put("pendingPositionRequests", memberRepository.findByPendingPositionIsNotNull().size());
        
        return stats;
    }
}

package com.portfolio.builder.portfolio.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.feedback.domain.FeedbackRepository;
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
    private final CommentRepository commentRepository;
    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeService badgeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // 현재 사용자 조회
        Member currentMember = memberRepository.findById(memberId).orElse(null);
        
        // 강사/운영팀은 모든 포트폴리오 조회 가능
        boolean isStaff = currentMember != null && 
                         ("강사".equals(currentMember.getPosition()) || "운영팀".equals(currentMember.getPosition()));
        
        // 본인 포트폴리오이거나 공개된 포트폴리오이거나 강사/운영팀만 조회 가능
        if (!portfolio.getMember().getId().equals(memberId) && !portfolio.getIsPublic() && !isStaff) {
            throw new RuntimeException("Access denied");
        }

        int likeCount = portfolioLikeRepository.countByPortfolioId(portfolioId);
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

        // 관련 피드백 삭제
        feedbackRepository.deleteAllByPortfolioId(portfolioId);
        
        // 관련 댓글 삭제
        commentRepository.deleteAllByPortfolio(portfolio);
        
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
                    PortfolioResponse.SelectedBadgeInfo selectedBadgeInfo = null;
                    
                    if (ownerId != null) {
                        badgeCount = (int) badgeRepository.countByMemberId(ownerId);
                        recentBadges = badgeRepository.findTop4ByMemberIdOrderByEarnedAtDesc(ownerId)
                                .stream()
                                .map(badge -> badgeService.getBadgeIcon(badge.getBadgeId()))
                                .collect(Collectors.toList());
                        
                        // 대표 배지 정보 조회
                        Member owner = portfolio.getMember();
                        if (owner.getSelectedBadgeId() != null) {
                            String badgeId = owner.getSelectedBadgeId();
                            selectedBadgeInfo = PortfolioResponse.SelectedBadgeInfo.builder()
                                    .id(badgeId)
                                    .icon(badgeService.getBadgeIcon(badgeId))
                                    .name(badgeService.getBadgeName(badgeId))
                                    .description(badgeService.getBadgeDescription(badgeId))
                                    .build();
                        }
                    }
                    
                    return PortfolioResponse.from(portfolio, likeCount, isLiked, badgeCount, recentBadges, selectedBadgeInfo);
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
                    PortfolioResponse.SelectedBadgeInfo selectedBadgeInfo = null;
                    
                    if (ownerId != null) {
                        badgeCount = (int) badgeRepository.countByMemberId(ownerId);
                        recentBadges = badgeRepository.findTop4ByMemberIdOrderByEarnedAtDesc(ownerId)
                                .stream()
                                .map(badge -> badgeService.getBadgeIcon(badge.getBadgeId()))
                                .collect(Collectors.toList());
                        
                        // 대표 배지 정보 조회
                        Member owner = portfolio.getMember();
                        if (owner.getSelectedBadgeId() != null) {
                            String badgeId = owner.getSelectedBadgeId();
                            selectedBadgeInfo = PortfolioResponse.SelectedBadgeInfo.builder()
                                    .id(badgeId)
                                    .icon(badgeService.getBadgeIcon(badgeId))
                                    .name(badgeService.getBadgeName(badgeId))
                                    .description(badgeService.getBadgeDescription(badgeId))
                                    .build();
                        }
                    }
                    
                    return PortfolioResponse.from(portfolio, likeCount, isLiked, badgeCount, recentBadges, selectedBadgeInfo);
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
        
        // 본인 여부 확인
        boolean isOwner = currentMember != null && 
                portfolio.getMember() != null && 
                currentMember.getId().equals(portfolio.getMember().getId());
        
        // 비공개 포트폴리오는 운영팀/강사/본인만 조회 가능
        if (!portfolio.getIsPublic() && !isStaff && !isOwner) {
            throw new RuntimeException("This portfolio is not public");
        }

        int likeCount = portfolioLikeRepository.countByPortfolioId(portfolioId);
        boolean isLiked = currentMember != null && 
                         portfolioLikeRepository.existsByPortfolioAndMember(portfolio, currentMember);

        PortfolioResponse response = PortfolioResponse.from(portfolio, likeCount, isLiked);
        
        // 본인/강사/운영팀이 아니면 개인정보 마스킹
        if (!isOwner && !isStaff) {
            response.setData(maskPersonalInfo(portfolio.getData()));
        }
        
        return response;
    }
    
    /**
     * 포트폴리오 데이터에서 개인정보 마스킹
     */
    private String maskPersonalInfo(String dataJson) {
        if (dataJson == null || dataJson.isEmpty()) {
            return dataJson;
        }
        
        try {
            Map<String, Object> data = objectMapper.readValue(dataJson, new TypeReference<Map<String, Object>>() {});
            
            // 이메일 마스킹
            if (data.containsKey("email") && data.get("email") != null) {
                data.put("email", maskEmail((String) data.get("email")));
            }
            
            // 전화번호 마스킹
            if (data.containsKey("phone") && data.get("phone") != null) {
                data.put("phone", maskPhone((String) data.get("phone")));
            }
            
            // 학력 마스킹
            if (data.containsKey("educations") && data.get("educations") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> educations = (List<Map<String, Object>>) data.get("educations");
                for (Map<String, Object> edu : educations) {
                    if (edu.containsKey("school") && edu.get("school") != null) {
                        edu.put("school", maskSchool((String) edu.get("school")));
                    }
                }
            }
            
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to mask personal info", e);
            return dataJson;
        }
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.length() <= 2 
                ? local.charAt(0) + "*".repeat(local.length() - 1)
                : local.substring(0, 2) + "*".repeat(local.length() - 2);
        return maskedLocal + "@" + domain;
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        // 숫자만 추출
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7) return phone;
        
        // 010-1234-5678 또는 01012345678 형식 모두 처리
        if (digits.length() == 11) {
            // 휴대폰 번호: 010-****-5678 형식으로 마스킹
            return digits.substring(0, 3) + "-****-" + digits.substring(7);
        } else if (digits.length() == 10) {
            // 지역번호 포함: 02-****-5678 형식으로 마스킹
            return digits.substring(0, 2) + "-****-" + digits.substring(6);
        }
        // 그 외의 경우
        return phone.replaceAll("\\d(?=\\d{4})", "*");
    }
    
    private String maskSchool(String school) {
        if (school == null || school.isEmpty()) return school;
        
        // 앞 두 글자만 OO으로 치환
        if (school.length() <= 2) return "OO";
        return "OO" + school.substring(2);
    }
}

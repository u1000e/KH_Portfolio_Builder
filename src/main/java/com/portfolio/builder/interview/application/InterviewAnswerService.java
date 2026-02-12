package com.portfolio.builder.interview.application;

import com.portfolio.builder.global.exception.NotFoundException;
import com.portfolio.builder.global.exception.ForbiddenException;
import com.portfolio.builder.interview.domain.*;
import com.portfolio.builder.interview.dto.InterviewAnswerRequest;
import com.portfolio.builder.interview.dto.InterviewAnswerResponse;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.quiz.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InterviewAnswerService {

    private final InterviewAnswerRepository answerRepository;
    private final InterviewAnswerLikeRepository likeRepository;
    private final InterviewQuestionRepository questionRepository;
    private final MemberRepository memberRepository;
    private final BadgeService badgeService;

    // 배지 ID
    private static final String BADGE_BEST_ANSWER_1ST = "hidden_best_answer_1st";
    private static final String BADGE_BEST_ANSWER_2ND = "hidden_best_answer_2nd";
    private static final String BADGE_BEST_ANSWER_3RD = "hidden_best_answer_3rd";
    private static final String BADGE_DISCUSSION_MASTER = "hidden_discussion_master";

    /**
     * 답변 생성
     */
    @Transactional
    public InterviewAnswerResponse createAnswer(Long questionId, Long memberId, InterviewAnswerRequest request) {
        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("질문을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        InterviewAnswer answer = InterviewAnswer.builder()
                .question(question)
                .member(member)
                .content(request.getContent())
                .build();

        answerRepository.save(answer);

        // 토론왕 배지 체크 (100개 이상)
        checkDiscussionMasterBadge(memberId);

        return toResponse(answer, memberId);
    }

    /**
     * 답변 수정 (본인만)
     */
    @Transactional
    public InterviewAnswerResponse updateAnswer(Long answerId, Long memberId, InterviewAnswerRequest request) {
        InterviewAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("답변을 찾을 수 없습니다."));

        if (!answer.getMember().getId().equals(memberId)) {
            throw new ForbiddenException("본인의 답변만 수정할 수 있습니다.");
        }

        answer.setContent(request.getContent());
        answerRepository.save(answer);

        return toResponse(answer, memberId);
    }

    /**
     * 답변 삭제 (본인만)
     */
    @Transactional
    public void deleteAnswer(Long answerId, Long memberId) {
        InterviewAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("답변을 찾을 수 없습니다."));

        if (!answer.getMember().getId().equals(memberId)) {
            throw new ForbiddenException("본인의 답변만 삭제할 수 있습니다.");
        }

        // 좋아요 먼저 삭제
        likeRepository.deleteAllByAnswerId(answerId);
        answerRepository.delete(answer);
    }

    /**
     * 좋아요 토글
     */
    @Transactional
    public InterviewAnswerResponse toggleLike(Long answerId, Long memberId) {
        InterviewAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("답변을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        Optional<InterviewAnswerLike> existingLike = likeRepository.findByAnswerIdAndMemberId(answerId, memberId);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            likeRepository.delete(existingLike.get());
            answer.decreaseLikeCount();
        } else {
            // 좋아요 추가
            InterviewAnswerLike like = InterviewAnswerLike.builder()
                    .answer(answer)
                    .member(member)
                    .build();
            likeRepository.save(like);
            answer.increaseLikeCount();

            // 베스트 답변자 배지 체크 (답변 작성자에게)
            checkBestAnswerBadges(answer.getMember().getId());
        }

        answerRepository.save(answer);
        return toResponse(answer, memberId);
    }

    /**
     * 특정 질문의 답변 목록 조회 (상위 3개는 좋아요순, 나머지는 오래된순)
     */
    public Page<InterviewAnswerResponse> getAnswersByQuestion(Long questionId, Long memberId, int page, int size) {
        // 해당 질문의 좋아요 상위 3개 답변 ID 조회
        List<Long> top3Ids = answerRepository.findTop3AnswerIdsByQuestionId(questionId, PageRequest.of(0, 3));
        List<Long> top3IdsForQuery = top3Ids.isEmpty() ? List.of(-1L) : top3Ids;

        // 커스텀 정렬로 조회 (상위 3개 먼저, 나머지 오래된순)
        Pageable pageable = PageRequest.of(page, size);
        Page<InterviewAnswer> answers = answerRepository.findByQuestionIdWithTop3First(questionId, top3IdsForQuery, pageable);

        return answers.map(answer -> {
            InterviewAnswerResponse response = toResponse(answer, memberId);
            // 상위 3개 중 하나면 rank 설정
            int rank = top3Ids.indexOf(answer.getId());
            if (rank >= 0 && rank < 3) {
                response.setRank(rank + 1);
            }
            return response;
        });
    }

    /**
     * 특정 질문의 답변 개수
     */
    public long getAnswerCount(Long questionId) {
        return answerRepository.countByQuestionId(questionId);
    }

    /**
     * 토론왕 배지 체크 (답변 100개 이상)
     */
    private void checkDiscussionMasterBadge(Long memberId) {
        long answerCount = answerRepository.countByMemberId(memberId);
        if (answerCount >= 100) {
            boolean awarded = badgeService.awardHiddenBadge(memberId, BADGE_DISCUSSION_MASTER);
            if (awarded) {
                log.info("회원 {}에게 토론왕 배지 부여 (답변 {}개)", memberId, answerCount);
            }
        }
    }

    /**
     * 베스트 답변자 배지 체크 (총 좋아요 기준)
     * - 면접 해결사: 총 좋아요 100개 이상
     * - 지식 전도사: 총 좋아요 50개 이상
     * - 답변메이트: 총 좋아요 10개 이상
     */
    private void checkBestAnswerBadges(Long memberId) {
        int totalLikes = answerRepository.sumLikeCountByMemberId(memberId);

        // 답변메이트: 10개 이상
        if (totalLikes >= 10) {
            boolean awarded = badgeService.awardHiddenBadge(memberId, BADGE_BEST_ANSWER_3RD);
            if (awarded) {
                log.info("회원 {}에게 답변메이트 배지 부여 (총 좋아요 {}개)", memberId, totalLikes);
            }
        }

        // 지식 전도사: 50개 이상
        if (totalLikes >= 50) {
            boolean awarded = badgeService.awardHiddenBadge(memberId, BADGE_BEST_ANSWER_2ND);
            if (awarded) {
                log.info("회원 {}에게 지식 전도사 배지 부여 (총 좋아요 {}개)", memberId, totalLikes);
            }
        }

        // 면접 해결사: 100개 이상
        if (totalLikes >= 100) {
            boolean awarded = badgeService.awardHiddenBadge(memberId, BADGE_BEST_ANSWER_1ST);
            if (awarded) {
                log.info("회원 {}에게 면접 해결사 배지 부여 (총 좋아요 {}개)", memberId, totalLikes);
            }
        }
    }

    /**
     * 엔티티를 Response DTO로 변환
     */
    private InterviewAnswerResponse toResponse(InterviewAnswer answer, Long currentMemberId) {
        Member member = answer.getMember();
        String displayName = member.getName() != null ? member.getName() : member.getGithubUsername();

        boolean isLiked = currentMemberId != null &&
                likeRepository.existsByAnswerIdAndMemberId(answer.getId(), currentMemberId);

        boolean isOwner = currentMemberId != null &&
                answer.getMember().getId().equals(currentMemberId);

        return InterviewAnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .memberId(member.getId())
                .memberName(displayName)
                .memberAvatarUrl(member.getAvatarUrl())
                .content(answer.getContent())
                .likeCount(answer.getLikeCount())
                .isLiked(isLiked)
                .isOwner(isOwner)
                .isHidden(Boolean.TRUE.equals(answer.getIsHidden()))
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .build();
    }

    /**
     * 내가 작성한 답변 목록 조회 (카테고리/키워드 필터, 최신순, 페이징)
     */
    public Page<MyAnswerResponse> getMyAnswers(Long memberId, String category, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String categoryParam = (category != null && !category.isEmpty()) ? category : null;
        String keywordParam = (keyword != null && !keyword.isEmpty()) ? keyword : null;

        Page<InterviewAnswer> answers = answerRepository.findByMemberIdWithFilters(memberId, categoryParam, keywordParam, pageable);

        return answers.map(answer -> MyAnswerResponse.builder()
                .id(answer.getId())
                .content(answer.getContent())
                .likeCount(answer.getLikeCount())
                .createdAt(answer.getCreatedAt())
                .questionId(answer.getQuestion().getId())
                .question(answer.getQuestion().getQuestion())
                .category(answer.getQuestion().getCategory())
                .company(answer.getQuestion().getCompany())
                .build());
    }

    /**
     * 내가 좋아요한 답변 목록 조회 (카테고리/키워드 필터, 최신순, 페이징)
     */
    public Page<LikedAnswerResponse> getMyLikedAnswers(Long memberId, String category, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String categoryParam = (category != null && !category.isEmpty()) ? category : null;
        String keywordParam = (keyword != null && !keyword.isEmpty()) ? keyword : null;

        Page<InterviewAnswerLike> likes = likeRepository.findByMemberIdWithFilters(memberId, categoryParam, keywordParam, pageable);

        return likes.map(like -> {
            InterviewAnswer answer = like.getAnswer();
            Member author = answer.getMember();
            String authorName = author.getName() != null ? author.getName() : author.getGithubUsername();

            return LikedAnswerResponse.builder()
                    .id(answer.getId())
                    .content(answer.getContent())
                    .likeCount(answer.getLikeCount())
                    .likedAt(like.getCreatedAt())
                    .authorName(authorName)
                    .authorAvatarUrl(author.getAvatarUrl())
                    .questionId(answer.getQuestion().getId())
                    .question(answer.getQuestion().getQuestion())
                    .category(answer.getQuestion().getCategory())
                    .company(answer.getQuestion().getCompany())
                    .build();
        });
    }

    /**
     * 내가 답변한 질문 ID 목록 조회
     */
    public List<Long> getMyAnsweredQuestionIds(Long memberId) {
        return answerRepository.findAnsweredQuestionIdsByMemberId(memberId);
    }

    /**
     * 핫한 토론 조회 (일주일 내 답변이 많이 달린 질문 상위 N개)
     */
    public List<HotQuestionResponse> getHotQuestions(int limit) {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> hotQuestionData = answerRepository.findHotQuestionIds(since, pageable);

        if (hotQuestionData.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = hotQuestionData.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        // 답변 수 맵 생성
        Map<Long, Long> answerCountMap = hotQuestionData.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 질문 정보 조회
        List<InterviewQuestion> questions = questionRepository.findAllById(questionIds);
        Map<Long, InterviewQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(InterviewQuestion::getId, q -> q));

        // 순서 유지하면서 응답 생성
        return questionIds.stream()
                .map(questionId -> {
                    InterviewQuestion q = questionMap.get(questionId);
                    if (q == null) return null;

                    return HotQuestionResponse.builder()
                            .questionId(q.getId())
                            .question(q.getQuestion())
                            .category(q.getCategory())
                            .company(q.getCompany())
                            .answerCount(answerCountMap.get(questionId).intValue())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 답변이 가장 많이 달린 질문 조회 (전체 기간)
     */
    public List<TopQuestionResponse> getTopQuestionsByAnswerCount(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> topQuestionData = answerRepository.findTopQuestionsByAnswerCount(pageable);

        if (topQuestionData.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = topQuestionData.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        // 답변 수 맵 생성
        Map<Long, Long> answerCountMap = topQuestionData.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 질문 정보 조회
        List<InterviewQuestion> questions = questionRepository.findAllById(questionIds);
        Map<Long, InterviewQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(InterviewQuestion::getId, q -> q));

        // 순서 유지하면서 응답 생성
        return questionIds.stream()
                .map(questionId -> {
                    InterviewQuestion q = questionMap.get(questionId);
                    if (q == null) return null;

                    return TopQuestionResponse.builder()
                            .questionId(q.getId())
                            .question(q.getQuestion())
                            .category(q.getCategory())
                            .company(q.getCompany())
                            .answerCount(answerCountMap.get(questionId).intValue())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 핫한 토론 응답 DTO (내부 클래스)
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HotQuestionResponse {
        private Long questionId;
        private String question;
        private String category;
        private String company;
        private int answerCount;
    }

    /**
     * 답변 많은 질문 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TopQuestionResponse {
        private Long questionId;
        private String question;
        private String category;
        private String company;
        private int answerCount;
    }

    /**
     * 내가 작성한 답변 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MyAnswerResponse {
        private Long id;
        private String content;
        private int likeCount;
        private java.time.LocalDateTime createdAt;
        private Long questionId;
        private String question;
        private String category;
        private String company;
    }

    /**
     * 내가 좋아요한 답변 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LikedAnswerResponse {
        private Long id;
        private String content;
        private int likeCount;
        private java.time.LocalDateTime likedAt;
        private String authorName;
        private String authorAvatarUrl;
        private Long questionId;
        private String question;
        private String category;
        private String company;
    }
}

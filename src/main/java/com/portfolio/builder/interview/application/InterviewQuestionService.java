package com.portfolio.builder.interview.application;

import com.portfolio.builder.interview.domain.InterviewQuestion;
import com.portfolio.builder.interview.domain.InterviewQuestionRepository;
import com.portfolio.builder.interview.dto.InterviewQuestionRequest;
import com.portfolio.builder.interview.dto.InterviewQuestionResponse;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InterviewQuestionService {

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final MemberRepository memberRepository;

    // 카테고리 목록 (고정)
    public static final List<String> CATEGORIES = List.of(
            "기술/Java",
            "기술/Spring",
            "기술/DevOps",
            "기술/SQL",
            "기술/React",
            "기술/JavaScript",
            "기술/Network",
            "기술/CS",
            "기술/기타",
            "인성"
    );

    /**
     * 질문 목록 조회 (필터 적용)
     */
    public List<InterviewQuestionResponse> getQuestions(List<String> periods, List<String> categories) {
        List<InterviewQuestion> questions;

        boolean hasPeriods = periods != null && !periods.isEmpty();
        boolean hasCategories = categories != null && !categories.isEmpty();

        if (hasPeriods && hasCategories) {
            questions = interviewQuestionRepository.findByPeriodInAndCategoryInOrderByCategoryAscCreatedAtDesc(periods, categories);
        } else if (hasPeriods) {
            questions = interviewQuestionRepository.findByPeriodInOrderByCategoryAscCreatedAtDesc(periods);
        } else if (hasCategories) {
            questions = interviewQuestionRepository.findByCategoryInOrderByCategoryAscCreatedAtDesc(categories);
        } else {
            questions = interviewQuestionRepository.findAllByOrderByCreatedAtDesc();
        }

        return questions.stream()
                .map(InterviewQuestionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별로 그룹핑된 질문 조회
     */
    public Map<String, List<InterviewQuestionResponse>> getQuestionsGroupedByCategory(
            List<String> periods, List<String> categories) {
        List<InterviewQuestionResponse> questions = getQuestions(periods, categories);

        return questions.stream()
                .collect(Collectors.groupingBy(
                        InterviewQuestionResponse::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * 존재하는 기간 목록 조회
     */
    public List<String> getAvailablePeriods() {
        return interviewQuestionRepository.findDistinctPeriods();
    }

    /**
     * 카테고리 목록 조회 (고정 목록 반환)
     */
    public List<String> getCategories() {
        return CATEGORIES;
    }

    /**
     * 질문 추가 (강사/운영팀만)
     */
    @Transactional
    public InterviewQuestionResponse addQuestion(Long memberId, InterviewQuestionRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        // 권한 확인
        if (!isStaff(member)) {
            throw new IllegalStateException("질문 추가 권한이 없습니다");
        }

        InterviewQuestion question = InterviewQuestion.builder()
                .period(request.getPeriod())
                .category(request.getCategory())
                .question(request.getQuestion())
                .company(request.getCompany())
                .createdBy(member)
                .build();

        InterviewQuestion saved = interviewQuestionRepository.save(question);
        log.info("Interview question added - id: {}, category: {}, by: {}",
                saved.getId(), saved.getCategory(), memberId);

        return InterviewQuestionResponse.from(saved);
    }

    /**
     * 질문 삭제 (작성자 또는 관리자만)
     */
    @Transactional
    public void deleteQuestion(Long memberId, Long questionId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        InterviewQuestion question = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다"));

        // 권한 확인: 작성자 또는 관리자
        boolean isAuthor = question.getCreatedBy().getId().equals(memberId);
        boolean isAdmin = "ADMIN".equals(member.getRole());

        if (!isAuthor && !isAdmin) {
            throw new IllegalStateException("질문 삭제 권한이 없습니다");
        }

        interviewQuestionRepository.delete(question);
        log.info("Interview question deleted - id: {}, by: {}", questionId, memberId);
    }

    /**
     * 기간별 통계
     */
    public Map<String, Object> getStatistics(List<String> periods) {
        if (periods == null || periods.isEmpty()) {
            periods = interviewQuestionRepository.findDistinctPeriods();
        }

        List<Object[]> categoryStats = interviewQuestionRepository.countByCategoryAndPeriods(periods);

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        long totalCount = 0;

        for (Object[] row : categoryStats) {
            String category = (String) row[0];
            Long count = (Long) row[1];
            categoryCounts.put(category, count);
            totalCount += count;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", totalCount);
        result.put("categoryCounts", categoryCounts);
        result.put("periods", periods);

        return result;
    }

    private boolean isStaff(Member member) {
        String position = member.getPosition();
        return "운영팀".equals(position) || "강사".equals(position);
    }
}

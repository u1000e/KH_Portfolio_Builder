package com.portfolio.builder.instructor.application;

import com.portfolio.builder.feedback.domain.FeedbackRepository;
import com.portfolio.builder.instructor.dto.ClassDashboardResponse;
import com.portfolio.builder.instructor.dto.ClassStatistics;
import com.portfolio.builder.instructor.dto.StudentStatusDto;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service 
@RequiredArgsConstructor 
@Slf4j
@Transactional(readOnly = true)
public class InstructorService {

    private final MemberRepository memberRepository;
    private final PortfolioRepository portfolioRepository;
    private final FeedbackRepository feedbackRepository;

    /**
     * 강사/운영팀 권한 확인
     */
    public void validateInstructor(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        String position = member.getPosition();
        if (!"운영팀".equals(position) && !"강사".equals(position)) {
            throw new IllegalStateException("강사 또는 운영팀만 접근할 수 있습니다");
        }
    }

    /**
     * 반별 현황 대시보드 조회
     */
    public ClassDashboardResponse getClassDashboard(String branch, String classroom, String cohort) {
        // 1. 수강생 목록 조회 (필터 적용)
        List<Member> students = memberRepository.findStudentsByFilters(branch, classroom, cohort);

        if (students.isEmpty()) {
            return ClassDashboardResponse.builder()
                    .statistics(ClassStatistics.builder()
                            .totalStudents(0)
                            .portfolioCount(0)
                            .completionRate(0.0)
                            .averageAiScore(null)
                            .totalFeedbacks(0)
                            .build())
                    .students(new ArrayList<>())
                    .build();
        }

        // 2. 수강생 ID 목록
        List<Long> studentIds = students.stream()
                .map(Member::getId)
                .collect(Collectors.toList());

        // 3. 각 수강생의 포트폴리오 조회 (첫 번째 포트폴리오만)
        Map<Long, List<Portfolio>> portfoliosByMember = students.stream()
                .collect(Collectors.toMap(
                        Member::getId,
                        m -> portfolioRepository.findByMember(m)
                ));

        // 4. 통계 계산
        int totalStudents = students.size();
        int portfolioCount = 0;
        long totalFeedbacks = 0;
        List<Integer> aiScores = new ArrayList<>();

        for (Member student : students) {
            List<Portfolio> memberPortfolios = portfoliosByMember.get(student.getId());
            if (memberPortfolios != null && !memberPortfolios.isEmpty()) {
                portfolioCount++;
                Portfolio portfolio = memberPortfolios.get(0);
                if (portfolio.getAiScore() != null) {
                    aiScores.add(portfolio.getAiScore());
                }
                totalFeedbacks += feedbackRepository.countByPortfolioId(portfolio.getId());
            }
        }

        double completionRate = totalStudents > 0
                ? Math.round((double) portfolioCount / totalStudents * 1000) / 10.0
                : 0.0;

        Double averageAiScore = aiScores.isEmpty()
                ? null
                : Math.round(aiScores.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0;

        ClassStatistics statistics = ClassStatistics.builder()
                .totalStudents(totalStudents)
                .portfolioCount(portfolioCount)
                .completionRate(completionRate)
                .averageAiScore(averageAiScore)
                .totalFeedbacks(totalFeedbacks)
                .build();

        // 5. 학생별 상태 DTO 생성
        List<StudentStatusDto> studentDtos = students.stream()
                .map(student -> {
                    List<Portfolio> memberPortfolios = portfoliosByMember.get(student.getId());
                    Portfolio portfolio = (memberPortfolios != null && !memberPortfolios.isEmpty())
                            ? memberPortfolios.get(0)
                            : null;

                    long feedbackCount = portfolio != null
                            ? feedbackRepository.countByPortfolioId(portfolio.getId())
                            : 0;

                    return StudentStatusDto.builder()
                            .id(student.getId())
                            .name(student.getName() != null ? student.getName() : "이름 없음")
                            .githubUsername(student.getGithubUsername())
                            .avatarUrl(student.getAvatarUrl())
                            .hasPortfolio(portfolio != null)
                            .portfolioId(portfolio != null ? portfolio.getId() : null)
                            .portfolioTitle(portfolio != null ? portfolio.getTitle() : null)
                            .aiScore(portfolio != null ? portfolio.getAiScore() : null)
                            .feedbackCount(feedbackCount)
                            .lastUpdated(portfolio != null ? portfolio.getUpdatedAt() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return ClassDashboardResponse.builder()
                .statistics(statistics)
                .students(studentDtos)
                .build();
    }
}

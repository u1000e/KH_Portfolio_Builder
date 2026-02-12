package com.portfolio.builder.instructor.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.builder.feedback.domain.FeedbackRepository;
import com.portfolio.builder.instructor.dto.CategoryAccuracy;
import com.portfolio.builder.instructor.dto.ClassDashboardResponse;
import com.portfolio.builder.instructor.dto.ClassStatistics;
import com.portfolio.builder.instructor.dto.InterviewDashboardResponse;
import com.portfolio.builder.instructor.dto.InterviewStatistics;
import com.portfolio.builder.instructor.dto.OverviewDashboardResponse;
import com.portfolio.builder.instructor.dto.OverviewStatistics;
import com.portfolio.builder.instructor.dto.QuizDashboardResponse;
import com.portfolio.builder.instructor.dto.QuizStatistics;
import com.portfolio.builder.instructor.dto.StudentInterviewStatusDto;
import com.portfolio.builder.instructor.dto.StudentOverviewDto;
import com.portfolio.builder.instructor.dto.StudentQuizStatusDto;
import com.portfolio.builder.instructor.dto.StudentStatusDto;
import com.portfolio.builder.interview.domain.InterviewAnswerRepository;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.portfolio.domain.Portfolio;
import com.portfolio.builder.portfolio.domain.PortfolioRepository;
import com.portfolio.builder.quiz.domain.QuizStreak;
import com.portfolio.builder.quiz.repository.QuizAttemptRepository;
import com.portfolio.builder.quiz.repository.QuizStreakRepository;
import com.portfolio.builder.til.domain.TILRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InstructorService {

    private final MemberRepository memberRepository;
    private final PortfolioRepository portfolioRepository;
    private final FeedbackRepository feedbackRepository;
    private final QuizStreakRepository quizStreakRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TILRepository tilRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;

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
     * 반별 포트폴리오 현황 대시보드 조회
     */
    public ClassDashboardResponse getClassDashboard(String branch, String classroom, String cohort) {
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

        Map<Long, List<Portfolio>> portfoliosByMember = students.stream()
                .collect(Collectors.toMap(
                        Member::getId,
                        m -> portfolioRepository.findByMember(m)
                ));

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

        List<StudentStatusDto> studentDtos = students.stream()
                .map(student -> {
                    List<Portfolio> memberPortfolios = portfoliosByMember.get(student.getId());
                    Portfolio portfolio = (memberPortfolios != null && !memberPortfolios.isEmpty())
                            ? memberPortfolios.get(0)
                            : null;

                    long feedbackCount = portfolio != null
                            ? feedbackRepository.countByPortfolioId(portfolio.getId())
                            : 0;

                    long unresolvedFeedbackCount = portfolio != null
                            ? feedbackRepository.countUnresolvedByPortfolioId(portfolio.getId())
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
                            .unresolvedFeedbackCount(unresolvedFeedbackCount)
                            .lastUpdated(portfolio != null ? portfolio.getUpdatedAt() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return ClassDashboardResponse.builder()
                .statistics(statistics)
                .students(studentDtos)
                .build();
    }

    /**
     * 반별 퀴즈 현황 대시보드 조회
     */
    public QuizDashboardResponse getQuizDashboard(String branch, String classroom, String cohort) {
        List<Member> students = memberRepository.findStudentsByFilters(branch, classroom, cohort);

        if (students.isEmpty()) {
            return QuizDashboardResponse.builder()
                    .statistics(QuizStatistics.builder()
                            .totalStudents(0)
                            .participants(0)
                            .participationRate(0.0)
                            .averageAccuracy(null)
                            .strongCategories(new ArrayList<>())
                            .weakCategories(new ArrayList<>())
                            .build())
                    .students(new ArrayList<>())
                    .build();
        }

        int totalStudents = students.size();
        int participants = 0;
        List<Double> accuracies = new ArrayList<>();

        // 반 전체 카테고리별 통계
        Map<String, Long> totalByCategory = new HashMap<>();
        Map<String, Long> correctByCategory = new HashMap<>();

        List<StudentQuizStatusDto> studentDtos = new ArrayList<>();

        for (Member student : students) {
            QuizStreak streak = quizStreakRepository.findByMemberId(student.getId()).orElse(null);

            if (streak != null && streak.getTotalQuizCount() > 0) {
                participants++;
                double accuracy = (double) streak.getCorrectCount() / streak.getTotalQuizCount() * 100;
                accuracies.add(accuracy);
            }

            // 학생별 카테고리 통계 조회
            List<Object[]> categoryStats = quizAttemptRepository.countSolvedByMemberIdGroupByCategory(student.getId());
            Map<String, CategoryAccuracy> studentCategories = new HashMap<>();

            for (Object[] stat : categoryStats) {
                String category = (String) stat[0];
                Long solved = (Long) stat[1];

                Long correct = quizAttemptRepository.countCorrectByMemberIdAndCategory(student.getId(), category);

                // 반 전체 통계에 합산
                totalByCategory.merge(category, solved, Long::sum);
                correctByCategory.merge(category, correct, Long::sum);

                // 학생별 카테고리 정답률
                if (solved > 0) {
                    double catAccuracy = Math.round((double) correct / solved * 1000) / 10.0;
                    studentCategories.put(category, new CategoryAccuracy(category, catAccuracy, solved.intValue()));
                }
            }

            // 강점/취약점 분석 (최소 3문제 이상 푼 카테고리)
            List<CategoryAccuracy> validCategories = studentCategories.values().stream()
                    .filter(c -> c.getSolvedCount() >= 3)
                    .collect(Collectors.toList());

            String strongCategory = validCategories.stream()
                    .max(Comparator.comparingDouble(CategoryAccuracy::getAccuracy))
                    .map(CategoryAccuracy::getCategory)
                    .orElse(null);

            String weakCategory = validCategories.stream()
                    .min(Comparator.comparingDouble(CategoryAccuracy::getAccuracy))
                    .map(CategoryAccuracy::getCategory)
                    .orElse(null);

            // 마지막 학습일 (QuizStreak의 lastStudyDate)
            LocalDate lastStudyDate = streak != null ? streak.getLastStudyDate() : null;

            studentDtos.add(StudentQuizStatusDto.builder()
                    .id(student.getId())
                    .name(student.getName() != null ? student.getName() : "이름 없음")
                    .githubUsername(student.getGithubUsername())
                    .avatarUrl(student.getAvatarUrl())
                    .totalQuizCount(streak != null ? streak.getTotalQuizCount() : 0)
                    .correctCount(streak != null ? streak.getCorrectCount() : 0)
                    .accuracy(streak != null && streak.getTotalQuizCount() > 0
                            ? Math.round((double) streak.getCorrectCount() / streak.getTotalQuizCount() * 1000) / 10.0
                            : null)
                    .currentStreak(streak != null ? streak.getCurrentStreak() : 0)
                    .strongCategory(strongCategory)
                    .weakCategory(weakCategory)
                    .lastStudyDate(lastStudyDate)
                    .build());
        }

        // 반 전체 통계 계산
        double participationRate = totalStudents > 0
                ? Math.round((double) participants / totalStudents * 1000) / 10.0
                : 0.0;

        Double averageAccuracy = accuracies.isEmpty()
                ? null
                : Math.round(accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 10) / 10.0;

        // 반 전체 강점/취약 카테고리 (최소 10문제 이상)
        List<CategoryAccuracy> classCategoryStats = totalByCategory.entrySet().stream()
                .filter(e -> e.getValue() >= 10)
                .map(e -> {
                    String cat = e.getKey();
                    long total = e.getValue();
                    long correct = correctByCategory.getOrDefault(cat, 0L);
                    double acc = Math.round((double) correct / total * 1000) / 10.0;
                    return new CategoryAccuracy(cat, acc, (int) total);
                })
                .collect(Collectors.toList());
 
        List<String> strongCategories = classCategoryStats.stream()
                .filter(c -> c.getAccuracy() >= 70)
                .sorted((a, b) -> Double.compare(b.getAccuracy(), a.getAccuracy()))
                .limit(3)
                .map(CategoryAccuracy::getCategory)
                .collect(Collectors.toList());

        List<String> weakCategories = classCategoryStats.stream()
                .filter(c -> c.getAccuracy() < 60)
                .sorted(Comparator.comparingDouble(CategoryAccuracy::getAccuracy))
                .limit(3)
                .map(CategoryAccuracy::getCategory)
                .collect(Collectors.toList());

        QuizStatistics statistics = QuizStatistics.builder()
                .totalStudents(totalStudents)
                .participants(participants)
                .participationRate(participationRate)
                .averageAccuracy(averageAccuracy)
                .strongCategories(strongCategories)
                .weakCategories(weakCategories)
                .build();

        return QuizDashboardResponse.builder()
                .statistics(statistics)
                .students(studentDtos)
                .build();
    }

    /**
     * 학생별 퀴즈 취약 카테고리 계산
     */
    private String calculateWeakCategory(Long memberId) {
        List<Object[]> categoryStats = quizAttemptRepository.countSolvedByMemberIdGroupByCategory(memberId);
        Map<String, CategoryAccuracy> studentCategories = new HashMap<>();

        for (Object[] stat : categoryStats) {
            String category = (String) stat[0];
            Long solved = (Long) stat[1];
            Long correct = quizAttemptRepository.countCorrectByMemberIdAndCategory(memberId, category);

            if (solved >= 3) {
                double catAccuracy = Math.round((double) correct / solved * 1000) / 10.0;
                studentCategories.put(category, new CategoryAccuracy(category, catAccuracy, solved.intValue()));
            }
        }

        return studentCategories.values().stream()
                .min(Comparator.comparingDouble(CategoryAccuracy::getAccuracy))
                .map(CategoryAccuracy::getCategory)
                .orElse(null);
    }

    /**
     * 반별 면접토론 현황 대시보드 조회
     */
    public InterviewDashboardResponse getInterviewDashboard(String branch, String classroom, String cohort) {
        List<Member> students = memberRepository.findStudentsByFilters(branch, classroom, cohort);

        if (students.isEmpty()) {
            return InterviewDashboardResponse.builder()
                    .statistics(InterviewStatistics.builder()
                            .totalStudents(0)
                            .participants(0)
                            .participationRate(0.0)
                            .totalAnswers(0)
                            .averageAnswers(0.0)
                            .totalLikes(0)
                            .build())
                    .students(new ArrayList<>())
                    .build();
        }

        // 배치 조회
        List<Object[]> interviewStats = interviewAnswerRepository.findInterviewStatsByClass(branch, classroom, cohort);
        Map<Long, Object[]> interviewMap = new HashMap<>();
        for (Object[] row : interviewStats) {
            interviewMap.put((Long) row[0], row);
        }

        int totalStudents = students.size();
        int participants = 0;
        long totalAnswers = 0;
        long totalLikes = 0;

        List<StudentInterviewStatusDto> studentDtos = new ArrayList<>();

        for (Member student : students) {
            Object[] data = interviewMap.get(student.getId());
            long answerCount = data != null ? ((Number) data[4]).longValue() : 0;
            long likes = data != null ? ((Number) data[5]).longValue() : 0;
            java.time.LocalDateTime lastAnswerDate = data != null ? (java.time.LocalDateTime) data[6] : null;

            if (answerCount > 0) {
                participants++;
                totalAnswers += answerCount;
                totalLikes += likes;
            }

            studentDtos.add(StudentInterviewStatusDto.builder()
                    .id(student.getId())
                    .name(student.getName() != null ? student.getName() : "이름 없음")
                    .githubUsername(student.getGithubUsername())
                    .avatarUrl(student.getAvatarUrl())
                    .answerCount(answerCount)
                    .totalLikes(likes)
                    .lastAnswerDate(lastAnswerDate)
                    .build());
        }

        double participationRate = totalStudents > 0
                ? Math.round((double) participants / totalStudents * 1000) / 10.0
                : 0.0;
        double averageAnswers = participants > 0
                ? Math.round((double) totalAnswers / participants * 10) / 10.0
                : 0.0;

        InterviewStatistics statistics = InterviewStatistics.builder()
                .totalStudents(totalStudents)
                .participants(participants)
                .participationRate(participationRate)
                .totalAnswers(totalAnswers)
                .averageAnswers(averageAnswers)
                .totalLikes(totalLikes)
                .build();

        return InterviewDashboardResponse.builder()
                .statistics(statistics)
                .students(studentDtos)
                .build();
    }

    /**
     * 반별 총괄 현황 대시보드 조회
     */
    public OverviewDashboardResponse getClassOverview(String branch, String classroom, String cohort) {
        List<Member> students = memberRepository.findStudentsByFilters(branch, classroom, cohort);

        if (students.isEmpty()) {
            return OverviewDashboardResponse.builder()
                    .statistics(OverviewStatistics.builder()
                            .totalStudents(0)
                            .portfolioRate(0.0)
                            .quizRate(0.0)
                            .tilRate(0.0)
                            .interviewRate(0.0)
                            .build())
                    .students(new ArrayList<>())
                    .build();
        }

        int totalStudents = students.size();

        // 배치 조회: TIL 통계
        List<Object[]> tilStats = tilRepository.findTilStatsByClass(branch, classroom, cohort);
        Map<Long, Long> tilCountMap = new HashMap<>();
        for (Object[] row : tilStats) {
            tilCountMap.put((Long) row[0], ((Number) row[4]).longValue());
        }

        // 배치 조회: 면접토론 통계
        List<Object[]> interviewStats = interviewAnswerRepository.findInterviewStatsByClass(branch, classroom, cohort);
        Map<Long, Long> interviewCountMap = new HashMap<>();
        for (Object[] row : interviewStats) {
            interviewCountMap.put((Long) row[0], ((Number) row[4]).longValue());
        }

        int portfolioCount = 0;
        int quizCount = 0;
        int tilCount = 0;
        int interviewCount = 0;

        List<StudentOverviewDto> studentDtos = new ArrayList<>();

        for (Member student : students) {
            // 포트폴리오
            List<Portfolio> portfolios = portfolioRepository.findByMember(student);
            boolean hasPortfolio = portfolios != null && !portfolios.isEmpty();
            if (hasPortfolio) portfolioCount++;

            // 퀴즈
            QuizStreak streak = quizStreakRepository.findByMemberId(student.getId()).orElse(null);
            boolean hasQuiz = streak != null && streak.getTotalQuizCount() > 0;
            int totalQuiz = streak != null ? streak.getTotalQuizCount() : 0;
            if (hasQuiz) quizCount++;

            // 퀴즈 취약 카테고리
            String weakCategory = hasQuiz ? calculateWeakCategory(student.getId()) : null;

            // TIL
            long studentTilCount = tilCountMap.getOrDefault(student.getId(), 0L);
            if (studentTilCount > 0) tilCount++;

            // 면접토론
            long studentInterviewCount = interviewCountMap.getOrDefault(student.getId(), 0L);
            if (studentInterviewCount > 0) interviewCount++;

            boolean inactive = !hasPortfolio && !hasQuiz && studentTilCount == 0 && studentInterviewCount == 0;

            studentDtos.add(StudentOverviewDto.builder()
                    .id(student.getId())
                    .name(student.getName() != null ? student.getName() : "이름 없음")
                    .githubUsername(student.getGithubUsername())
                    .avatarUrl(student.getAvatarUrl())
                    .hasPortfolio(hasPortfolio)
                    .hasQuiz(hasQuiz)
                    .totalQuizCount(totalQuiz)
                    .tilCount(studentTilCount)
                    .interviewAnswerCount(studentInterviewCount)
                    .quizWeakCategory(weakCategory)
                    .inactive(inactive)
                    .build());
        }

        double portfolioRate = totalStudents > 0
                ? Math.round((double) portfolioCount / totalStudents * 1000) / 10.0
                : 0.0;
        double quizRate = totalStudents > 0
                ? Math.round((double) quizCount / totalStudents * 1000) / 10.0
                : 0.0;
        double tilRate = totalStudents > 0
                ? Math.round((double) tilCount / totalStudents * 1000) / 10.0
                : 0.0;
        double interviewRate = totalStudents > 0
                ? Math.round((double) interviewCount / totalStudents * 1000) / 10.0
                : 0.0;

        OverviewStatistics statistics = OverviewStatistics.builder()
                .totalStudents(totalStudents)
                .portfolioRate(portfolioRate)
                .quizRate(quizRate)
                .tilRate(tilRate)
                .interviewRate(interviewRate)
                .build();

        return OverviewDashboardResponse.builder()
                .statistics(statistics)
                .students(studentDtos)
                .build();
    }
}

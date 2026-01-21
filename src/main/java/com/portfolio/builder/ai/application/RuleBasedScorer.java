package com.portfolio.builder.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.builder.ai.dto.PortfolioData;
import com.portfolio.builder.ai.dto.ScoreResult;
import com.portfolio.builder.portfolio.domain.Troubleshooting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 규칙 기반 포트폴리오 점수 계산기 (120점 만점)
 * - 완성도: 30점
 * - 기술력: 30점
 * - 트러블슈팅: 25점
 * - 활동성: 15점
 * - 표현력: 20점 (AI 평가 대상, 규칙 기반으로 기본 점수 산출)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleBasedScorer {
    
    private final ObjectMapper objectMapper;
    
    // 기술 키워드 패턴 (에러/예외)
    private static final List<String> ERROR_PATTERNS = List.of(
        "exception", "error", "에러", "오류", "실패", "버그", "bug", "예외", "예외처리",
        "stacktrace", "traceback", "warning", "경고", "충돌", "실패",
        "404", "500", "403", "401", "cors", "npe", "oom", "timeout",
        "null", "undefined", "crash", "memory", "leak", "deadlock"
    );
    
    // 기술명 패턴
    private static final List<String> TECH_PATTERNS = List.of(
        "react", "spring", "java", "javascript", "typescript", "mysql", "oracle",
        "mybatis", "thymeleaf", "lombok", "querydsl", "websocket",
        "boot", "security", "validation", "java", "c++", "c#",
        "docker", "aws", "jpa", "hibernate", "redis", "nginx", "tomcat",
        "jwt", "oauth", "api", "rest", "graphql", "webpack", "node", "python",
        "git", "linux", "gradle", "maven", "npm", "vue", "angular", "next",
        "kubernetes", "jenkins", "terraform", "mongodb", "postgresql", "ansible"
    );
    
    /**
     * 완성도 점수 (30점 만점)
     * - 이름: 3점
     * - 연락처: 2점
     * - 자기소개: 5점
     * - 스킬: 5점
     * - 프로젝트: 10점
     * - 학력/자격증: 5점
     */
    public ScoreResult calculateCompleteness(PortfolioData data) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        // 이름 (3점)
        if (hasValue(data.getName())) {
            score += 3;
        } else {
            details.add("이름을 입력해주세요");
        }
        
        // 연락처 - 이메일 또는 전화 (2점)
        if (hasValue(data.getEmail()) || hasValue(data.getPhone())) {
            score += 2;
        } else {
            details.add("연락처를 추가해주세요");
        }
        
        // 자기소개 길이 (5점)
        int introLength = getLength(data.getIntroduction());
        if (introLength >= 200) {
            score += 5;
        } else if (introLength >= 100) {
            score += 3;
            details.add("자기소개를 더 자세히 작성해보세요 (200자 이상 권장)");
        } else if (introLength > 0) {
            score += 1;
            details.add("자기소개가 너무 짧습니다");
        } else {
            details.add("자기소개를 작성해주세요");
        }
        
        // 스킬 개수 (5점)
        int skillCount = getSize(data.getSkillNames());
        if (skillCount >= 7) {
            score += 5;
        } else if (skillCount >= 4) {
            score += 3;
        } else if (skillCount > 0) {
            score += 1;
            details.add("스킬을 더 추가해보세요 (7개 이상 권장)");
        } else {
            details.add("보유 스킬을 입력해주세요");
        }
        
        // 프로젝트 개수 (10점)
        int projectCount = getSize(data.getProjects());
        if (projectCount >= 3) {
            score += 10;
        } else if (projectCount >= 2) {
            score += 7;
        } else if (projectCount >= 1) {
            score += 4;
            details.add("프로젝트를 더 추가해보세요 (3개 이상 권장)");
        } else {
            details.add("프로젝트를 추가해주세요");
        }
        
        // 학력/자격증 (5점) - NEW
        int educationCount = getSize(data.getEducations());
        int certCount = getSize(data.getCertificates());
        
        if (educationCount > 0 && certCount > 0) {
            score += 5;
        } else if (educationCount > 0 || certCount > 0) {
            score += 3;
            if (certCount == 0) {
                details.add("자격증을 추가하면 신뢰도가 올라갑니다");
            }
        } else {
            details.add("학력 또는 자격증을 추가해보세요");
        }
        
        return new ScoreResult(score, 30, details);
    }
    
    /**
     * 기술력 점수 (30점 만점)
     * - 기술 스택 다양성: 10점
     * - 프로젝트 설명 상세도: 8점
     * - 프로젝트 기술 스택 명시: 5점
     * - 프로젝트 링크 (GitHub/배포): 4점
     * - 기술 숙련도 표시: 3점
     */
    public ScoreResult calculateTechnical(PortfolioData data) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        List<String> skills = data.getSkillNames();
        
        // 기술 스택 다양성 (10점)
        Set<String> categories = categorizeTechStack(skills);
        if (categories.size() >= 4) {
            score += 10;
        } else if (categories.size() >= 3) {
            score += 7;
        } else if (categories.size() >= 2) {
            score += 4;
        } else if (categories.size() >= 1) {
            score += 2;
            details.add("다양한 분야의 기술 스택을 경험해보세요 (프론트엔드, 백엔드, DB, DevOps)");
        } else {
            details.add("기술 스택을 추가해주세요");
        }
        
        // 프로젝트 관련 점수
        if (data.getProjects() != null && !data.getProjects().isEmpty()) {
            // 프로젝트 설명 상세도 (8점)
            int avgDescLength = calculateAvgProjectDescLength(data.getProjects());
            if (avgDescLength >= 200) {
                score += 8;
            } else if (avgDescLength >= 100) {
                score += 5;
                details.add("프로젝트 설명을 더 상세히 작성해보세요 (200자 이상 권장)");
            } else if (avgDescLength > 0) {
                score += 2;
                details.add("프로젝트 설명이 너무 짧습니다");
            }
            
            // 프로젝트에 기술 스택 명시 (5점)
            long techStackCount = data.getProjects().stream()
                .filter(PortfolioData.ProjectData::hasTechStack)
                .count();
            if (techStackCount == data.getProjects().size()) {
                score += 5;
            } else if (techStackCount > 0) {
                score += 3;
                details.add("모든 프로젝트에 기술 스택을 명시해주세요");
            } else {
                details.add("프로젝트에 사용한 기술 스택을 명시해주세요");
            }
            
            // 프로젝트 링크 (4점) - NEW
            boolean hasGithub = data.getProjects().stream()
                .anyMatch(p -> hasValue(p.getGithubUrl()));
            boolean hasDemoOrLink = data.getProjects().stream()
                .anyMatch(p -> hasValue(p.getDemoUrl()) || hasValue(p.getLink()));
            
            if (hasGithub && hasDemoOrLink) {
                score += 4;
            } else if (hasGithub) {
                score += 3;
                details.add("배포 URL을 추가하면 실제 결과물을 보여줄 수 있습니다");
            } else if (hasDemoOrLink) {
                score += 2;
                details.add("GitHub 링크를 추가하면 코드를 보여줄 수 있습니다");
            } else {
                details.add("프로젝트에 GitHub 또는 배포 링크를 추가해보세요");
            }
        } else {
            details.add("프로젝트를 추가하면 기술력을 보여줄 수 있습니다");
        }
        
        // 기술 숙련도 표시 (3점) - NEW
        if (data.getSkills() != null && !data.getSkills().isEmpty()) {
            boolean hasLevel = data.getSkills().stream()
                .anyMatch(s -> s.getLevel() != null && s.getLevel() > 0);
            if (hasLevel) {
                score += 3;
            } else {
                details.add("기술 숙련도를 표시하면 역량을 더 잘 보여줄 수 있습니다");
            }
        }
        
        return new ScoreResult(score, 30, details);
    }
    
    /**
     * 트러블슈팅 점수 (25점 만점)
     * - 트러블슈팅 개수: 15점 (최대 3개 기준)
     * - 각 필드 충실도: 5점
     * - 기술 키워드 포함: 5점
     */
    public ScoreResult calculateTroubleshooting(List<Troubleshooting> troubleshootings) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        int count = troubleshootings != null ? troubleshootings.size() : 0;
        
        // 트러블슈팅 개수 (15점)
        if (count >= 3) {
            score += 15;
        } else if (count >= 2) {
            score += 10;
            details.add("트러블슈팅을 1개 더 추가하면 더 높은 점수를 받을 수 있습니다");
        } else if (count >= 1) {
            score += 5;
            details.add("트러블슈팅 사례를 더 추가하면 좋습니다 (3개 권장)");
        } else {
            details.add("트러블슈팅 사례를 추가해주세요 - 문제 해결 능력을 보여줄 수 있습니다");
            return new ScoreResult(score, 25, details);
        }
        
        // 각 필드 충실도 (5점) - problem, cause, solution, lesson 각 50자 이상
        int wellWrittenCount = 0;
        for (Troubleshooting t : troubleshootings) {
            int fieldScore = 0;
            if (getLength(t.getProblem()) >= 50) fieldScore++;
            if (getLength(t.getCause()) >= 50) fieldScore++;
            if (getLength(t.getSolution()) >= 50) fieldScore++;
            if (getLength(t.getLesson()) >= 30) fieldScore++;
            if (fieldScore >= 3) wellWrittenCount++;
        }
        
        if (wellWrittenCount >= count) {
            score += 5;
        } else if (wellWrittenCount > 0) {
            score += 3;
            details.add("트러블슈팅의 각 항목을 더 자세히 작성해보세요 (50자 이상 권장)");
        } else {
            score += 1;
            details.add("트러블슈팅 내용이 너무 짧습니다");
        }
        
        // 기술 키워드 포함 (5점) - NEW
        int techKeywordCount = 0;
        for (Troubleshooting t : troubleshootings) {
            String allText = (t.getProblem() + " " + t.getCause() + " " + t.getSolution()).toLowerCase();
            if (hasTechnicalKeywords(allText)) {
                techKeywordCount++;
            }
        }
        
        if (techKeywordCount >= count) {
            score += 5;
        } else if (techKeywordCount > 0) {
            score += 3;
            details.add("구체적인 에러명이나 기술명을 포함하면 더 좋습니다");
        } else {
            details.add("구체적인 에러명, 기술명을 포함해보세요 (예: NullPointerException, CORS 에러)");
        }
        
        return new ScoreResult(score, 25, details);
    }
    
    /**
     * 표현력 점수 (20점 만점) - 규칙 기반 기본 점수
     * AI 평가로 보완될 수 있음
     * - 자기소개 품질: 8점
     * - 프로젝트 설명 품질: 7점
     * - 역할 명시: 5점
     */
    public ScoreResult calculateExpression(PortfolioData data) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        // 자기소개 품질 (8점)
        String intro = data.getIntroduction();
        if (hasValue(intro)) {
            if (intro.length() >= 300 && containsSpecificWords(intro)) {
                score += 8;
            } else if (intro.length() >= 200) {
                score += 6;
            } else if (intro.length() >= 100) {
                score += 4;
            } else {
                score += 2;
                details.add("자기소개에 구체적인 경험이나 목표를 추가해보세요");
            }
        } else {
            details.add("자기소개를 작성해주세요");
        }
        
        // 프로젝트 설명 품질 (7점)
        if (data.getProjects() != null && !data.getProjects().isEmpty()) {
            long detailedCount = data.getProjects().stream()
                .filter(p -> getLength(p.getDescription()) >= 100)
                .count();
            
            if (detailedCount == data.getProjects().size()) {
                score += 7;
            } else if (detailedCount > 0) {
                score += 4;
                details.add("모든 프로젝트의 설명을 더 구체적으로 작성해주세요");
            } else {
                score += 1;
                details.add("프로젝트 설명이 너무 짧습니다");
            }
            
            // 역할 명시 (5점)
            long roleCount = data.getProjects().stream()
                .filter(p -> hasValue(p.getRole()))
                .count();
            
            if (roleCount == data.getProjects().size()) {
                score += 5;
            } else if (roleCount > 0) {
                score += 3;
                details.add("모든 프로젝트에서 담당한 역할을 명시해주세요");
            } else {
                details.add("프로젝트에서 담당한 역할을 명시해주세요");
            }
        }
        
        return new ScoreResult(score, 20, details);
    }
    
    /**
     * 활동성 점수 (25점 만점)
     * - GitHub 잔디 기여도: 20점 (100개+ → 20점, 50개+ → 15점, 30개+ → 10점, 10개+ → 5점)
     * - 프로젝트 링크 다양성: 5점
     */
    public ScoreResult calculateActivity(Boolean showContributionGraph, String snapshotJson, PortfolioData data) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        // GitHub 잔디 (20점)
        if (showContributionGraph != null && showContributionGraph) {
            if (hasValue(snapshotJson)) {
                try {
                    int contributions = parseContributions(snapshotJson);
                    if (contributions >= 100) {
                        score += 20;
                    } else if (contributions >= 50) {
                        score += 15;
                    } else if (contributions >= 30) {
                        score += 10;
                    } else if (contributions >= 10) {
                        score += 5;
                    } else if (contributions > 0) {
                        score += 2;
                        details.add("GitHub 활동을 더 늘려보세요 (30개 이상 권장)");
                    } else {
                        score += 1;
                        details.add("최근 GitHub 활동이 없습니다");
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse contribution snapshot", e);
                    score += 5;
                }
            } else {
                score += 5;
            }
        } else {
            details.add("GitHub 잔디를 표시하면 활동성을 보여줄 수 있습니다");
        }
        
        // 프로젝트 링크 다양성 (5점)
        if (data != null && data.getProjects() != null) {
            long linkCount = data.getProjects().stream()
                .filter(p -> hasValue(p.getGithubUrl()) || hasValue(p.getDemoUrl()) || hasValue(p.getLink()))
                .count();
            
            if (linkCount >= 2) {
                score += 5;
            } else if (linkCount >= 1) {
                score += 3;
            } else {
                details.add("프로젝트에 링크를 추가하면 활동성을 보여줄 수 있습니다");
            }
        }
        
        return new ScoreResult(score, 25, details);
    }
    
    /**
     * 활동성 점수 (기존 호환용 오버로드)
     */
    public ScoreResult calculateActivity(Boolean showContributionGraph, String snapshotJson) {
        return calculateActivity(showContributionGraph, snapshotJson, null);
    }
    
    // ==================== 유틸 메서드 ====================
    
    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }
    
    private int getLength(String s) {
        return s != null ? s.length() : 0;
    }
    
    private int getSize(List<?> list) {
        return list != null ? list.size() : 0;
    }
    
    /**
     * 기술 키워드 포함 여부 확인 (에러명, 기술명)
     */
    private boolean hasTechnicalKeywords(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        
        boolean hasError = ERROR_PATTERNS.stream().anyMatch(lower::contains);
        boolean hasTech = TECH_PATTERNS.stream().anyMatch(lower::contains);
        
        return hasError || hasTech;
    }
    
    /**
     * 기술 스택을 카테고리로 분류
     */
    private Set<String> categorizeTechStack(List<String> skills) {
        Set<String> categories = new HashSet<>();
        
        Map<String, List<String>> techMap = Map.of(
            "Frontend", List.of("react", "vue", "angular", "javascript", "typescript", "html", "css", "next", "tailwind", "sass", "jquery", "svelte"),
            "Backend", List.of("spring", "boot", "security", "validation", "java", "mybatis", "thymeleaf", "lombok", "querydsl", "JDBC", "c++", "c#", "node", "python", "django", "express", "flask", "nestjs", "go", "rust", "php", "ruby", "kotlin"),
            "Database", List.of("mysql", "oracle", "postgresql", "mongodb", "redis", "mariadb", "sqlite", "mssql", "dynamodb", "elasticsearch"),
            "DevOps", List.of("docker", "kubernetes", "aws", "jenkins", "git", "linux", "nginx", "gcp", "azure", "ci", "cd", "terraform", "github", "ansible")
        );
        
        if (skills == null) return categories;
        
        for (String skill : skills) {
            String lower = skill.toLowerCase();
            for (Map.Entry<String, List<String>> entry : techMap.entrySet()) {
                if (entry.getValue().stream().anyMatch(lower::contains)) {
                    categories.add(entry.getKey());
                }
            }
        }
        return categories;
    }
    
    /**
     * 프로젝트 설명 평균 길이 계산
     */
    private int calculateAvgProjectDescLength(List<PortfolioData.ProjectData> projects) {
        if (projects == null || projects.isEmpty()) return 0;
        
        int totalLength = projects.stream()
            .mapToInt(p -> getLength(p.getDescription()))
            .sum();
        
        return totalLength / projects.size();
    }
    
    /**
     * 자기소개에 구체적인 키워드가 있는지 확인
     */
    private boolean containsSpecificWords(String text) {
        List<String> specificWords = List.of(
            "경험", "개발", "프로젝트", "학습", "성장", "목표", "관심", "열정",
            "팀", "협업", "문제", "해결", "기술", "도전", "역량", "성과"
        );
        String lower = text.toLowerCase();
        return specificWords.stream().filter(lower::contains).count() >= 3;
    }
    
    /**
     * GitHub 잔디 스냅샷에서 총 기여도 파싱
     */
    private int parseContributions(String snapshotJson) throws Exception {
        JsonNode root = objectMapper.readTree(snapshotJson);
        JsonNode weeks = root.get("weeks");
        
        if (weeks == null || !weeks.isArray()) {
            return 0;
        }
        
        int total = 0;
        for (JsonNode week : weeks) {
            JsonNode days = week.get("contributionDays");
            if (days != null && days.isArray()) {
                for (JsonNode day : days) {
                    JsonNode count = day.get("contributionCount");
                    if (count != null) {
                        total += count.asInt();
                    }
                }
            }
        }
        return total;
    }
}

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

/**
 * 규칙 기반 포트폴리오 점수 계산기
 * 일관된 점수 산출을 위해 명확한 규칙으로 계산
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleBasedScorer {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 완성도 점수 (25점 만점)
     * - 이름: 3점
     * - 연락처: 2점
     * - 자기소개: 5점
     * - 스킬: 5점
     * - 프로젝트: 10점
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
        int projectCount = data.getProjects() != null ? data.getProjects().size() : 0;
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
        
        return new ScoreResult(score, 25, details);
    }
    
    /**
     * 기술력 점수 (25점 만점)
     * - 기술 스택 다양성: 10점
     * - 프로젝트 설명 상세도: 10점
     * - 프로젝트 기술 스택 명시: 5점
     */
    public ScoreResult calculateTechnical(PortfolioData data) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        List<String> skills = data.getSkillNames();
        
        // 기술 스택 다양성 (10점)
        Set<String> categories = categorizeTechStack(skills);
        if (categories.size() >= 4) {  // Frontend, Backend, DB, DevOps 등
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
        
        // 프로젝트 기술 설명 상세도 (10점)
        if (data.getProjects() != null && !data.getProjects().isEmpty()) {
            int avgDescLength = calculateAvgProjectDescLength(data.getProjects());
            if (avgDescLength >= 200) {
                score += 10;
            } else if (avgDescLength >= 100) {
                score += 6;
                details.add("프로젝트 설명을 더 상세히 작성해보세요");
            } else if (avgDescLength > 0) {
                score += 2;
                details.add("프로젝트 설명이 너무 짧습니다");
            }
            
            // 프로젝트에 기술 스택 명시 (5점)
            boolean hasProjectTech = data.getProjects().stream()
                .anyMatch(PortfolioData.ProjectData::hasTechStack);
            if (hasProjectTech) {
                score += 5;
            } else {
                details.add("프로젝트에 사용한 기술 스택을 명시해주세요");
            }
        } else {
            details.add("프로젝트를 추가하면 기술력을 보여줄 수 있습니다");
        }
        
        return new ScoreResult(score, 25, details);
    }
    
    /**
     * 트러블슈팅 점수 (25점 만점)
     * - 트러블슈팅 개수: 15점 (최대 3개 기준)
     * - 트러블슈팅 상세도: 10점
     */
    public ScoreResult calculateTroubleshooting(List<Troubleshooting> troubleshootings) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        int count = troubleshootings != null ? troubleshootings.size() : 0;
        
        // 트러블슈팅 개수 (15점) - 최대 3개 기준
        if (count >= 3) {
            score += 15;
        } else if (count >= 2) {
            score += 10;
            details.add("트러블슈팅을 1개 더 추가하면 만점을 받을 수 있습니다");
        } else if (count >= 1) {
            score += 5;
            details.add("트러블슈팅 사례를 더 추가하면 좋습니다 (3개 권장)");
        } else {
            details.add("트러블슈팅 사례를 추가해주세요 - 문제 해결 능력을 보여줄 수 있습니다");
            return new ScoreResult(score, 25, details);
        }
        
        // 트러블슈팅 상세도 (10점)
        int avgLength = troubleshootings.stream()
            .mapToInt(t -> getLength(t.getProblem()) + getLength(t.getSolution()) + getLength(t.getLesson()))
            .sum() / count;
        
        if (avgLength >= 300) {
            score += 10;
        } else if (avgLength >= 150) {
            score += 6;
        } else {
            score += 2;
            details.add("트러블슈팅 내용을 더 상세히 작성해보세요");
        }
        
        return new ScoreResult(score, 25, details);
    }
    
    /**
     * 표현력 점수 (15점 만점)
     * - 자기소개 품질: 7점
     * - 프로젝트 설명 품질: 8점
     */
    public ScoreResult calculateExpression(PortfolioData data) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        // 자기소개 품질 (7점) - 길이 + 구체성
        String intro = data.getIntroduction();
        if (hasValue(intro)) {
            if (intro.length() >= 300 && containsSpecificWords(intro)) {
                score += 7;
            } else if (intro.length() >= 150) {
                score += 4;
            } else {
                score += 2;
                details.add("자기소개에 구체적인 경험이나 목표를 추가해보세요");
            }
        } else {
            details.add("자기소개를 작성해주세요");
        }
        
        // 프로젝트 설명 품질 (8점)
        if (data.getProjects() != null && !data.getProjects().isEmpty()) {
            boolean hasRole = data.getProjects().stream()
                .anyMatch(p -> hasValue(p.getRole()));
            boolean hasDetailedDesc = data.getProjects().stream()
                .anyMatch(p -> getLength(p.getDescription()) >= 100);
            
            if (hasRole) {
                score += 4;
            } else {
                details.add("프로젝트에서 담당한 역할을 명시해주세요");
            }
            
            if (hasDetailedDesc) {
                score += 4;
            } else {
                details.add("프로젝트 설명을 더 구체적으로 작성해주세요");
            }
        }
        
        return new ScoreResult(score, 15, details);
    }
    
    /**
     * 활동성 점수 (10점 만점)
     * - GitHub 잔디 기여도 기반
     */
    public ScoreResult calculateActivity(Boolean showContributionGraph, String snapshotJson) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        if (showContributionGraph == null || !showContributionGraph) {
            details.add("GitHub 잔디를 표시하면 활동성을 보여줄 수 있습니다");
            return new ScoreResult(0, 10, details);
        }
        
        // 스냅샷에서 기여도 분석
        if (hasValue(snapshotJson)) {
            try {
                int contributions = parseContributions(snapshotJson);
                if (contributions >= 50) {
                    score = 10;
                } else if (contributions >= 30) {
                    score = 7;
                } else if (contributions >= 10) {
                    score = 4;
                } else if (contributions > 0) {
                    score = 2;
                    details.add("GitHub 활동을 더 늘려보세요");
                } else {
                    score = 1;
                    details.add("최근 GitHub 활동이 없습니다");
                }
            } catch (Exception e) {
                log.warn("Failed to parse contribution snapshot", e);
                score = 5;  // 파싱 실패 시 기본 점수
            }
        } else {
            score = 5;  // 잔디 표시만 했으면 기본 점수
        }
        
        return new ScoreResult(score, 10, details);
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
     * 기술 스택을 카테고리로 분류
     */
    private Set<String> categorizeTechStack(List<String> skills) {
        Set<String> categories = new HashSet<>();
        
        Map<String, List<String>> techMap = Map.of(
            "Frontend", List.of("react", "vue", "angular", "javascript", "typescript", "html", "css", "next", "tailwind", "sass", "jquery"),
            "Backend", List.of("spring", "java", "node", "python", "django", "express", "flask", "nestjs", "go", "rust", "php", "ruby"),
            "Database", List.of("mysql", "oracle", "postgresql", "mongodb", "redis", "mariadb", "sqlite", "mssql", "dynamodb"),
            "DevOps", List.of("docker", "kubernetes", "aws", "jenkins", "git", "linux", "nginx", "gcp", "azure", "ci", "cd", "terraform")
        );
        
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
            "팀", "협업", "문제", "해결", "기술", "도전"
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

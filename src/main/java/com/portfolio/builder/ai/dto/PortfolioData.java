package com.portfolio.builder.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioData {
    private String name;
    private String email;
    private String phone;
    private String introduction;
    private List<SkillData> skills = new ArrayList<>();
    private List<ProjectData> projects = new ArrayList<>();
    private List<EducationData> educations = new ArrayList<>();
    private List<ExperienceData> experiences = new ArrayList<>();
    private List<CertificateData> certificates = new ArrayList<>();
    private String avatarUrl;
    private String colorTheme;
    
    /**
     * 스킬 이름만 추출
     */
    public List<String> getSkillNames() {
        if (skills == null) return List.of();
        return skills.stream()
                .map(SkillData::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillData {
        private String name;
        private Integer level;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectData {
        private String name;
        private String description;
        private String role;
        private String period;
        private String tech;           // 기술 스택 (문자열)
        private List<String> techStack; // 기술 스택 (배열) - 호환용
        private String link;
        private String githubUrl;
        private String demoUrl;
        private String imageUrl;
        private String thumbnail;
        private String features;
        private String featureDetail;
        private String reflection;
        
        /**
         * 기술 스택 존재 여부 확인 (tech 또는 techStack)
         */
        public boolean hasTechStack() {
            if (tech != null && !tech.isBlank()) return true;
            if (techStack != null && !techStack.isEmpty()) return true;
            return false;
        }
        
        /**
         * 기술 스택 문자열 반환
         */
        public String getTechString() {
            if (tech != null && !tech.isBlank()) return tech;
            if (techStack != null && !techStack.isEmpty()) return String.join(", ", techStack);
            return "";
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationData {
        private String school;
        private String major;
        private String degree;
        private String period;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceData {
        private String company;
        private String position;
        private String period;
        private String description;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateData {
        private String name;
        private String issuer;
        private String date;
    }
}

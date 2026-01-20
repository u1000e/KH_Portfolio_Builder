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
    private List<String> skills = new ArrayList<>();
    private List<ProjectData> projects = new ArrayList<>();
    private List<EducationData> educations = new ArrayList<>();
    private List<ExperienceData> experiences = new ArrayList<>();
    private List<CertificateData> certificates = new ArrayList<>();
    private String avatarUrl;
    private String colorTheme;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectData {
        private String name;
        private String description;
        private String role;
        private String period;
        private List<String> techStack;
        private String githubUrl;
        private String demoUrl;
        private String imageUrl;
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

package com.portfolio.builder.github.presentation;

import com.portfolio.builder.github.application.GithubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Slf4j
public class GithubController {

    private final GithubService githubService;

    @GetMapping("/repos")
    public ResponseEntity<List<Map<String, Object>>> getRepositories(
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(githubService.getRepositories(memberId));
    }

    @GetMapping("/repos/{repoName}/languages")
    public ResponseEntity<Map<String, Integer>> getLanguages(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("repoName") String repoName) {
        return ResponseEntity.ok(githubService.getLanguages(memberId, repoName));
    }

    @GetMapping("/repos/{repoName}/readme")
    public ResponseEntity<Map<String, String>> getReadme(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("repoName") String repoName) {
        String readme = githubService.getReadme(memberId, repoName);
        return ResponseEntity.ok(Map.of("content", readme != null ? readme : ""));
    }

    @GetMapping("/repos/{repoName}")
    public ResponseEntity<Map<String, Object>> getRepositoryDetails(
            @RequestAttribute(name = "memberId") Long memberId,
            @PathVariable("repoName") String repoName) {
        return ResponseEntity.ok(githubService.getRepositoryDetails(memberId, repoName));
    }

    @GetMapping("/contributions")
    public ResponseEntity<Map<String, Object>> getContributions(
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(githubService.getContributions(memberId));
    }
}

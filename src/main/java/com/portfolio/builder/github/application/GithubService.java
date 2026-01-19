package com.portfolio.builder.github.application;

import com.portfolio.builder.member.application.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubService {

    private final AuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();

    public List<Map<String, Object>> getRepositories(Long memberId) {
        String accessToken = authService.getGithubAccessToken(memberId);
        String username = authService.getGithubUsername(memberId);
        
        String url = "https://api.github.com/users/" + username + "/repos?sort=updated&per_page=100";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> repos = response.getBody();
            if (repos == null) {
                return Collections.emptyList();
            }

            return repos.stream()
                    .map(repo -> {
                        Map<String, Object> simplified = new HashMap<>();
                        simplified.put("id", repo.get("id"));
                        simplified.put("name", repo.get("name"));
                        simplified.put("full_name", repo.get("full_name"));
                        simplified.put("description", repo.get("description"));
                        simplified.put("html_url", repo.get("html_url"));
                        simplified.put("homepage", repo.get("homepage"));
                        simplified.put("language", repo.get("language"));
                        simplified.put("stargazers_count", repo.get("stargazers_count"));
                        simplified.put("forks_count", repo.get("forks_count"));
                        simplified.put("updated_at", repo.get("updated_at"));
                        simplified.put("topics", repo.get("topics"));
                        simplified.put("fork", repo.get("fork"));
                        simplified.put("private", repo.get("private"));
                        return simplified;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to fetch GitHub repositories: ", e);
            throw new RuntimeException("Failed to fetch GitHub repositories");
        }
    }

    public Map<String, Integer> getLanguages(Long memberId, String repoName) {
        String accessToken = authService.getGithubAccessToken(memberId);
        String username = authService.getGithubUsername(memberId);
        
        String url = "https://api.github.com/repos/" + username + "/" + repoName + "/languages";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Integer>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<Map<String, Integer>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch repository languages: ", e);
            return Collections.emptyMap();
        }
    }

    public String getReadme(Long memberId, String repoName) {
        String accessToken = authService.getGithubAccessToken(memberId);
        String username = authService.getGithubUsername(memberId);
        
        String url = "https://api.github.com/repos/" + username + "/" + repoName + "/readme";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github.raw")));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.warn("README not found for repo {}: {}", repoName, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getRepositoryDetails(Long memberId, String repoName) {
        String accessToken = authService.getGithubAccessToken(memberId);
        String username = authService.getGithubUsername(memberId);
        
        String url = "https://api.github.com/repos/" + username + "/" + repoName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> repo = response.getBody();
            if (repo == null) {
                return Collections.emptyMap();
            }

            // 언어 정보 추가
            Map<String, Integer> languages = getLanguages(memberId, repoName);
            repo.put("languages", languages);

            return repo;
        } catch (Exception e) {
            log.error("Failed to fetch repository details: ", e);
            throw new RuntimeException("Failed to fetch repository details");
        }
    }

    /**
     * GitHub GraphQL API를 사용하여 잔디(Contribution) 데이터 가져오기
     */
    public Map<String, Object> getContributions(Long memberId) {
        String accessToken = authService.getGithubAccessToken(memberId);
        String username = authService.getGithubUsername(memberId);

        String graphqlUrl = "https://api.github.com/graphql";

        // GraphQL 쿼리
        String query = """
            query($username: String!) {
                user(login: $username) {
                    contributionsCollection {
                        totalContributions
                        contributionCalendar {
                            totalContributions
                            weeks {
                                contributionDays {
                                    date
                                    contributionCount
                                    contributionLevel
                                }
                            }
                        }
                    }
                }
            }
            """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("variables", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    graphqlUrl,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || body.containsKey("errors")) {
                log.error("GraphQL error: {}", body);
                return Collections.emptyMap();
            }

            // 데이터 추출
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            Map<String, Object> user = (Map<String, Object>) data.get("user");
            Map<String, Object> contributionsCollection = (Map<String, Object>) user.get("contributionsCollection");
            Map<String, Object> calendar = (Map<String, Object>) contributionsCollection.get("contributionCalendar");

            return Map.of(
                    "totalContributions", calendar.get("totalContributions"),
                    "weeks", calendar.get("weeks")
            );

        } catch (Exception e) {
            log.error("Failed to fetch GitHub contributions: ", e);
            return Collections.emptyMap();
        }
    }
}

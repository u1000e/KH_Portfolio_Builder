package com.portfolio.builder.member.presentation;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.builder.member.application.AuthService;
import com.portfolio.builder.member.dto.MemberResponse;
import com.portfolio.builder.member.dto.ProfileUpdateRequest;
import com.portfolio.builder.member.dto.TokenResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @GetMapping("/github/login")
    public ResponseEntity<Map<String, String>> getGithubAuthUrl() {
        String authUrl = authService.getGithubAuthUrl();
        return ResponseEntity.ok(Map.of("url", authUrl));
    }

    @GetMapping("/github/callback") 
    public ResponseEntity<?> githubCallback(@RequestParam(name = "code") String code) {
        log.info("GitHub callback received with code: {}", code.substring(0, 5) + "...");

        try {
            log.info("Calling authService.processGithubCallback...");
            TokenResponse tokenResponse = authService.processGithubCallback(code);
            log.info("TokenResponse received, returning response...");
            log.info("accessToken: {}", tokenResponse.getAccessToken() != null ? "exists" : "null");
            log.info("member: {}", tokenResponse.getMember() != null ? tokenResponse.getMember().getGithubUsername() : "null");
            ResponseEntity<?> response = ResponseEntity.ok(tokenResponse);
            log.info("ResponseEntity created, returning now");
            return response;
        } catch (Exception e) {
            log.error("GitHub OAuth error: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getCurrentMember(
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(authService.getCurrentMember(memberId));
    }

    @PutMapping("/profile") 
    public ResponseEntity<MemberResponse> updateProfile(
            @RequestAttribute(name = "memberId") Long memberId,
            @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(authService.updateProfile(memberId, request));
    }
} 

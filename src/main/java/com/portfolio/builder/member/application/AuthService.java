package com.portfolio.builder.member.application;

import com.portfolio.builder.global.security.JwtTokenProvider;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.member.dto.MemberResponse;
import com.portfolio.builder.member.dto.ProfileUpdateRequest;
import com.portfolio.builder.member.dto.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    
    public AuthService(MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        
        // 타임아웃 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10초
        factory.setReadTimeout(10000);    // 10초
        this.restTemplate = new RestTemplate(factory);
    }

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    public String getGithubAuthUrl() {
        return "https://github.com/login/oauth/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=user:email,read:user,repo";
    }

    public TokenResponse processGithubCallback(String code) {
        log.info("processGithubCallback started");
        
        // 1. GitHub에서 Access Token 받기
        String accessToken = getGithubAccessToken(code);
        log.info("Step 1 complete: got access token");
        
        // 2. GitHub 사용자 정보 가져오기
        Map<String, Object> userInfo = getGithubUserInfo(accessToken);
        log.info("Step 2 complete: got user info");
        
        // 3. 이메일 정보 가져오기 (별도 API 호출 필요)
        String email = getGithubUserEmail(accessToken);
        log.info("Step 3 complete: got email - {}", email);
        
        // 4. Member 찾거나 생성
        Member member = findOrCreateMember(userInfo, accessToken, email);
        log.info("Step 4 complete: member id={}", member.getId());
        
        // 5. JWT 토큰 생성
        log.info("Step 5: creating JWT token...");
        String jwtToken = jwtTokenProvider.createToken(member.getId(), member.getGithubId());
        log.info("Step 5 complete: JWT token created");
        
        log.info("Creating response...");
        TokenResponse response = TokenResponse.of(jwtToken, MemberResponse.from(member));
        log.info("processGithubCallback completed successfully");
        return response;
    }

    private String getGithubAccessToken(String code) {
        log.info("Getting GitHub access token for code: {}...", code.substring(0, 5));
        String tokenUrl = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        log.info("Sending request to GitHub...");
        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                Map.class
        );
        log.info("GitHub response received: {}", response.getStatusCode());

        Map<String, Object> body = response.getBody();
        if (body == null || body.containsKey("error")) {
            log.error("GitHub token error: {}", body);
            String errorMsg = body != null ? 
                "error=" + body.get("error") + ", description=" + body.get("error_description") :
                "Empty response";
            throw new RuntimeException("Failed to get GitHub access token: " + errorMsg);
        }

        log.info("GitHub access token obtained successfully");
        return (String) body.get("access_token");
    }

    private Map<String, Object> getGithubUserInfo(String accessToken) {
        String userUrl = "https://api.github.com/user";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userUrl,
                HttpMethod.GET,
                request,
                Map.class
        );

        return response.getBody();
    }

    private String getGithubUserEmail(String accessToken) {
        String emailUrl = "https://api.github.com/user/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    emailUrl,
                    HttpMethod.GET,
                    request,
                    List.class
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null && !emails.isEmpty()) {
                // primary 이메일 찾기
                for (Map<String, Object> emailInfo : emails) {
                    Boolean primary = (Boolean) emailInfo.get("primary");
                    Boolean verified = (Boolean) emailInfo.get("verified");
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailInfo.get("email");
                    }
                }
                // primary가 없으면 첫 번째 이메일 사용
                return (String) emails.get(0).get("email");
            }
        } catch (Exception e) {
            log.warn("Failed to get user email: {}", e.getMessage());
        }
        return null;
    }

    private Member findOrCreateMember(Map<String, Object> userInfo, String accessToken, String email) {
        String githubId = String.valueOf(userInfo.get("id"));

        return memberRepository.findByGithubId(githubId)
                .map(existingMember -> {
                    // 기존 회원 정보 업데이트
                    existingMember.setAccessToken(accessToken);
                    existingMember.setAvatarUrl((String) userInfo.get("avatar_url"));
                    existingMember.setName((String) userInfo.get("name"));
                    if (email != null) {
                        existingMember.setEmail(email);
                    }
                    return memberRepository.save(existingMember);
                })
                .orElseGet(() -> {
                    // 신규 회원 생성
                    Member newMember = Member.builder()
                            .githubId(githubId)
                            .githubUsername((String) userInfo.get("login"))
                            .email(email)
                            .name((String) userInfo.get("name"))
                            .avatarUrl((String) userInfo.get("avatar_url"))
                            .accessToken(accessToken)
                            .role(Member.Role.USER)
                            .status(Member.Status.ACTIVE)  // 바로 활성 상태
                            .build();
                    return memberRepository.save(newMember);
                });
    }

    @Transactional(readOnly = true)
    public MemberResponse getCurrentMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        return MemberResponse.from(member);
    }

    public MemberResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        // 유효성 검사
        if (!isValidPosition(request.getPosition())) {
            throw new IllegalArgumentException("유효하지 않은 직급입니다. (운영팀, 강사, 수강생 중 선택)");
        }
        if (!isValidBranch(request.getBranch())) {
            throw new IllegalArgumentException("유효하지 않은 소속입니다. (종로, 강남 중 선택)");
        }
        
        // 강사/운영팀 신청은 관리자 승인 필요
        if ("강사".equals(request.getPosition()) || "운영팀".equals(request.getPosition())) {
            // 이미 해당 직급이면 그대로 유지
            if (!request.getPosition().equals(member.getPosition())) {
                member.setPendingPosition(request.getPosition());
                log.info("Member {} requested position change to {} (pending approval)", memberId, request.getPosition());
            }
            // 기존 position이 없으면 수강생으로 설정 (최초 설정 시)
            if (member.getPosition() == null) {
                member.setPosition("수강생");
            }
        } else {
            // 수강생은 바로 적용
            member.setPosition(request.getPosition());
            member.setPendingPosition(null);  // 대기 중인 신청 취소
        }
        
        member.setBranch(request.getBranch());
        
        // 수강생인 경우만 강의실 설정
        if ("수강생".equals(member.getPosition())) {
            member.setClassroom(request.getClassroom());
        } else {
            member.setClassroom(null);
        }
        
        // 프로필 설정 완료 시 상태 변경
        if (member.getStatus() == Member.Status.PENDING) {
            member.setStatus(Member.Status.ACTIVE);
        }
        
        Member updatedMember = memberRepository.save(member);
        return MemberResponse.from(updatedMember);
    }
    
    private boolean isValidPosition(String position) {
        return position != null && 
               (position.equals("운영팀") || position.equals("강사") || position.equals("수강생"));
    }
    
    private boolean isValidBranch(String branch) {
        return branch != null && 
               (branch.equals("종로") || branch.equals("강남"));
    }
    
    // GitHub Access Token 가져오기 (다른 서비스에서 사용)
    @Transactional(readOnly = true)
    public String getGithubAccessToken(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        return member.getAccessToken();
    }

    @Transactional(readOnly = true)
    public String getGithubUsername(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        return member.getGithubUsername();
    }
}

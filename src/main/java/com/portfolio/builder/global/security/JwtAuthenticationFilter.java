package com.portfolio.builder.global.security;

import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // 토큰이 있는 경우
        if (StringUtils.hasText(token)) {
            // 토큰이 유효하지 않으면 401 응답
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Token validation failed for request: {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Token expired or invalid\",\"code\":\"TOKEN_EXPIRED\"}");
                return; // 필터 체인 중단
            }
            
            // 토큰이 유효하면 인증 설정
            Long memberId = jwtTokenProvider.getMemberId(token);
            Optional<Member> memberOpt = memberRepository.findById(memberId);
            
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                request.setAttribute("memberId", memberId);
                request.setAttribute("member", member);
                
                // Spring Security 인증 설정
                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + member.getRole().name())
                );
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(memberId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 토큰은 유효하지만 회원이 없는 경우 (탈퇴 등)
                log.warn("Member not found for token memberId: {}", memberId);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Member not found\",\"code\":\"MEMBER_NOT_FOUND\"}");
                return;
            }
        }
        // 토큰이 없는 경우 → 그대로 진행 (공개 API용)

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

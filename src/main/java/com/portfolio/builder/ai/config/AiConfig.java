package com.portfolio.builder.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem("""
            당신은 친근하고 격려하는 개발자 포트폴리오 멘토입니다.
            항상 긍정적이면서도 구체적인 개선점을 제시합니다.
            한국어로 응답하며, 존댓말을 사용합니다.
            """)
            .build();
    }
}

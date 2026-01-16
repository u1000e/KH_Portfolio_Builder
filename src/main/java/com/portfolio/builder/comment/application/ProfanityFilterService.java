package com.portfolio.builder.comment.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProfanityFilterService {
 
    // 금지어 목록 (한국어)
    private static final Set<String> KOREAN_PROFANITIES = new HashSet<>();
    
    // 금지어 목록 (영어)
    private static final Set<String> ENGLISH_PROFANITIES = new HashSet<>();
    
    static {
        // 한국어 비속어 목록
        KOREAN_PROFANITIES.add("시발");
        KOREAN_PROFANITIES.add("씨발");
        KOREAN_PROFANITIES.add("ㅅㅂ");
        KOREAN_PROFANITIES.add("ㅆㅂ");
        KOREAN_PROFANITIES.add("병신");
        KOREAN_PROFANITIES.add("ㅂㅅ");
        KOREAN_PROFANITIES.add("지랄");
        KOREAN_PROFANITIES.add("ㅈㄹ");
        KOREAN_PROFANITIES.add("개새끼");
        KOREAN_PROFANITIES.add("새끼");
        KOREAN_PROFANITIES.add("ㅅㄲ");
        KOREAN_PROFANITIES.add("미친");
        KOREAN_PROFANITIES.add("개같은");
        KOREAN_PROFANITIES.add("썅");
        KOREAN_PROFANITIES.add("닥쳐");
        KOREAN_PROFANITIES.add("꺼져");
        KOREAN_PROFANITIES.add("죽어");
        KOREAN_PROFANITIES.add("엿먹어");
        KOREAN_PROFANITIES.add("니미");
        KOREAN_PROFANITIES.add("느금마");
        KOREAN_PROFANITIES.add("애미");
        KOREAN_PROFANITIES.add("애비");
        
        // 영어 비속어 목록
        ENGLISH_PROFANITIES.add("fuck");
        ENGLISH_PROFANITIES.add("shit");
        ENGLISH_PROFANITIES.add("damn");
        ENGLISH_PROFANITIES.add("bitch");
        ENGLISH_PROFANITIES.add("asshole");
        ENGLISH_PROFANITIES.add("bastard");
        ENGLISH_PROFANITIES.add("crap");
        ENGLISH_PROFANITIES.add("dick");
        ENGLISH_PROFANITIES.add("pussy");
        ENGLISH_PROFANITIES.add("cock");
    }

    /**
     * 욕설이 포함되어 있는지 검사
     * @param content 검사할 내용
     * @return true면 욕설 포함
     */
    public boolean containsProfanity(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        String noSpaceContent = lowerContent.replaceAll("\\s+", "");
        
        // 한국어 비속어 검사
        for (String profanity : KOREAN_PROFANITIES) {
            if (noSpaceContent.contains(profanity)) {
                log.warn("Profanity detected: {}", profanity);
                return true;
            }
        }
        
        // 영어 비속어 검사 (단어 경계 고려)
        for (String profanity : ENGLISH_PROFANITIES) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(profanity) + "\\b", 
                                              Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(lowerContent).find()) {
                log.warn("Profanity detected: {}", profanity);
                return true;
            }
        }
        
        return false;
    }

    /**
     * 욕설을 마스킹 처리
     * @param content 원본 내용
     * @return 마스킹된 내용
     */
    public String maskProfanity(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        String masked = content;
        
        // 한국어 비속어 마스킹
        for (String profanity : KOREAN_PROFANITIES) {
            String replacement = "*".repeat(profanity.length());
            masked = masked.replace(profanity, replacement);
        }
        
        // 영어 비속어 마스킹
        for (String profanity : ENGLISH_PROFANITIES) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(profanity) + "\\b", 
                                              Pattern.CASE_INSENSITIVE);
            String replacement = "*".repeat(profanity.length());
            masked = pattern.matcher(masked).replaceAll(replacement);
        }
        
        return masked;
    }
}

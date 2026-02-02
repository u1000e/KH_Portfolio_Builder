package com.portfolio.builder.quiz.service;

import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.portfolio.domain.PortfolioLikeRepository;
import com.portfolio.builder.quiz.domain.QuizStreak;
import com.portfolio.builder.quiz.dto.QuizDto.*;
import com.portfolio.builder.quiz.repository.QuizAttemptRepository;
import com.portfolio.builder.quiz.repository.QuizStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BorderService {

    private final MemberRepository memberRepository;
    private final QuizStreakRepository quizStreakRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final PortfolioLikeRepository portfolioLikeRepository;
    private final CommentRepository commentRepository;

    // 테두리 정의 (10레벨 단위)
    private static final Map<String, BorderDefinition> BORDER_DEFINITIONS = new LinkedHashMap<>();
    // 배경색 정의
    private static final Map<String, BackgroundDefinition> BACKGROUND_DEFINITIONS = new LinkedHashMap<>();
    // 칭호 정의
    private static final Map<String, TitleDefinition> TITLE_DEFINITIONS = new LinkedHashMap<>();
    // 헤더 색상 정의 (10레벨 단위)
    private static final Map<String, HeaderDefinition> HEADER_DEFINITIONS = new LinkedHashMap<>();

    static {
        // borderId, name, requiredLevel, gradientFrom, gradientTo
        BORDER_DEFINITIONS.put("border_0", new BorderDefinition("기본", 0, "gray-400", "gray-500"));
        BORDER_DEFINITIONS.put("border_10", new BorderDefinition("브론즈", 10, "amber-600", "orange-700"));
        BORDER_DEFINITIONS.put("border_20", new BorderDefinition("실버", 20, "gray-300", "slate-400"));
        BORDER_DEFINITIONS.put("border_30", new BorderDefinition("골드", 30, "yellow-400", "amber-500"));
        BORDER_DEFINITIONS.put("border_40", new BorderDefinition("플래티넘", 40, "cyan-300", "blue-400"));
        BORDER_DEFINITIONS.put("border_50", new BorderDefinition("다이아몬드", 50, "blue-400", "indigo-500"));
        BORDER_DEFINITIONS.put("border_60", new BorderDefinition("에메랄드", 60, "emerald-400", "green-500"));
        BORDER_DEFINITIONS.put("border_70", new BorderDefinition("루비", 70, "red-500", "rose-600"));
        BORDER_DEFINITIONS.put("border_80", new BorderDefinition("사파이어", 80, "blue-500", "violet-600"));
        BORDER_DEFINITIONS.put("border_90", new BorderDefinition("오팔", 90, "pink-400", "purple-500"));
        BORDER_DEFINITIONS.put("border_100", new BorderDefinition("레전더리", 100, "amber-400", "pink-500"));

        // 배경색 정의 (backgroundId, name, colorClass, colorHex) - 전체 해금
        BACKGROUND_DEFINITIONS.put("bg_white", new BackgroundDefinition("화이트", "bg-white", "#ffffff"));
        BACKGROUND_DEFINITIONS.put("bg_gray", new BackgroundDefinition("그레이", "bg-gray-100", "#f3f4f6"));
        BACKGROUND_DEFINITIONS.put("bg_slate", new BackgroundDefinition("슬레이트", "bg-slate-100", "#f1f5f9"));
        BACKGROUND_DEFINITIONS.put("bg_zinc", new BackgroundDefinition("징크", "bg-zinc-100", "#f4f4f5"));
        BACKGROUND_DEFINITIONS.put("bg_stone", new BackgroundDefinition("스톤", "bg-stone-100", "#f5f5f4"));
        BACKGROUND_DEFINITIONS.put("bg_blue", new BackgroundDefinition("스카이 블루", "bg-blue-50", "#eff6ff"));
        BACKGROUND_DEFINITIONS.put("bg_blue_light", new BackgroundDefinition("라이트 블루", "bg-sky-50", "#f0f9ff"));
        BACKGROUND_DEFINITIONS.put("bg_indigo", new BackgroundDefinition("인디고", "bg-indigo-50", "#eef2ff"));
        BACKGROUND_DEFINITIONS.put("bg_violet", new BackgroundDefinition("바이올렛", "bg-violet-50", "#f5f3ff"));
        BACKGROUND_DEFINITIONS.put("bg_purple", new BackgroundDefinition("퍼플", "bg-purple-50", "#faf5ff"));
        BACKGROUND_DEFINITIONS.put("bg_fuchsia", new BackgroundDefinition("푸시아", "bg-fuchsia-50", "#fdf4ff"));
        BACKGROUND_DEFINITIONS.put("bg_pink", new BackgroundDefinition("핑크", "bg-pink-50", "#fdf2f8"));
        BACKGROUND_DEFINITIONS.put("bg_rose", new BackgroundDefinition("로즈", "bg-rose-50", "#fff1f2"));
        BACKGROUND_DEFINITIONS.put("bg_red", new BackgroundDefinition("레드", "bg-red-50", "#fef2f2"));
        BACKGROUND_DEFINITIONS.put("bg_orange", new BackgroundDefinition("오렌지", "bg-orange-50", "#fff7ed"));
        BACKGROUND_DEFINITIONS.put("bg_amber", new BackgroundDefinition("앰버", "bg-amber-50", "#fffbeb"));
        BACKGROUND_DEFINITIONS.put("bg_yellow", new BackgroundDefinition("옐로우", "bg-yellow-50", "#fefce8"));
        BACKGROUND_DEFINITIONS.put("bg_lime", new BackgroundDefinition("라임", "bg-lime-50", "#f7fee7"));
        BACKGROUND_DEFINITIONS.put("bg_green", new BackgroundDefinition("그린", "bg-green-50", "#f0fdf4"));
        BACKGROUND_DEFINITIONS.put("bg_emerald", new BackgroundDefinition("에메랄드", "bg-emerald-50", "#ecfdf5"));
        BACKGROUND_DEFINITIONS.put("bg_teal", new BackgroundDefinition("틸", "bg-teal-50", "#f0fdfa"));
        BACKGROUND_DEFINITIONS.put("bg_cyan", new BackgroundDefinition("시안", "bg-cyan-50", "#ecfeff"));

        // 헤더 색상 정의 (headerId, name, requiredLevel, colorClass, colorHex, gradientFrom, gradientTo)
        HEADER_DEFINITIONS.put("header_0", new HeaderDefinition("기본", 0, "bg-white", "#ffffff", null, null));
        HEADER_DEFINITIONS.put("header_10", new HeaderDefinition("스카이 블루", 10, "bg-sky-100", "#e0f2fe", null, null));
        HEADER_DEFINITIONS.put("header_20", new HeaderDefinition("민트", 20, "bg-emerald-100", "#d1fae5", null, null));
        HEADER_DEFINITIONS.put("header_30", new HeaderDefinition("라벤더", 30, "bg-violet-100", "#ede9fe", null, null));
        HEADER_DEFINITIONS.put("header_40", new HeaderDefinition("피치", 40, "bg-orange-100", "#ffedd5", null, null));
        HEADER_DEFINITIONS.put("header_50", new HeaderDefinition("로즈", 50, "bg-rose-100", "#ffe4e6", null, null));
        HEADER_DEFINITIONS.put("header_60", new HeaderDefinition("오션 그라데이션", 60, null, null, "from-cyan-200", "to-blue-300"));
        HEADER_DEFINITIONS.put("header_70", new HeaderDefinition("크림슨 그라데이션", 70, null, null, "from-rose-300", "to-red-400"));
        HEADER_DEFINITIONS.put("header_80", new HeaderDefinition("오로라 그라데이션", 80, null, null, "from-purple-200", "to-pink-300"));
        HEADER_DEFINITIONS.put("header_90", new HeaderDefinition("골드 그라데이션", 90, null, null, "from-amber-200", "to-yellow-300"));
        HEADER_DEFINITIONS.put("header_100", new HeaderDefinition("레전더리 그라데이션", 100, null, null, "from-fuchsia-400", "to-amber-400"));

        // 레벨 기반 칭호 (titleId, name, emoji, colorClass, colorHex, requiredLevel, condition)
        TITLE_DEFINITIONS.put("title_0", new TitleDefinition("코딩 새싹", "🌱", "text-green-500", "#22c55e", 0, "레벨 0 달성", false));
        TITLE_DEFINITIONS.put("title_10", new TitleDefinition("버그 사냥꾼", "🐛", "text-amber-600", "#d97706", 10, "레벨 10 달성", false));
        TITLE_DEFINITIONS.put("title_20", new TitleDefinition("주니어 개발자", "💻", "text-blue-500", "#3b82f6", 20, "레벨 20 달성", false));
        TITLE_DEFINITIONS.put("title_30", new TitleDefinition("커밋 장인", "⚡", "text-yellow-500", "#eab308", 30, "레벨 30 달성", false));
        TITLE_DEFINITIONS.put("title_40", new TitleDefinition("코드가 좋아요", "♨", "text-orange-500", "#f97316", 40, "레벨 40 달성", false));
        TITLE_DEFINITIONS.put("title_50", new TitleDefinition("퀴즈 정복", "🧠", "text-purple-500", "#a855f7", 50, "레벨 50 달성", false));
        TITLE_DEFINITIONS.put("title_60", new TitleDefinition("도내남바완", "🏆", "text-amber-500", "#f59e0b", 60, "레벨 60 달성", false));
        TITLE_DEFINITIONS.put("title_70", new TitleDefinition("풀스택 히어로", "🦸", "text-indigo-500", "#6366f1", 70, "레벨 70 달성", false));
        TITLE_DEFINITIONS.put("title_80", new TitleDefinition("전국재패", "🏴‍☠️", "text-red-500", "#ef4444", 80, "레벨 80 달성", false));
        TITLE_DEFINITIONS.put("title_90", new TitleDefinition("레잔도", "⭐", "text-pink-500", "#ec4899", 90, "레벨 90 달성", false));
        TITLE_DEFINITIONS.put("title_100", new TitleDefinition("개발왕", "🏅", "text-amber-400", "#fbbf24", 100, "레벨 100 달성 (MAX)", false));

        // 특별 칭호 (레벨 조건 없음, 특정 업적 달성 시)
        TITLE_DEFINITIONS.put("title_earlybird", new TitleDefinition("얼리버드", "🌅", "text-orange-400", "#fb923c", 0, "아침 6-9시 50문제 풀기", true));
        TITLE_DEFINITIONS.put("title_nightowl", new TitleDefinition("올빼미", "🦉", "text-indigo-400", "#818cf8", 0, "밤 22-02시 50문제 풀기", true));
        TITLE_DEFINITIONS.put("title_reviewer", new TitleDefinition("복습왕", "📚", "text-emerald-500", "#10b981", 0, "복습 모드 200회 달성", true));
        TITLE_DEFINITIONS.put("title_streaker", new TitleDefinition("스트릭 마스터", "🔥", "text-red-400", "#f87171", 0, "30일 연속 학습 달성", true));

        // 추가 특별 칭호
        TITLE_DEFINITIONS.put("title_effort", new TitleDefinition("노력파", "💪", "text-blue-500", "#3b82f6", 0, "오답 100회 이상", true));
        TITLE_DEFINITIONS.put("title_nevergiveup", new TitleDefinition("포기란 없다", "🥊", "text-rose-500", "#f43f5e", 0, "오답 200회 이상", true));
        TITLE_DEFINITIONS.put("title_500warrior", new TitleDefinition("오백전사", "⚔️", "text-violet-500", "#8b5cf6", 0, "총 500문제 풀기", true));
        TITLE_DEFINITIONS.put("title_lunchtime", new TitleDefinition("밥 좀 드세요", "🍱", "text-amber-500", "#f59e0b", 0, "점심시간(13-14시) 30문제 풀기", true));
        TITLE_DEFINITIONS.put("title_7days", new TitleDefinition("7일 챌린저", "🗓️", "text-cyan-500", "#06b6d4", 0, "7일 연속 학습 달성", true));
    }

    /**
     * 사용자의 테두리, 배경색, 칭호, 헤더 목록 조회 (해금 여부 포함)
     */
    public BorderListResponse getBorders(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        int currentLevel = calculateLevel(memberId);
        String selectedBorderId = member.getSelectedBorderId();
        String selectedBackgroundId = member.getSelectedBackgroundId();
        String selectedTitleId = member.getSelectedTitleId();
        String selectedHeaderId = member.getSelectedHeaderId();

        // 특별 칭호 해금 조건 체크
        Map<String, Boolean> specialTitleUnlocked = checkSpecialTitlesUnlocked(memberId);

        // 테두리 목록
        List<BorderResponse> borders = new ArrayList<>();
        for (Map.Entry<String, BorderDefinition> entry : BORDER_DEFINITIONS.entrySet()) {
            String borderId = entry.getKey();
            BorderDefinition def = entry.getValue();
            boolean unlocked = currentLevel >= def.requiredLevel;
            boolean selected = borderId.equals(selectedBorderId);

            borders.add(BorderResponse.builder()
                    .borderId(borderId)
                    .name(def.name)
                    .requiredLevel(def.requiredLevel)
                    .borderStyle(generateBorderStyle(def, true))
                    .borderStyleLight(generateBorderStyle(def, false))
                    .gradientFrom(def.gradientFrom)
                    .gradientTo(def.gradientTo)
                    .unlocked(unlocked)
                    .selected(selected)
                    .build());
        }

        // 배경색 목록
        List<BackgroundResponse> backgrounds = new ArrayList<>();
        for (Map.Entry<String, BackgroundDefinition> entry : BACKGROUND_DEFINITIONS.entrySet()) {
            String bgId = entry.getKey();
            BackgroundDefinition def = entry.getValue();
            boolean selected = bgId.equals(selectedBackgroundId);

            backgrounds.add(BackgroundResponse.builder()
                    .backgroundId(bgId)
                    .name(def.name)
                    .colorClass(def.colorClass)
                    .colorHex(def.colorHex)
                    .selected(selected)
                    .build());
        }

        // 칭호 목록
        List<TitleResponse> titles = new ArrayList<>();
        for (Map.Entry<String, TitleDefinition> entry : TITLE_DEFINITIONS.entrySet()) {
            String titleId = entry.getKey();
            TitleDefinition def = entry.getValue();

            boolean unlocked;
            if (def.isSpecial) {
                // 특별 칭호: 업적 달성 여부로 해금
                unlocked = specialTitleUnlocked.getOrDefault(titleId, false);
            } else {
                // 레벨 기반 칭호: 레벨로 해금
                unlocked = currentLevel >= def.requiredLevel;
            }
            boolean selected = titleId.equals(selectedTitleId);

            titles.add(TitleResponse.builder()
                    .titleId(titleId)
                    .name(def.name)
                    .emoji(def.emoji)
                    .colorClass(def.colorClass)
                    .colorHex(def.colorHex)
                    .requiredLevel(def.requiredLevel)
                    .condition(def.condition)
                    .unlocked(unlocked)
                    .selected(selected)
                    .build());
        }

        // 헤더 색상 목록
        List<HeaderResponse> headers = new ArrayList<>();
        for (Map.Entry<String, HeaderDefinition> entry : HEADER_DEFINITIONS.entrySet()) {
            String headerId = entry.getKey();
            HeaderDefinition def = entry.getValue();
            boolean unlocked = currentLevel >= def.requiredLevel;
            boolean selected = headerId.equals(selectedHeaderId);

            headers.add(HeaderResponse.builder()
                    .headerId(headerId)
                    .name(def.name)
                    .requiredLevel(def.requiredLevel)
                    .colorClass(def.colorClass)
                    .colorHex(def.colorHex)
                    .gradientFrom(def.gradientFrom)
                    .gradientTo(def.gradientTo)
                    .unlocked(unlocked)
                    .selected(selected)
                    .build());
        }

        return BorderListResponse.builder()
                .borders(borders)
                .backgrounds(backgrounds)
                .titles(titles)
                .headers(headers)
                .selectedBorderId(selectedBorderId)
                .selectedBackgroundId(selectedBackgroundId)
                .selectedTitleId(selectedTitleId)
                .selectedHeaderId(selectedHeaderId)
                .currentLevel(currentLevel)
                .build();
    }

    /**
     * 특별 칭호 해금 조건 체크
     */
    private Map<String, Boolean> checkSpecialTitlesUnlocked(Long memberId) {
        Map<String, Boolean> result = new HashMap<>();

        // 복습왕: 복습 모드 200회 달성
        Long reviewCount = quizAttemptRepository.countReviewModeByMemberId(memberId);
        result.put("title_reviewer", reviewCount != null && reviewCount >= 200);

        // 스트릭 관련
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId).orElse(null);
        // 스트릭 마스터: 30일 연속 학습
        result.put("title_streaker", streak != null && streak.getMaxStreak() >= 30);
        // 7일 챌린저: 7일 연속 학습
        result.put("title_7days", streak != null && streak.getMaxStreak() >= 7);

        // 얼리버드: 아침 6-10시 50문제
        Long earlyBirdCount = quizAttemptRepository.countMorningQuizzesByMemberId(memberId);
        result.put("title_earlybird", earlyBirdCount != null && earlyBirdCount >= 50);

        // 올빼미: 밤 22-02시 50문제
        Long nightOwlCount = quizAttemptRepository.countNightQuizzesByMemberId(memberId);
        result.put("title_nightowl", nightOwlCount != null && nightOwlCount >= 50);

        // 노력파: 오답 100회 이상
        Long wrongCount = quizAttemptRepository.countWrongAnswersByMemberId(memberId);
        result.put("title_effort", wrongCount != null && wrongCount >= 100);

        // 포기란 없다: 오답 200회 이상
        result.put("title_nevergiveup", wrongCount != null && wrongCount >= 200);

        // 오백전사: 총 500문제 풀기
        Long totalCount = quizAttemptRepository.countByMemberId(memberId);
        result.put("title_500warrior", totalCount != null && totalCount >= 500);

        // 밥 좀 드세요: 점심시간(13-14시) 30문제
        Long lunchCount = quizAttemptRepository.countLunchTimeQuizzesByMemberId(memberId);
        result.put("title_lunchtime", lunchCount != null && lunchCount >= 30);

        return result;
    }

    /**
     * 테두리 선택
     */
    @Transactional
    public void selectBorder(Long memberId, String borderId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (borderId == null || borderId.isEmpty()) {
            member.setSelectedBorderId(null);
            memberRepository.save(member);
            return;
        }

        // 테두리 존재 여부 확인
        BorderDefinition def = BORDER_DEFINITIONS.get(borderId);
        if (def == null) {
            throw new RuntimeException("존재하지 않는 테두리입니다.");
        }

        // 해금 여부 확인
        int currentLevel = calculateLevel(memberId);
        if (currentLevel < def.requiredLevel) {
            throw new RuntimeException("아직 해금되지 않은 테두리입니다. (필요 레벨: " + def.requiredLevel + ")");
        }

        member.setSelectedBorderId(borderId);
        memberRepository.save(member);
    }

    /**
     * 특정 테두리 정보 조회 (갤러리용)
     */
    public BorderResponse getBorderInfo(String borderId) {
        if (borderId == null || borderId.isEmpty()) {
            return null;
        }

        BorderDefinition def = BORDER_DEFINITIONS.get(borderId);
        if (def == null) {
            return null;
        }

        return BorderResponse.builder()
                .borderId(borderId)
                .name(def.name)
                .requiredLevel(def.requiredLevel)
                .borderStyle(generateBorderStyle(def, true))
                .borderStyleLight(generateBorderStyle(def, false))
                .gradientFrom(def.gradientFrom)
                .gradientTo(def.gradientTo)
                .unlocked(true)
                .selected(false)
                .build();
    }

    /**
     * 레벨 계산 (QuizService와 동일한 공식)
     * rawScore = (푼 문제 수 × 정답률/100) + (복습 횟수 / 10) + (최대 스트릭 × 5) + (좋아요 × 2) + (댓글 × 2)
     */
    private int calculateLevel(Long memberId) {
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId)
                .orElse(null);

        if (streak == null) {
            // 스트릭 없어도 커뮤니티 활동으로 레벨업 가능
            int likesGiven = portfolioLikeRepository.countLikesGivenByMemberId(memberId);
            int commentsGiven = commentRepository.countCommentsGivenByMemberId(memberId);
            double rawScore = (likesGiven * 2) + (commentsGiven * 2);
            return Math.min(100, (int) Math.floor(rawScore / 10.0));
        }

        double accuracy = streak.getTotalQuizCount() > 0
                ? (streak.getCorrectCount() * 100.0 / streak.getTotalQuizCount())
                : 0;

        Long reviewCount = quizAttemptRepository.countReviewModeByMemberId(memberId);
        if (reviewCount == null) reviewCount = 0L;

        // 커뮤니티 활동 보너스
        int likesGiven = portfolioLikeRepository.countLikesGivenByMemberId(memberId);
        int commentsGiven = commentRepository.countCommentsGivenByMemberId(memberId);

        double rawScore = (streak.getTotalQuizCount() * (accuracy / 100.0))
                + (reviewCount / 2.0)
                + (streak.getMaxStreak() * 5)
                + (likesGiven * 2)
                + (commentsGiven * 2);

        return Math.min(100, (int) Math.floor(rawScore / 10.0));
    }

    /**
     * CSS 스타일 생성
     */
    private String generateBorderStyle(BorderDefinition def, boolean isDark) {
        // Tailwind CSS 클래스 형태로 반환
        return String.format("from-%s to-%s", def.gradientFrom, def.gradientTo);
    }

    /**
     * 배경색 선택
     */
    @Transactional
    public void selectBackground(Long memberId, String backgroundId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (backgroundId == null || backgroundId.isEmpty()) {
            member.setSelectedBackgroundId(null);
            memberRepository.save(member);
            return;
        }

        // 배경색 존재 여부 확인
        if (!BACKGROUND_DEFINITIONS.containsKey(backgroundId)) {
            throw new RuntimeException("존재하지 않는 배경색입니다.");
        }

        member.setSelectedBackgroundId(backgroundId);
        memberRepository.save(member);
    }

    /**
     * 특정 배경색 정보 조회 (갤러리용)
     */
    public BackgroundResponse getBackgroundInfo(String backgroundId) {
        if (backgroundId == null || backgroundId.isEmpty()) {
            return null;
        }

        BackgroundDefinition def = BACKGROUND_DEFINITIONS.get(backgroundId);
        if (def == null) {
            return null;
        }

        return BackgroundResponse.builder()
                .backgroundId(backgroundId)
                .name(def.name)
                .colorClass(def.colorClass)
                .colorHex(def.colorHex)
                .selected(false)
                .build();
    }

    /**
     * 칭호 선택
     */
    @Transactional
    public void selectTitle(Long memberId, String titleId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (titleId == null || titleId.isEmpty()) {
            member.setSelectedTitleId(null);
            memberRepository.save(member);
            return;
        }

        // 칭호 존재 여부 확인
        TitleDefinition def = TITLE_DEFINITIONS.get(titleId);
        if (def == null) {
            throw new RuntimeException("존재하지 않는 칭호입니다.");
        }

        // 해금 여부 확인
        if (def.isSpecial) {
            Map<String, Boolean> specialUnlocked = checkSpecialTitlesUnlocked(memberId);
            if (!specialUnlocked.getOrDefault(titleId, false)) {
                throw new RuntimeException("아직 해금되지 않은 칭호입니다. (조건: " + def.condition + ")");
            }
        } else {
            int currentLevel = calculateLevel(memberId);
            if (currentLevel < def.requiredLevel) {
                throw new RuntimeException("아직 해금되지 않은 칭호입니다. (필요 레벨: " + def.requiredLevel + ")");
            }
        }

        member.setSelectedTitleId(titleId);
        memberRepository.save(member);
    }

    /**
     * 특정 칭호 정보 조회 (갤러리용)
     */
    public TitleResponse getTitleInfo(String titleId) {
        if (titleId == null || titleId.isEmpty()) {
            return null;
        }

        TitleDefinition def = TITLE_DEFINITIONS.get(titleId);
        if (def == null) {
            return null;
        }

        return TitleResponse.builder()
                .titleId(titleId)
                .name(def.name)
                .emoji(def.emoji)
                .colorClass(def.colorClass)
                .colorHex(def.colorHex)
                .requiredLevel(def.requiredLevel)
                .condition(def.condition)
                .unlocked(true)
                .selected(false)
                .build();
    }

    /**
     * 헤더 색상 선택
     */
    @Transactional
    public void selectHeader(Long memberId, String headerId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (headerId == null || headerId.isEmpty()) {
            member.setSelectedHeaderId(null);
            memberRepository.save(member);
            return;
        }

        // 헤더 존재 여부 확인
        HeaderDefinition def = HEADER_DEFINITIONS.get(headerId);
        if (def == null) {
            throw new RuntimeException("존재하지 않는 헤더 색상입니다.");
        }

        // 해금 여부 확인
        int currentLevel = calculateLevel(memberId);
        if (currentLevel < def.requiredLevel) {
            throw new RuntimeException("아직 해금되지 않은 헤더 색상입니다. (필요 레벨: " + def.requiredLevel + ")");
        }

        member.setSelectedHeaderId(headerId);
        memberRepository.save(member);
    }

    /**
     * 특정 헤더 색상 정보 조회 (갤러리용)
     */
    public HeaderResponse getHeaderInfo(String headerId) {
        if (headerId == null || headerId.isEmpty()) {
            return null;
        }

        HeaderDefinition def = HEADER_DEFINITIONS.get(headerId);
        if (def == null) {
            return null;
        }

        return HeaderResponse.builder()
                .headerId(headerId)
                .name(def.name)
                .requiredLevel(def.requiredLevel)
                .colorClass(def.colorClass)
                .colorHex(def.colorHex)
                .gradientFrom(def.gradientFrom)
                .gradientTo(def.gradientTo)
                .unlocked(true)
                .selected(false)
                .build();
    }

    /**
     * 테두리 정의 내부 클래스
     */
    private static class BorderDefinition {
        final String name;
        final int requiredLevel;
        final String gradientFrom;
        final String gradientTo;

        BorderDefinition(String name, int requiredLevel, String gradientFrom, String gradientTo) {
            this.name = name;
            this.requiredLevel = requiredLevel;
            this.gradientFrom = gradientFrom;
            this.gradientTo = gradientTo;
        }
    }

    /**
     * 배경색 정의 내부 클래스
     */
    private static class BackgroundDefinition {
        final String name;
        final String colorClass;
        final String colorHex;

        BackgroundDefinition(String name, String colorClass, String colorHex) {
            this.name = name;
            this.colorClass = colorClass;
            this.colorHex = colorHex;
        }
    }

    /**
     * 칭호 정의 내부 클래스
     */
    private static class TitleDefinition {
        final String name;
        final String emoji;
        final String colorClass;
        final String colorHex;
        final int requiredLevel;
        final String condition;
        final boolean isSpecial;  // 특별 칭호 여부 (업적 기반)

        TitleDefinition(String name, String emoji, String colorClass, String colorHex, int requiredLevel, String condition, boolean isSpecial) {
            this.name = name;
            this.emoji = emoji;
            this.colorClass = colorClass;
            this.colorHex = colorHex;
            this.requiredLevel = requiredLevel;
            this.condition = condition;
            this.isSpecial = isSpecial;
        }
    }

    /**
     * 헤더 색상 정의 내부 클래스
     */
    private static class HeaderDefinition {
        final String name;
        final int requiredLevel;
        final String colorClass;
        final String colorHex;
        final String gradientFrom;  // 그라데이션 시작 (고급 헤더용)
        final String gradientTo;    // 그라데이션 끝 (고급 헤더용)

        HeaderDefinition(String name, int requiredLevel, String colorClass, String colorHex, String gradientFrom, String gradientTo) {
            this.name = name;
            this.requiredLevel = requiredLevel;
            this.colorClass = colorClass;
            this.colorHex = colorHex;
            this.gradientFrom = gradientFrom;
            this.gradientTo = gradientTo;
        }
    }
}

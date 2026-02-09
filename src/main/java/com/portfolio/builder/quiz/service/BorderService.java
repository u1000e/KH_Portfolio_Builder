package com.portfolio.builder.quiz.service;

import com.portfolio.builder.comment.domain.CommentRepository;
import com.portfolio.builder.interview.domain.InterviewAnswerRepository;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.quiz.domain.Badge;
import com.portfolio.builder.portfolio.domain.PortfolioLikeRepository;
import com.portfolio.builder.til.domain.TILRepository;
import com.portfolio.builder.quiz.domain.QuizStreak;
import com.portfolio.builder.quiz.dto.QuizDto.*;
import com.portfolio.builder.quiz.repository.BadgeRepository;
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
    private final BadgeRepository badgeRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final TILRepository tilRepository;

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

        // 칭호 보유 시 해금 테두리
        BORDER_DEFINITIONS.put("border_500warrior", new BorderDefinition("바이올렛 그라데이션", 0, "violet-500", "purple-700", "title_500warrior"));
        BORDER_DEFINITIONS.put("border_600swordsman", new BorderDefinition("실버블랙 그라데이션", 0, "gray-200", "gray-600", "title_600swordsman"));
        BORDER_DEFINITIONS.put("border_weekly_1st", new BorderDefinition("로즈골드 그라데이션", 0, "rose-500", "pink-300", "amber-400", "title_weekly_reviewer_1st"));
        BORDER_DEFINITIONS.put("border_weekly_2nd", new BorderDefinition("선셋 그라데이션", 0, "orange-400", "amber-600", "title_weekly_reviewer_2nd"));
        BORDER_DEFINITIONS.put("border_weekly_3rd", new BorderDefinition("로즈 그라데이션", 0, "pink-300", "rose-400", "title_weekly_reviewer_3rd"));
        BORDER_DEFINITIONS.put("border_best_answer", new BorderDefinition("딥오션 그라데이션", 0, "blue-500", "teal-600", "cyan-400", "title_best_answer_1st"));
        BORDER_DEFINITIONS.put("border_knowledge", new BorderDefinition("네이비 그라데이션", 0, "blue-700", "blue-400", "slate-500", "title_best_answer_2nd"));
        BORDER_DEFINITIONS.put("border_answer_mate", new BorderDefinition("옐로우 그라데이션", 0, "yellow-300", "amber-400", "title_best_answer_3rd"));
        BORDER_DEFINITIONS.put("border_7days", new BorderDefinition("시안 그라데이션", 0, "teal-400", "cyan-600", "title_7days"));
        BORDER_DEFINITIONS.put("border_streaker", new BorderDefinition("미드나잇 그라데이션", 0, "slate-700", "amber-400", "red-500", "title_streaker"));
        BORDER_DEFINITIONS.put("border_777jackpot", new BorderDefinition("잭팟 그라데이션", 0, "amber-400", "orange-500", "red-500", "title_777jackpot"));
        BORDER_DEFINITIONS.put("border_1000conqueror", new BorderDefinition("컨커러 그라데이션", 0, "rose-500", "teal-500", "amber-500", "title_1000conqueror"));

        // TIL 마일스톤 테두리
        BORDER_DEFINITIONS.put("border_til_first", new BorderDefinition("그린 그라데이션", 0, "emerald-400", "teal-500", "title_til_first"));
        BORDER_DEFINITIONS.put("border_til_50", new BorderDefinition("캔디 그라데이션", 0, "yellow-300", "pink-400", "title_til_50"));
        BORDER_DEFINITIONS.put("border_til_100", new BorderDefinition("코스믹 그라데이션", 0, "cyan-400", "teal-400", "blue-500", "title_til_100"));

        // 고레벨 달성 테두리
        BORDER_DEFINITIONS.put("border_level_150", new BorderDefinition("스프링 그라데이션", 0, "lime-300", "sky-400", "teal-400", "title_level_150"));
        BORDER_DEFINITIONS.put("border_level_200", new BorderDefinition("아이스 그라데이션", 0, "sky-500", "slate-700", "indigo-500", "title_level_200"));

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

        // 채도 높은 파스텔 (-100/-200 레벨)
        BACKGROUND_DEFINITIONS.put("bg_lavender", new BackgroundDefinition("라벤더", "bg-violet-200", "#ddd6fe"));
        BACKGROUND_DEFINITIONS.put("bg_peach", new BackgroundDefinition("피치", "bg-orange-200", "#fed7aa"));
        BACKGROUND_DEFINITIONS.put("bg_blush", new BackgroundDefinition("블러시", "bg-rose-200", "#fecdd3"));
        BACKGROUND_DEFINITIONS.put("bg_butter", new BackgroundDefinition("버터", "bg-amber-100", "#fef3c3"));
        BACKGROUND_DEFINITIONS.put("bg_sky_deep", new BackgroundDefinition("딥 스카이", "bg-sky-200", "#bae6fd"));
        BACKGROUND_DEFINITIONS.put("bg_mint_deep", new BackgroundDefinition("딥 민트", "bg-teal-200", "#99f6e4"));
        BACKGROUND_DEFINITIONS.put("bg_sage", new BackgroundDefinition("세이지", "bg-emerald-200", "#a7f3d0"));
        BACKGROUND_DEFINITIONS.put("bg_periwinkle", new BackgroundDefinition("페리윙클", "bg-indigo-200", "#c7d2fe"));

        // 뉴 컬렉션 배경색 (기존과 다른 결)
        BACKGROUND_DEFINITIONS.put("bg_charcoal", new BackgroundDefinition("차콜", "bg-gray-300", "#d1d5db"));
        BACKGROUND_DEFINITIONS.put("bg_ice", new BackgroundDefinition("아이스", "bg-cyan-100", "#cffafe"));
        BACKGROUND_DEFINITIONS.put("bg_coral", new BackgroundDefinition("코랄", "bg-red-100", "#fee2e2"));
        BACKGROUND_DEFINITIONS.put("bg_lilac", new BackgroundDefinition("라일락", "bg-purple-100", "#f3e8ff"));
        BACKGROUND_DEFINITIONS.put("bg_milktea", new BackgroundDefinition("밀크티", "bg-stone-200", "#e7e5e4"));

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

        // 칭호 보유 시 해금 헤더 (requiredLevel=0, requiredTitleId로 해금)
        HEADER_DEFINITIONS.put("header_500warrior", new HeaderDefinition("퍼플 그라데이션", 0, null, null, "from-violet-300", "to-purple-500", "title_500warrior", true));
        HEADER_DEFINITIONS.put("header_600swordsman", new HeaderDefinition("실버블랙 그라데이션", 0, null, null, "from-gray-100", "to-gray-700", "title_600swordsman", true));
        HEADER_DEFINITIONS.put("header_weekly_1st", new HeaderDefinition("로즈골드 그라데이션", 0, null, null, "from-rose-500", "to-pink-300", "via-amber-400", "title_weekly_reviewer_1st", false));
        HEADER_DEFINITIONS.put("header_weekly_2nd", new HeaderDefinition("선셋 그라데이션", 0, null, null, "from-orange-300", "to-amber-500", "title_weekly_reviewer_2nd"));
        HEADER_DEFINITIONS.put("header_weekly_3rd", new HeaderDefinition("로즈 그라데이션", 0, null, null, "from-pink-100", "to-rose-200", "title_weekly_reviewer_3rd"));
        HEADER_DEFINITIONS.put("header_best_answer", new HeaderDefinition("딥오션 그라데이션", 0, null, null, "from-blue-600", "to-teal-500", "via-cyan-400", "title_best_answer_1st", true));
        HEADER_DEFINITIONS.put("header_knowledge", new HeaderDefinition("네이비 그라데이션", 0, null, null, "from-blue-700", "to-blue-400", "via-slate-500", "title_best_answer_2nd", true));
        HEADER_DEFINITIONS.put("header_answer_mate", new HeaderDefinition("옐로우 그라데이션", 0, null, null, "from-yellow-100", "to-amber-200", "title_best_answer_3rd"));
        HEADER_DEFINITIONS.put("header_7days", new HeaderDefinition("시안 그라데이션", 0, null, null, "from-teal-300", "to-cyan-500", "title_7days", true));
        HEADER_DEFINITIONS.put("header_streaker", new HeaderDefinition("미드나잇 그라데이션", 0, null, null, "from-slate-800", "to-amber-400", "via-red-500", "title_streaker", true));
        HEADER_DEFINITIONS.put("header_777jackpot", new HeaderDefinition("잭팟 그라데이션", 0, null, null, "from-yellow-300", "to-orange-500", "via-red-500", "title_777jackpot", true));
        HEADER_DEFINITIONS.put("header_1000conqueror", new HeaderDefinition("컨커러 그라데이션", 0, null, null, "from-rose-400", "to-teal-500", "via-amber-500", "title_1000conqueror", true));

        // TIL 마일스톤 헤더
        HEADER_DEFINITIONS.put("header_til_first", new HeaderDefinition("그린 그라데이션", 0, null, null, "from-emerald-200", "to-teal-200", "title_til_first"));
        HEADER_DEFINITIONS.put("header_til_50", new HeaderDefinition("캔디 그라데이션", 0, null, null, "from-yellow-200", "to-pink-300", "title_til_50"));
        HEADER_DEFINITIONS.put("header_til_100", new HeaderDefinition("코스믹 그라데이션", 0, null, null, "from-cyan-400", "to-teal-300", "via-blue-500", "title_til_100", false));

        // 고레벨 달성 헤더
        HEADER_DEFINITIONS.put("header_level_150", new HeaderDefinition("스프링 그라데이션", 0, null, null, "from-lime-300", "to-sky-400", "via-teal-400", "title_level_150", false));
        HEADER_DEFINITIONS.put("header_level_200", new HeaderDefinition("아이스 그라데이션", 0, null, null, "from-sky-500", "to-slate-700", "via-indigo-500", "title_level_200", true));

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
        TITLE_DEFINITIONS.put("title_100", new TitleDefinition("개발왕", "🏅", "text-amber-400", "#fbbf24", 100, "레벨 100 달성", false));

        // 특별 칭호 (레벨 조건 없음, 특정 업적 달성 시)
        TITLE_DEFINITIONS.put("title_earlybird", new TitleDefinition("얼리버드", "🌅", "text-orange-400", "#fb923c", 0, "아침 6-9시 50문제 풀기", true));
        TITLE_DEFINITIONS.put("title_nightowl", new TitleDefinition("올빼미", "🦉", "text-indigo-400", "#818cf8", 0, "밤 22-02시 50문제 풀기", true));
        TITLE_DEFINITIONS.put("title_reviewer", new TitleDefinition("복습왕", "📚", "text-emerald-500", "#10b981", 0, "복습 모드 200회 달성", true));
        TITLE_DEFINITIONS.put("title_streaker", new TitleDefinition("스트릭 마스터", "🔥", "text-red-400", "#f87171", 0, "30일 연속 학습 달성", true));

        // 추가 특별 칭호
        TITLE_DEFINITIONS.put("title_effort", new TitleDefinition("노력파", "💪", "text-blue-500", "#3b82f6", 0, "오답 100회 이상", true));
        TITLE_DEFINITIONS.put("title_nevergiveup", new TitleDefinition("포기란 없다", "🥊", "text-rose-500", "#f43f5e", 0, "오답 200회 이상", true));
        TITLE_DEFINITIONS.put("title_500warrior", new TitleDefinition("오백전사", "🛡", "text-violet-500", "#8b5cf6", 0, "총 500문제 풀기", true));
        TITLE_DEFINITIONS.put("title_600swordsman", new TitleDefinition("퀴즈 헌터", "🗡", "text-slate-600", "#475569", 0, "총 600문제 풀기", true));
        TITLE_DEFINITIONS.put("title_777jackpot", new TitleDefinition("럭키 세븐", "🎰", "text-amber-500", "#f59e0b", 0, "총 777문제 풀기", true));
        TITLE_DEFINITIONS.put("title_1000conqueror", new TitleDefinition("승철링고 정복", "⚔", "text-rose-600", "#e11d48", 0, "총 1000문제 풀기", true));
        TITLE_DEFINITIONS.put("title_lunchtime", new TitleDefinition("밥 좀 드세요", "🍱", "text-amber-500", "#f59e0b", 0, "점심시간(13-14시) 30문제 풀기", true));
        TITLE_DEFINITIONS.put("title_7days", new TitleDefinition("7일 챌린저", "🗓️", "text-cyan-500", "#06b6d4", 0, "7일 연속 학습 달성", true));

        // 주간 베스트 리뷰어 칭호
        TITLE_DEFINITIONS.put("title_weekly_reviewer_1st", new TitleDefinition("주간 리뷰왕", "💖", "text-pink-500", "#ec4899", 0, "주간 베스트 리뷰어 1등 달성", true));
        TITLE_DEFINITIONS.put("title_weekly_reviewer_2nd", new TitleDefinition("주간 리뷰메이트", "💞", "text-rose-400", "#fb7185", 0, "주간 베스트 리뷰어 2등 달성", true));
        TITLE_DEFINITIONS.put("title_weekly_reviewer_3rd", new TitleDefinition("주간 리뷰버디", "💌", "text-pink-300", "#f9a8d4", 0, "주간 베스트 리뷰어 3등 달성", true));

        // 면접 토론 칭호 (총 좋아요 기준)
        TITLE_DEFINITIONS.put("title_best_answer_1st", new TitleDefinition("면접 해결사", "🕵️", "text-red-500", "#ef4444", 0, "면접 토론 답변에 총 좋아요 100개", true));
        TITLE_DEFINITIONS.put("title_best_answer_2nd", new TitleDefinition("지식 전도사", "🧙", "text-purple-500", "#a855f7", 0, "면접 토론 답변에 총 좋아요 50개", true));
        TITLE_DEFINITIONS.put("title_best_answer_3rd", new TitleDefinition("답변메이트", "✨", "text-yellow-300", "#fde047", 0, "면접 토론 답변에 총 좋아요 10개", true));
        TITLE_DEFINITIONS.put("title_discussion_master", new TitleDefinition("토론왕", "🏇", "text-blue-500", "#3b82f6", 0, "면접 토론 답변 100개 이상", true));

        // 피드백 칭호
        TITLE_DEFINITIONS.put("title_feedback_star", new TitleDefinition("참잘했어요", "🍒", "text-red-400", "#f87171", 0, "피드백 5회 이상 반영", true));

        // TIL 마일스톤 칭호
        TITLE_DEFINITIONS.put("title_til_first", new TitleDefinition("TIL 입문자", "📝", "text-emerald-500", "#10b981", 0, "TIL 1개 작성", true));
        TITLE_DEFINITIONS.put("title_til_50", new TitleDefinition("TIL 장인", "📓", "text-teal-500", "#14b8a6", 0, "TIL 50개 작성", true));
        TITLE_DEFINITIONS.put("title_til_100", new TitleDefinition("TIL 마스터", "🛸", "text-cyan-600", "#0891b2", 0, "TIL 100개 작성", true));

        // 고레벨 달성 칭호
        TITLE_DEFINITIONS.put("title_level_150", new TitleDefinition("야근 졸업생", "🎓", "text-teal-500", "#14b8a6", 0, "레벨 150 달성", true));
        TITLE_DEFINITIONS.put("title_level_200", new TitleDefinition("스택 오버플로우", "🌊", "text-indigo-600", "#4f46e5", 0, "레벨 200 달성", true));
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
            boolean unlocked;
            String unlockCondition;
            if (def.requiredTitleId != null) {
                unlocked = specialTitleUnlocked.getOrDefault(def.requiredTitleId, false);
                TitleDefinition titleDef = TITLE_DEFINITIONS.get(def.requiredTitleId);
                unlockCondition = "칭호: " + (titleDef != null ? titleDef.name : "특별 칭호");
            } else {
                unlocked = currentLevel >= def.requiredLevel;
                unlockCondition = "Lv." + def.requiredLevel;
            }
            boolean selected = borderId.equals(selectedBorderId);

            borders.add(BorderResponse.builder()
                    .borderId(borderId)
                    .name(def.name)
                    .requiredLevel(def.requiredLevel)
                    .borderStyle(generateBorderStyle(def, true))
                    .borderStyleLight(generateBorderStyle(def, false))
                    .gradientFrom(def.gradientFrom)
                    .gradientTo(def.gradientTo)
                    .gradientVia(def.gradientVia)
                    .unlocked(unlocked)
                    .selected(selected)
                    .unlockCondition(unlockCondition)
                    .build());
        }

        // 배경색 목록 (전체 해금)
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
                    .unlocked(true)
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
            boolean unlocked;
            if (def.requiredTitleId != null) {
                // 칭호 보유 시 해금
                unlocked = specialTitleUnlocked.getOrDefault(def.requiredTitleId, false);
            } else {
                // 레벨 기반 해금
                unlocked = currentLevel >= def.requiredLevel;
            }
            boolean selected = headerId.equals(selectedHeaderId);

            // 해금 조건 텍스트 생성
            String unlockCondition;
            if (def.requiredTitleId != null) {
                TitleDefinition titleDef = TITLE_DEFINITIONS.get(def.requiredTitleId);
                unlockCondition = "칭호: " + (titleDef != null ? titleDef.name : "특별 칭호");
            } else {
                unlockCondition = "Lv." + def.requiredLevel;
            }

            headers.add(HeaderResponse.builder()
                    .headerId(headerId)
                    .name(def.name)
                    .requiredLevel(def.requiredLevel)
                    .colorClass(def.colorClass)
                    .colorHex(def.colorHex)
                    .gradientFrom(def.gradientFrom)
                    .gradientTo(def.gradientTo)
                    .gradientVia(def.gradientVia)
                    .unlocked(unlocked)
                    .selected(selected)
                    .unlockCondition(unlockCondition)
                    .isDark(def.isDark)
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

        // 퀴즈 헌터: 총 600문제 풀기
        result.put("title_600swordsman", totalCount != null && totalCount >= 600);

        // 럭키 세븐: 총 777문제 풀기
        result.put("title_777jackpot", totalCount != null && totalCount >= 777);

        // 승철링고 정복: 총 1000문제 풀기
        result.put("title_1000conqueror", totalCount != null && totalCount >= 1000);

        // 밥 좀 드세요: 점심시간(13-14시) 30문제
        Long lunchCount = quizAttemptRepository.countLunchTimeQuizzesByMemberId(memberId);
        result.put("title_lunchtime", lunchCount != null && lunchCount >= 30);

        // 주간 리뷰어 칭호 해금 (배지 보유 여부로 판단)
        result.put("title_weekly_reviewer_1st", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_weekly_reviewer_1st"));
        result.put("title_weekly_reviewer_2nd", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_weekly_reviewer_2nd"));
        result.put("title_weekly_reviewer_3rd", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_weekly_reviewer_3rd"));

        // 면접 토론 칭호 해금 (배지 보유 여부로 판단)
        result.put("title_best_answer_1st", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_best_answer_1st"));
        result.put("title_best_answer_2nd", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_best_answer_2nd"));
        result.put("title_best_answer_3rd", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_best_answer_3rd"));
        result.put("title_discussion_master", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_discussion_master"));

        // 피드백 칭호 해금 (배지 보유 여부로 판단)
        result.put("title_feedback_star", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_feedback_star"));

        // TIL 마일스톤 칭호 해금 (배지 보유 여부로 판단)
        result.put("title_til_first", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_til_first"));
        result.put("title_til_50", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_til_50"));
        result.put("title_til_100", badgeRepository.existsByMemberIdAndBadgeId(memberId, "hidden_til_100"));

        // 고레벨 달성 칭호 해금
        int currentLevel = calculateLevel(memberId);
        result.put("title_level_150", currentLevel >= 150);
        result.put("title_level_200", currentLevel >= 200);

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
        if (def.requiredTitleId != null) {
            // 칭호 기반 해금
            Map<String, Boolean> specialUnlocked = checkSpecialTitlesUnlocked(memberId);
            if (!specialUnlocked.getOrDefault(def.requiredTitleId, false)) {
                throw new RuntimeException("아직 해금되지 않은 테두리입니다. (필요 칭호 미보유)");
            }
        } else {
            // 레벨 기반 해금
            int currentLevel = calculateLevel(memberId);
            if (currentLevel < def.requiredLevel) {
                throw new RuntimeException("아직 해금되지 않은 테두리입니다. (필요 레벨: " + def.requiredLevel + ")");
            }
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
                .gradientVia(def.gradientVia)
                .unlocked(true)
                .selected(false)
                .build();
    }

    /**
     * TIL 칭호 해금 (배지를 통한 해금 기록)
     */
    @Transactional
    public void unlockTitleIfNotOwned(Long memberId, String titleId) {
        String badgeId = "hidden_" + titleId.substring("title_".length());
        if (!badgeRepository.existsByMemberIdAndBadgeId(memberId, badgeId)) {
            Member member = memberRepository.findById(memberId).orElse(null);
            if (member == null) return;
            Badge badge = Badge.builder()
                    .member(member)
                    .badgeId(badgeId)
                    .build();
            badgeRepository.save(badge);
        }
    }

    /**
     * 레벨 계산 (QuizService와 동일한 공식)
     * rawScore = (푼 문제 수 × 정답률/100) + (복습 횟수 / 2) + (최대 스트릭 × 5) + (좋아요 × 2) + (댓글 × 2) + (면접 답변 수 × 2) + (TIL 작성 수 × 2)
     */
    private int calculateLevel(Long memberId) {
        QuizStreak streak = quizStreakRepository.findByMemberId(memberId)
                .orElse(null);

        // 면접 답변 + TIL 보너스
        long answerCount = interviewAnswerRepository.countByMemberId(memberId);
        long tilCount = tilRepository.countByMemberId(memberId);

        if (streak == null) {
            // 스트릭 없어도 커뮤니티 활동으로 레벨업 가능
            int likesGiven = portfolioLikeRepository.countLikesGivenByMemberId(memberId);
            int commentsGiven = commentRepository.countCommentsGivenByMemberId(memberId);
            double rawScore = (likesGiven * 2) + (commentsGiven * 2) + (answerCount * 2) + (tilCount * 2);
            return Math.min(200, (int) Math.floor(rawScore / 10.0));
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
                + (commentsGiven * 2)
                + (answerCount * 2)
                + (tilCount * 2);

        return Math.min(200, (int) Math.floor(rawScore / 10.0));
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
        BackgroundDefinition def = BACKGROUND_DEFINITIONS.get(backgroundId);
        if (def == null) {
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
        if (def.requiredTitleId != null) {
            // 칭호 보유 시 해금
            Map<String, Boolean> specialUnlocked = checkSpecialTitlesUnlocked(memberId);
            if (!specialUnlocked.getOrDefault(def.requiredTitleId, false)) {
                throw new RuntimeException("아직 해금되지 않은 헤더 색상입니다. (필요 칭호 미획득)");
            }
        } else {
            int currentLevel = calculateLevel(memberId);
            if (currentLevel < def.requiredLevel) {
                throw new RuntimeException("아직 해금되지 않은 헤더 색상입니다. (필요 레벨: " + def.requiredLevel + ")");
            }
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
                .gradientVia(def.gradientVia)
                .unlocked(true)
                .selected(false)
                .isDark(def.isDark)
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
        final String gradientVia;  // 3색 그라데이션 중간색
        final String requiredTitleId;

        BorderDefinition(String name, int requiredLevel, String gradientFrom, String gradientTo) {
            this(name, requiredLevel, gradientFrom, gradientTo, null, null);
        }

        BorderDefinition(String name, int requiredLevel, String gradientFrom, String gradientTo, String requiredTitleId) {
            this(name, requiredLevel, gradientFrom, gradientTo, null, requiredTitleId);
        }

        // 3색 그라데이션용 생성자
        BorderDefinition(String name, int requiredLevel, String gradientFrom, String gradientTo, String gradientVia, String requiredTitleId) {
            this.name = name;
            this.requiredLevel = requiredLevel;
            this.gradientFrom = gradientFrom;
            this.gradientTo = gradientTo;
            this.gradientVia = gradientVia;
            this.requiredTitleId = requiredTitleId;
        }
    }

    /**
     * 배경색 정의 내부 클래스
     */
    private static class BackgroundDefinition {
        final String name;
        final String colorClass;
        final String colorHex;
        final String requiredTitleId;

        BackgroundDefinition(String name, String colorClass, String colorHex) {
            this(name, colorClass, colorHex, null);
        }

        BackgroundDefinition(String name, String colorClass, String colorHex, String requiredTitleId) {
            this.name = name;
            this.colorClass = colorClass;
            this.colorHex = colorHex;
            this.requiredTitleId = requiredTitleId;
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
        final String gradientFrom;
        final String gradientTo;
        final String gradientVia;  // 3색 그라데이션 중간색
        final String requiredTitleId;
        final boolean isDark;  // 어두운 헤더 (흰색 글자 필요)

        HeaderDefinition(String name, int requiredLevel, String colorClass, String colorHex, String gradientFrom, String gradientTo) {
            this(name, requiredLevel, colorClass, colorHex, gradientFrom, gradientTo, null, null, false);
        }

        HeaderDefinition(String name, int requiredLevel, String colorClass, String colorHex, String gradientFrom, String gradientTo, String requiredTitleId) {
            this(name, requiredLevel, colorClass, colorHex, gradientFrom, gradientTo, null, requiredTitleId, false);
        }

        HeaderDefinition(String name, int requiredLevel, String colorClass, String colorHex, String gradientFrom, String gradientTo, String requiredTitleId, boolean isDark) {
            this(name, requiredLevel, colorClass, colorHex, gradientFrom, gradientTo, null, requiredTitleId, isDark);
        }

        // 3색 그라데이션용 생성자
        HeaderDefinition(String name, int requiredLevel, String colorClass, String colorHex, String gradientFrom, String gradientTo, String gradientVia, String requiredTitleId, boolean isDark) {
            this.name = name;
            this.requiredLevel = requiredLevel;
            this.colorClass = colorClass;
            this.colorHex = colorHex;
            this.gradientFrom = gradientFrom;
            this.gradientTo = gradientTo;
            this.gradientVia = gradientVia;
            this.requiredTitleId = requiredTitleId;
            this.isDark = isDark;
        }
    }
}

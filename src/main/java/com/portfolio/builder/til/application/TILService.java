package com.portfolio.builder.til.application;

import com.portfolio.builder.comment.application.ProfanityFilterService;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import com.portfolio.builder.quiz.service.BadgeService;
import com.portfolio.builder.quiz.service.BorderService;
import com.portfolio.builder.til.domain.TIL;
import com.portfolio.builder.til.domain.TILLike;
import com.portfolio.builder.til.domain.TILLikeRepository;
import com.portfolio.builder.til.domain.TILRepository;
import com.portfolio.builder.til.dto.TILRequest;
import com.portfolio.builder.til.dto.TILResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TILService {

    private final TILRepository tilRepository;
    private final TILLikeRepository tilLikeRepository;
    private final MemberRepository memberRepository;
    private final ProfanityFilterService profanityFilterService;
    private final BadgeService badgeService;
    private final BorderService borderService;

    public TILResponse createTIL(Long memberId, TILRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (profanityFilterService.containsProfanity(request.getTitle()) ||
            profanityFilterService.containsProfanity(request.getDifficulty()) ||
            (request.getDescription() != null && profanityFilterService.containsProfanity(request.getDescription()))) {
            throw new IllegalArgumentException("부적절한 표현이 포함되어 있습니다.");
        }

        TIL til = TIL.builder()
                .member(member)
                .title(request.getTitle())
                .difficulty(request.getDifficulty())
                .description(request.getDescription())
                .codeSnippet(request.getCodeSnippet())
                .codeLanguage(request.getCodeLanguage())
                .tags(request.getTags())
                .imageUrl(request.getImageUrl())
                .reflection(request.getReflection())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .build();

        TIL saved = tilRepository.save(til);
        log.info("TIL created: {} by member: {}", saved.getId(), memberId);

        long tilCount = tilRepository.countByMemberId(memberId);
        checkTILBadges(memberId, tilCount);

        return TILResponse.from(saved, memberId, false);
    }

    @Transactional(readOnly = true)
    public List<TILResponse> getFeed(Long memberId, String sort, String tag, int limit, boolean myClassOnly) {
        Pageable pageable = PageRequest.of(0, limit);
        List<TIL> tils;

        Member currentMember = memberRepository.findById(memberId).orElse(null);

        // 같은 반 필터링
        boolean useClassFilter = myClassOnly && currentMember != null
                && currentMember.getBranch() != null
                && currentMember.getClassroom() != null
                && currentMember.getCohort() != null;

        if (useClassFilter) {
            String branch = currentMember.getBranch();
            String classroom = currentMember.getClassroom();
            String cohort = currentMember.getCohort();

            if (tag != null && !tag.isEmpty()) {
                tils = tilRepository.findByClassAndTagContaining(branch, classroom, cohort, tag, pageable);
            } else if ("popular".equals(sort)) {
                tils = tilRepository.findAllPublicByClassOrderByLikeCountDesc(branch, classroom, cohort, pageable);
            } else {
                tils = tilRepository.findAllPublicByClassOrderByCreatedAtDesc(branch, classroom, cohort, pageable);
            }
        } else {
            if (tag != null && !tag.isEmpty()) {
                tils = tilRepository.findByTagContaining(tag, pageable);
            } else if ("popular".equals(sort)) {
                tils = tilRepository.findAllPublicOrderByLikeCountDesc(pageable);
            } else {
                tils = tilRepository.findAllPublicOrderByCreatedAtDesc(pageable);
            }
        }

        return tils.stream()
                .map(til -> {
                    boolean isLiked = tilLikeRepository.existsByTilIdAndMemberId(til.getId(), memberId);
                    return TILResponse.from(til, memberId, isLiked);
                })
                .collect(Collectors.toList());
    }

    // 강사용: 반별 TIL 대시보드
    @Transactional(readOnly = true)
    public Map<String, Object> getTilDashboard(String branch, String classroom, String cohort) {
        Map<String, Object> result = new HashMap<>();

        // TIL 작성 통계 (학생별)
        List<Object[]> stats = tilRepository.findTilStatsByClass(branch, classroom, cohort);
        List<Map<String, Object>> students = new ArrayList<>();
        long totalTils = 0;

        for (Object[] row : stats) {
            Map<String, Object> student = new HashMap<>();
            student.put("memberId", row[0]);
            student.put("name", row[1]);
            student.put("avatarUrl", row[2]);
            student.put("githubUsername", row[3]);
            student.put("tilCount", row[4]);
            student.put("lastTilDate", row[5]);
            students.add(student);
            totalTils += ((Number) row[4]).longValue();
        }

        result.put("students", students);
        result.put("totalTils", totalTils);
        result.put("writersCount", students.size());

        return result;
    }

    // 강사용: 반별 특정 날짜 TIL 조회
    @Transactional(readOnly = true)
    public List<TILResponse> getTilsByClassAndDate(Long memberId, String branch, String classroom, String cohort, LocalDate date) {
        List<TIL> tils;
        if (date != null) {
            tils = tilRepository.findAllByClassAndDate(branch, classroom, cohort, date);
        } else {
            tils = tilRepository.findAllByClassOrderByCreatedAtDesc(branch, classroom, cohort);
        }
        return tils.stream()
                .map(til -> TILResponse.from(til, memberId, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TILResponse> getMyTILs(Long memberId) {
        return tilRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(til -> {
                    boolean isLiked = tilLikeRepository.existsByTilIdAndMemberId(til.getId(), memberId);
                    return TILResponse.from(til, memberId, isLiked);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TILResponse getTIL(Long tilId, Long memberId) {
        TIL til = tilRepository.findById(tilId)
                .orElseThrow(() -> new RuntimeException("TIL을 찾을 수 없습니다."));
        boolean isLiked = tilLikeRepository.existsByTilIdAndMemberId(tilId, memberId);
        return TILResponse.from(til, memberId, isLiked);
    }

    public TILResponse updateTIL(Long tilId, Long memberId, TILRequest request) {
        TIL til = tilRepository.findById(tilId)
                .orElseThrow(() -> new RuntimeException("TIL을 찾을 수 없습니다."));

        if (!til.getMember().getId().equals(memberId)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        if (profanityFilterService.containsProfanity(request.getTitle()) ||
            profanityFilterService.containsProfanity(request.getDifficulty()) ||
            (request.getDescription() != null && profanityFilterService.containsProfanity(request.getDescription()))) {
            throw new IllegalArgumentException("부적절한 표현이 포함되어 있습니다.");
        }

        til.setTitle(request.getTitle());
        til.setDifficulty(request.getDifficulty());
        til.setDescription(request.getDescription());
        til.setCodeSnippet(request.getCodeSnippet());
        til.setCodeLanguage(request.getCodeLanguage());
        til.setTags(request.getTags());
        til.setImageUrl(request.getImageUrl());
        til.setReflection(request.getReflection());
        if (request.getIsPublic() != null) {
            til.setIsPublic(request.getIsPublic());
        }

        boolean isLiked = tilLikeRepository.existsByTilIdAndMemberId(tilId, memberId);
        return TILResponse.from(til, memberId, isLiked);
    }

    public void deleteTIL(Long tilId, Long memberId) {
        TIL til = tilRepository.findById(tilId)
                .orElseThrow(() -> new RuntimeException("TIL을 찾을 수 없습니다."));

        if (!til.getMember().getId().equals(memberId)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        tilLikeRepository.deleteAllByTilId(tilId);
        tilRepository.delete(til);
        log.info("TIL deleted: {} by member: {}", tilId, memberId);
    }

    public Map<String, Object> toggleLike(Long tilId, Long memberId) {
        TIL til = tilRepository.findById(tilId)
                .orElseThrow(() -> new RuntimeException("TIL을 찾을 수 없습니다."));

        Optional<TILLike> existingLike = tilLikeRepository.findByTilIdAndMemberId(tilId, memberId);
        boolean isLiked;

        if (existingLike.isPresent()) {
            tilLikeRepository.delete(existingLike.get());
            til.decrementLikeCount();
            isLiked = false;
        } else {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            TILLike like = TILLike.builder()
                    .til(til)
                    .member(member)
                    .build();
            tilLikeRepository.save(like);
            til.incrementLikeCount();
            isLiked = true;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", til.getLikeCount());
        return result;
    }

    @Transactional(readOnly = true)
    public List<TILResponse> getRecentTILsForPortfolio(Long memberId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tilRepository.findRecentByMemberId(memberId, pageable)
                .stream()
                .map(til -> TILResponse.from(til, memberId, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TILResponse> getPublicTILsByMemberId(Long memberId) {
        return tilRepository.findAllPublicByMemberId(memberId)
                .stream()
                .map(til -> TILResponse.from(til, null, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getWrittenDates(Long memberId, int year, int month) {
        return tilRepository.findWrittenDatesByMemberAndMonth(memberId, year, month);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTilStatsForPortfolio(Long memberId) {
        long totalCount = tilRepository.countByMemberId(memberId);

        List<TIL> myTils = tilRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        // 태그 빈도 집계 → 상위 5개
        Map<String, Long> tagFrequency = new HashMap<>();
        for (TIL til : myTils) {
            if (til.getTags() != null && !til.getTags().isEmpty()) {
                for (String tag : til.getTags().split(",")) {
                    String trimmed = tag.trim();
                    if (!trimmed.isEmpty()) {
                        tagFrequency.merge(trimmed, 1L, Long::sum);
                    }
                }
            }
        }

        List<String> topTags = tagFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 스트릭 계산 (연속 학습일)
        Set<LocalDate> writtenDates = myTils.stream()
                .map(til -> til.getCreatedAt().toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));

        int currentStreak = 0;
        int maxStreak = 0;

        if (!writtenDates.isEmpty()) {
            // 현재 스트릭: 오늘/어제부터 역순으로 연속일 카운트
            LocalDate check = LocalDate.now();
            if (!writtenDates.contains(check)) {
                check = check.minusDays(1); // 어제까지 썼으면 유지
            }
            while (writtenDates.contains(check)) {
                currentStreak++;
                check = check.minusDays(1);
            }

            // 최대 스트릭
            List<LocalDate> sorted = new ArrayList<>(new TreeSet<>(writtenDates));
            int streak = 1;
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).minusDays(1).equals(sorted.get(i - 1))) {
                    streak++;
                } else {
                    maxStreak = Math.max(maxStreak, streak);
                    streak = 1;
                }
            }
            maxStreak = Math.max(maxStreak, streak);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("topTags", topTags);
        result.put("currentStreak", currentStreak);
        result.put("maxStreak", maxStreak);
        return result;
    }

    private void checkTILBadges(Long memberId, long tilCount) {
        if (tilCount == 1) {
            badgeService.awardHiddenBadge(memberId, "hidden_first_til");
        }
        if (tilCount == 7) {
            badgeService.awardHiddenBadge(memberId, "hidden_til_week");
        }
        if (tilCount == 30) {
            badgeService.awardHiddenBadge(memberId, "hidden_til_month");
        }

        // TIL 마일스톤 칭호 해금
        if (tilCount >= 1) {
            borderService.unlockTitleIfNotOwned(memberId, "title_til_first");
        }
        if (tilCount >= 50) {
            borderService.unlockTitleIfNotOwned(memberId, "title_til_50");
        }
        if (tilCount >= 100) {
            borderService.unlockTitleIfNotOwned(memberId, "title_til_100");
        }
    }
}

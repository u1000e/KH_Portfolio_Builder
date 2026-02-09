package com.portfolio.builder.til.presentation;

import com.portfolio.builder.til.application.TILService;
import com.portfolio.builder.til.dto.TILRequest;
import com.portfolio.builder.til.dto.TILResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/til")
@RequiredArgsConstructor
@Slf4j
public class TILController {

    private final TILService tilService;

    @PostMapping
    public ResponseEntity<TILResponse> createTIL(
            @RequestAttribute(name = "memberId") Long memberId,
            @Valid @RequestBody TILRequest request) {
        return ResponseEntity.ok(tilService.createTIL(memberId, request));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<TILResponse>> getFeed(
            @RequestAttribute(name = "memberId") Long memberId,
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "myClassOnly", defaultValue = "false") boolean myClassOnly) {
        return ResponseEntity.ok(tilService.getFeed(memberId, sort, tag, limit, myClassOnly));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TILResponse>> getMyTILs(
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(tilService.getMyTILs(memberId));
    }

    @GetMapping("/{tilId}")
    public ResponseEntity<TILResponse> getTIL(
            @PathVariable("tilId") Long tilId,
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(tilService.getTIL(tilId, memberId));
    }

    @PutMapping("/{tilId}")
    public ResponseEntity<TILResponse> updateTIL(
            @PathVariable("tilId") Long tilId,
            @RequestAttribute(name = "memberId") Long memberId,
            @Valid @RequestBody TILRequest request) {
        return ResponseEntity.ok(tilService.updateTIL(tilId, memberId, request));
    }

    @DeleteMapping("/{tilId}")
    public ResponseEntity<Void> deleteTIL(
            @PathVariable("tilId") Long tilId,
            @RequestAttribute(name = "memberId") Long memberId) {
        tilService.deleteTIL(tilId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tilId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable("tilId") Long tilId,
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(tilService.toggleLike(tilId, memberId));
    }

    @GetMapping("/portfolio/{memberId}")
    public ResponseEntity<List<TILResponse>> getTILsForPortfolio(
            @PathVariable("memberId") Long memberId,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(tilService.getRecentTILsForPortfolio(memberId, limit));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<LocalDate>> getWrittenDates(
            @RequestAttribute(name = "memberId") Long memberId,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month) {
        return ResponseEntity.ok(tilService.getWrittenDates(memberId, year, month));
    }

    @GetMapping("/my/stats")
    public ResponseEntity<Map<String, Object>> getMyTilStats(
            @RequestAttribute(name = "memberId") Long memberId) {
        return ResponseEntity.ok(tilService.getTilStatsForPortfolio(memberId));
    }

    // 강사용: 반별 TIL 대시보드 (통계)
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getTilDashboard(
            @RequestParam(name = "branch") String branch,
            @RequestParam(name = "classroom") String classroom,
            @RequestParam(name = "cohort") String cohort) {
        return ResponseEntity.ok(tilService.getTilDashboard(branch, classroom, cohort));
    }

    // 강사용: 반별 TIL 목록 (날짜 필터 가능)
    @GetMapping("/class")
    public ResponseEntity<List<TILResponse>> getTilsByClass(
            @RequestAttribute(name = "memberId") Long memberId,
            @RequestParam(name = "branch") String branch,
            @RequestParam(name = "classroom") String classroom,
            @RequestParam(name = "cohort") String cohort,
            @RequestParam(name = "date", required = false) String date) {
        LocalDate localDate = date != null && !date.isEmpty() ? LocalDate.parse(date) : null;
        return ResponseEntity.ok(tilService.getTilsByClassAndDate(memberId, branch, classroom, cohort, localDate));
    }
}

package com.portfolio.builder.activity.presentation;

import com.portfolio.builder.activity.application.ActivityFeedService;
import com.portfolio.builder.activity.dto.ActivityFeedDto;
import com.portfolio.builder.member.domain.Member;
import com.portfolio.builder.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;
    private final MemberRepository memberRepository;

    /**
     * 활동 피드 조회
     * - 로그인한 사용자의 반 기준 또는 전체
     */
    @GetMapping("/feed")
    public ResponseEntity<List<ActivityFeedDto>> getActivityFeed(
            @RequestAttribute("memberId") Long memberId,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "myClassOnly", defaultValue = "false") boolean myClassOnly
    ) {
        String branch = null;
        String classroom = null;
        String cohort = null;

        if (myClassOnly) {
            Member member = memberRepository.findById(memberId).orElse(null);
            if (member != null) {
                branch = member.getBranch();
                classroom = member.getClassroom();
                cohort = member.getCohort();
            }
        }

        List<ActivityFeedDto> feeds = activityFeedService.getRecentActivities(branch, classroom, cohort, limit);
        return ResponseEntity.ok(feeds);
    }
}

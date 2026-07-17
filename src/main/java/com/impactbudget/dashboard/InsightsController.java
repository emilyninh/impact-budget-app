package com.impactbudget.dashboard;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/** Actionable insights for the authenticated user — currently "greener swap" suggestions. */
@RestController
@RequestMapping("/api/v1/insights")
class InsightsController {

    private final RecommendationService recommendationService;

    InsightsController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/swaps")
    List<RecommendationService.Swap> swaps(@AuthenticationPrincipal String userId,
                                           @RequestParam(required = false) String month) {
        return recommendationService.greenerSwaps(userId, month(month));
    }

    private String month(String month) {
        return StringUtils.hasText(month) ? month : YearMonth.now().toString();
    }
}

package com.impactbudget.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Maintenance actions scoped to the authenticated user (the JWT subject — never a client-supplied
 * id). {@code POST /recategorize} re-derives categories for already-loaded transactions after a
 * taxonomy change (see {@link RecategorizeService}); it's idempotent and safe to re-run.
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController {

    private final RecategorizeService recategorizeService;
    private final RescoreService rescoreService;

    AdminController(RecategorizeService recategorizeService, RescoreService rescoreService) {
        this.recategorizeService = recategorizeService;
        this.rescoreService = rescoreService;
    }

    @PostMapping("/recategorize")
    Map<String, Integer> recategorize(@AuthenticationPrincipal String userId) {
        return Map.of("updated", recategorizeService.recategorize(userId));
    }

    /** Re-score the user's transactions so improved scoring (e.g. website signals) reaches old data. */
    @PostMapping("/rescore")
    Map<String, Integer> rescore(@AuthenticationPrincipal String userId) {
        return Map.of("merchants", rescoreService.rescore(userId));
    }
}

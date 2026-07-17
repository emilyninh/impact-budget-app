package com.impactbudget.dashboard;

import com.impactbudget.common.DeadLetterService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoint to replay dead-lettered events back onto their original topics. Requires
 * authentication (any signed-in user in this portfolio build).
 */
@RestController
@RequestMapping("/api/v1/admin/dlq")
class DlqController {

    private final DeadLetterService deadLetterService;

    DlqController(DeadLetterService deadLetterService) {
        this.deadLetterService = deadLetterService;
    }

    @PostMapping("/replay")
    ReplayResponse replay() {
        return new ReplayResponse(deadLetterService.replayAll());
    }

    record ReplayResponse(int replayed) {
    }
}

package com.impactbudget.dashboard;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Live dashboard stream. The browser's {@code EventSource} can't set an Authorization header,
 * so it passes the JWT as {@code ?token=}, which the JwtAuthFilter accepts — hence the
 * authenticated principal is available here as usual.
 */
@RestController
@RequestMapping("/api/v1/stream")
class SseController {

    private final SseHub hub;

    SseController(SseHub hub) {
        this.hub = hub;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@AuthenticationPrincipal String userId) {
        return hub.subscribe(userId);
    }
}

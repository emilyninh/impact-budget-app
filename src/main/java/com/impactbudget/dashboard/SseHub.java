package com.impactbudget.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds live Server-Sent Events connections, one or more per user, and pushes updates to them.
 * Dead connections are pruned on completion/timeout/error.
 */
@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);
    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<String, Collection<SseEmitter>> byUser = new ConcurrentHashMap<>();

    /** Open a stream for a user and register lifecycle cleanup. */
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        byUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    /** Send an event to all of a user's open streams. */
    public void push(String userId, String eventName, Object data) {
        Collection<SseEmitter> emitters = byUser.get(userId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.debug("Dropping dead SSE connection for {} ({})", userId, e.toString());
                remove(userId, emitter);
            }
        }
    }

    private void remove(String userId, SseEmitter emitter) {
        Collection<SseEmitter> emitters = byUser.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}

package com.yourlifeone.live.application.service;

import com.yourlifeone.commonservice.changes.ChangeEvent;
import com.yourlifeone.live.api.mapper.LiveEventMapper;
import com.yourlifeone.live.config.LiveProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Every open stream, by user. A user's events go only to that user's streams. A stream ends when its
 * access token expires (the tab reconnects with a fresh one) and a quiet stream gets a comment line on
 * every heartbeat, so proxies keep it open.
 */
@Service
public class LiveStreams {
    private static final Logger logger = LoggerFactory.getLogger(LiveStreams.class);
    private static final String READY = "ready";
    private static final String CHANGE = "change";
    private static final long MIN_TIMEOUT_MS = 1000;

    private final Map<UUID, Deque<SseEmitter>> streams = new ConcurrentHashMap<>();
    private final LiveProperties properties;
    private final LiveEventMapper mapper;
    private final Clock clock;

    public LiveStreams(LiveProperties properties, LiveEventMapper mapper, Clock clock, MeterRegistry meters) {
        this.properties = properties;
        this.mapper = mapper;
        this.clock = clock;
        Gauge.builder("yourlife.live.streams", this, LiveStreams::count)
                .description("Open live-update streams")
                .register(meters);
    }

    /**
     * Opens a stream for the user. Its first event, {@code ready}, tells the tab it is connected, so it can
     * reload anything it may have missed while it was not.
     */
    public SseEmitter open(UUID userId, Instant tokenExpiresAt) {
        var emitter = new SseEmitter(timeoutMs(tokenExpiresAt));
        var userStreams = streams.computeIfAbsent(userId, ignored -> new ConcurrentLinkedDeque<>());
        userStreams.add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> remove(userId, emitter));
        closeOldestBeyondLimit(userStreams);
        send(userId, emitter, SseEmitter.event().name(READY).data(clock.instant().toString()));
        return emitter;
    }

    /** Sends the event to each of its user's open streams. */
    public void deliver(ChangeEvent event) {
        var userStreams = streams.get(event.userId());
        if (userStreams == null) return;
        var response = mapper.toResponse(event);
        for (var emitter : List.copyOf(userStreams)) {
            send(event.userId(), emitter, SseEmitter.event().name(CHANGE).data(response, MediaType.APPLICATION_JSON));
        }
    }

    @Scheduled(fixedDelayString = "${app.live.heartbeat}")
    public void heartbeat() {
        streams.forEach((userId, userStreams) -> List.copyOf(userStreams)
                .forEach(emitter -> send(userId, emitter, SseEmitter.event().comment("keep-alive"))));
    }

    public int count() {
        return streams.values().stream().mapToInt(Deque::size).sum();
    }

    private long timeoutMs(Instant tokenExpiresAt) {
        var now = clock.instant();
        var latest = now.plus(properties.maxStreamDuration());
        var until = tokenExpiresAt == null || tokenExpiresAt.isAfter(latest) ? latest : tokenExpiresAt;
        return Math.max(MIN_TIMEOUT_MS, Duration.between(now, until).toMillis());
    }

    private void closeOldestBeyondLimit(Deque<SseEmitter> userStreams) {
        while (userStreams.size() > properties.maxStreamsPerUser()) {
            var oldest = userStreams.poll();
            if (oldest != null) oldest.complete();
        }
    }

    private void send(UUID userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            logger.debug("Live stream closed while sending: {}", exception.getMessage());
            remove(userId, emitter);
            emitter.completeWithError(exception);
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        streams.computeIfPresent(userId, (ignored, userStreams) -> {
            userStreams.remove(emitter);
            return userStreams.isEmpty() ? null : userStreams;
        });
    }
}

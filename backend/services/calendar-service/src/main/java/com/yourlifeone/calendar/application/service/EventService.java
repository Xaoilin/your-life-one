package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.model.EventDetails;
import com.yourlifeone.calendar.application.model.GoogleEvent;
import com.yourlifeone.calendar.application.model.GoogleEventDraft;
import com.yourlifeone.calendar.application.port.GoogleCalendarApi;
import com.yourlifeone.calendar.config.CalendarProperties;
import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.calendar.domain.CalendarEvent;
import com.yourlifeone.calendar.domain.CalendarSource;
import com.yourlifeone.calendar.domain.EventTiming;
import com.yourlifeone.calendar.infrastructure.persistence.entity.EventEntity;
import com.yourlifeone.calendar.infrastructure.persistence.mapper.CalendarPersistenceMapper;
import com.yourlifeone.calendar.infrastructure.persistence.repository.EventRepository;
import com.yourlifeone.commonservice.web.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calendar events. An event on a Google calendar is written to Google first and stored only once
 * Google accepts it, so the two never disagree; a refused change leaves both untouched.
 */
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository events;
    private final SourceService sources;
    private final AccountService accounts;
    private final GoogleAccessTokens googleTokens;
    private final GoogleCalendarApi google;
    private final CalendarPersistenceMapper mapper;
    private final CalendarProperties properties;
    private final Clock clock;
    private final CalendarSyncFreshness freshness;

    /**
     * Events overlapping the dates {@code from} to {@code to} (inclusive) in any time zone: the range
     * is widened by a day on each side, so callers filter by their own zone.
     */
    public List<CalendarEvent> list(UUID userId, LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > properties.maxRangeDays()) {
            throw ApiException.badRequest("invalid_range",
                    "from must not be after to, and the range must be at most " + properties.maxRangeDays() + " days.");
        }
        var start = from.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var end = to.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant();
        return events.findByUserIdAndStartsAtLessThanAndEndsAtGreaterThanEqualOrderByStartsAtAscIdAsc(userId, end, start)
                .stream().map(mapper::toEvent).toList();
    }

    /**
     * The Google account an event write on this calendar goes through; empty for local calendars and
     * for a missing calendar, which the write itself then reports.
     */
    public Optional<CalendarAccount> googleAccountForSource(UUID userId, String sourceId) {
        return sources.lookup(userId, sourceId)
                .filter(CalendarSource::isGoogle)
                .flatMap(source -> accounts.lookup(userId, source.accountId()));
    }

    /**
     * The Google account a change to this event goes through; empty for local events and for events
     * that no longer exist, so a retried delete still reaches its stored result.
     */
    public Optional<CalendarAccount> googleAccountForEvent(UUID userId, String eventId) {
        return events.findByUserIdAndId(userId, eventId)
                .flatMap(event -> googleAccountForSource(userId, event.getSourceId()));
    }

    @Transactional
    public CalendarEvent create(UUID userId, EventDetails details) {
        freshness.invalidate(userId);
        var source = writableSource(userId, details.sourceId());
        var entity = EventEntity.builder()
                .userId(userId)
                .id(UUID.randomUUID().toString())
                .sourceId(source.id())
                .createdAt(clock.instant())
                .build();
        if (source.isGoogle()) {
            var created = google.create(tokenFor(userId, source), source.googleCalendarId(), draft(details));
            entity.setGoogleEventId(created.id());
            apply(entity, created);
        } else {
            apply(entity, details);
        }
        entity.setUpdatedAt(clock.instant());
        return mapper.toEvent(events.save(entity));
    }

    /** Changes an event; it can move between local calendars, but not into or out of Google. */
    @Transactional
    public CalendarEvent update(UUID userId, String eventId, EventDetails details) {
        freshness.invalidate(userId);
        var entity = find(userId, eventId);
        var current = sources.get(userId, entity.getSourceId());
        var target = details.sourceId().equals(current.id()) ? current : writableSource(userId, details.sourceId());
        if (target != current && (current.isGoogle() || target.isGoogle())) {
            throw ApiException.conflict("move_not_supported", "Events cannot move to or from a Google calendar.");
        }
        requireWritable(current);
        if (current.isGoogle()) {
            var updated = google.update(tokenFor(userId, current), current.googleCalendarId(), entity.getGoogleEventId(),
                    draft(details));
            apply(entity, updated);
        } else {
            entity.setSourceId(target.id());
            apply(entity, details);
        }
        entity.setUpdatedAt(clock.instant());
        return mapper.toEvent(entity);
    }

    @Transactional
    public void delete(UUID userId, String eventId) {
        freshness.invalidate(userId);
        var entity = find(userId, eventId);
        var source = sources.get(userId, entity.getSourceId());
        if (source.isGoogle()) {
            requireWritable(source);
            google.delete(tokenFor(userId, source), source.googleCalendarId(), entity.getGoogleEventId());
        }
        events.delete(entity);
    }

    private String tokenFor(UUID userId, CalendarSource source) {
        return googleTokens.current(userId, accounts.get(userId, source.accountId()).email());
    }

    private CalendarSource writableSource(UUID userId, String sourceId) {
        var source = sources.get(userId, sourceId);
        requireWritable(source);
        return source;
    }

    private static void requireWritable(CalendarSource source) {
        if (!source.isWritable()) {
            throw ApiException.conflict("calendar_read_only", "This Google calendar is read-only.");
        }
    }

    private static GoogleEventDraft draft(EventDetails details) {
        return GoogleEventDraft.builder()
                .title(details.title())
                .description(details.description())
                .location(details.location())
                .timing(details.timing())
                .timeZone(details.timeZone() == null ? "UTC" : details.timeZone())
                .build();
    }

    private static void apply(EventEntity entity, EventDetails details) {
        entity.setTitle(details.title().trim());
        entity.setDescription(details.description() == null ? "" : details.description());
        entity.setLocation(blankToNull(details.location()));
        setTiming(entity, details.timing());
    }

    /** Stores what Google saved, which is what the next sync will mirror. */
    static void apply(EventEntity entity, GoogleEvent event) {
        entity.setTitle(event.title());
        entity.setDescription(event.description());
        entity.setLocation(event.location());
        setTiming(entity, event.timing());
    }

    private static void setTiming(EventEntity entity, EventTiming timing) {
        entity.setAllDay(timing.allDay());
        entity.setStartsAt(timing.startsAt());
        entity.setEndsAt(timing.endsAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private EventEntity find(UUID userId, String eventId) {
        return events.findByUserIdAndId(userId, eventId)
                .orElseThrow(() -> ApiException.notFound("event_not_found", "This event does not exist."));
    }
}

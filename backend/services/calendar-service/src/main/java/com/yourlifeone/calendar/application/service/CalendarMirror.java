package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.model.GoogleCalendar;
import com.yourlifeone.calendar.application.model.GoogleEvent;
import com.yourlifeone.calendar.config.CalendarProperties;
import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.calendar.domain.CalendarSource;
import com.yourlifeone.calendar.infrastructure.persistence.entity.EventEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.SourceEntity;
import com.yourlifeone.calendar.infrastructure.persistence.mapper.CalendarPersistenceMapper;
import com.yourlifeone.calendar.infrastructure.persistence.repository.EventRepository;
import com.yourlifeone.calendar.infrastructure.persistence.repository.SourceRepository;
import com.yourlifeone.calendar.infrastructure.persistence.model.EventComparisonValue;
import com.yourlifeone.calendar.infrastructure.persistence.model.EventDifference;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes what Google returned into the calendar tables. Each call is one short transaction, made
 * after Google has answered, so no database connection is held while waiting on Google.
 */
@Component
@RequiredArgsConstructor
public class CalendarMirror {
    private final SourceRepository sources;
    private final EventRepository events;
    private final CalendarPersistenceMapper mapper;
    private final CalendarProperties properties;
    private final Clock clock;

    /**
     * Makes the account's Google calendars match Google's calendar list: new calendars are added,
     * listed ones keep the user's colour and visibility, and unlisted ones are removed with their
     * events. A calendar shared into several of the user's accounts is kept under the account that
     * has it first, so its events are not shown twice.
     *
     * @return the account's Google calendars after reconciling
     */
    @Transactional
    public List<CalendarSource> reconcileCalendars(UUID userId, CalendarAccount account, List<GoogleCalendar> listed) {
        var now = clock.instant();
        var ownedElsewhere = sources.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
                .filter(source -> source.getGoogleCalendarId() != null && !source.getAccountId().equals(account.id()))
                .map(SourceEntity::getGoogleCalendarId)
                .collect(Collectors.toSet());
        var existing = sources.findByUserIdAndAccountId(userId, account.id()).stream()
                .filter(source -> source.getGoogleCalendarId() != null)
                .collect(Collectors.toMap(SourceEntity::getGoogleCalendarId, Function.identity()));
        var kept = new ArrayList<SourceEntity>();
        for (var calendar : listed) {
            var source = existing.remove(calendar.id());
            if (source == null) {
                if (ownedElsewhere.contains(calendar.id())) continue;
                source = sources.save(newSource(userId, account, calendar, now));
            }
            source.setName(nameOf(calendar));
            source.setAccessRole(calendar.accessRole());
            kept.add(source);
        }
        sources.deleteAll(existing.values());
        return kept.stream().map(mapper::toSource).toList();
    }

    /**
     * Makes the calendar's events overlapping {@code [from, to)} match Google's: fetched events are
     * added or updated (matched by Google event ID), and stored ones Google no longer returns are removed.
     *
     * @return how many events the calendar has in the window
     */
    @Transactional
    public int mirrorEvents(UUID userId, CalendarSource source, List<GoogleEvent> fetched, Instant from, Instant to) {
        var now = clock.instant();
        var byGoogleId = fetched.stream()
                .collect(Collectors.toMap(GoogleEvent::id, Function.identity(), (first, duplicate) -> first));
        Map<String, EventEntity> stored = new HashMap<>();
        var values = byGoogleId.values().stream().map(CalendarMirror::comparisonValue).toList();
        var differences = events.differences(userId, source.id(), values, from, to);
        var changedIds = differences.stream().filter(diff -> diff.googleId() != null && diff.storedId() != null)
                .map(EventDifference::storedId).toList();
        if (!changedIds.isEmpty()) {
            events.findByUserIdAndSourceIdAndIdIn(userId, source.id(), changedIds)
                    .forEach(event -> stored.put(event.getGoogleEventId(), event));
        }
        var inserts = new ArrayList<EventEntity>();
        for (var difference : differences) {
            if (difference.googleId() == null) continue;
            var event = byGoogleId.get(difference.googleId());
            var entity = stored.get(event.id());
            if (entity == null) {
                entity = EventEntity.builder().userId(userId).id(UUID.randomUUID().toString())
                        .sourceId(source.id()).googleEventId(event.id()).createdAt(now).build();
                inserts.add(entity);
            }
            EventService.apply(entity, event);
            entity.setUpdatedAt(now);
        }
        events.insertAll(inserts);
        var deletedIds = differences.stream().filter(diff -> diff.googleId() == null)
                .map(EventDifference::storedId).toList();
        if (!deletedIds.isEmpty()) events.deleteMirrored(userId, source.id(), deletedIds);
        return byGoogleId.size();
    }

    private SourceEntity newSource(UUID userId, CalendarAccount account, GoogleCalendar calendar, Instant now) {
        var color = calendar.backgroundColor() == null ? properties.defaultSourceColor() : calendar.backgroundColor();
        return SourceEntity.builder()
                .userId(userId)
                .id(UUID.randomUUID().toString())
                .accountId(account.id())
                .name(nameOf(calendar))
                .color(color)
                .visible(!Boolean.FALSE.equals(calendar.selected()))
                .googleCalendarId(calendar.id())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static String nameOf(GoogleCalendar calendar) {
        var name = calendar.summary() == null || calendar.summary().isBlank() ? calendar.id() : calendar.summary();
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private static EventComparisonValue comparisonValue(GoogleEvent event) {
        return EventComparisonValue.builder().id(event.id()).title(event.title()).description(event.description())
                .location(event.location()).allDay(event.timing().allDay())
                .startsAt(event.timing().startsAt().toString()).endsAt(event.timing().endsAt().toString()).build();
    }
}

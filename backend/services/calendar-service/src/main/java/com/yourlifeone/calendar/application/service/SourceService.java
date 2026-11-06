package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.domain.CalendarSource;
import com.yourlifeone.calendar.infrastructure.persistence.entity.SourceEntity;
import com.yourlifeone.calendar.infrastructure.persistence.mapper.CalendarPersistenceMapper;
import com.yourlifeone.calendar.infrastructure.persistence.repository.SourceRepository;
import com.yourlifeone.commonservice.web.ApiException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calendars inside accounts. Google calendars are created, renamed and removed by sync; the user
 * chooses only their colour and visibility. Local calendars are fully editable.
 */
@Service
@RequiredArgsConstructor
public class SourceService {
    private final SourceRepository sources;
    private final AccountService accounts;
    private final CalendarPersistenceMapper mapper;
    private final Clock clock;
    private final CalendarSyncFreshness freshness;

    public List<CalendarSource> list(UUID userId) {
        return sources.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream().map(mapper::toSource).toList();
    }

    public CalendarSource get(UUID userId, String sourceId) {
        return mapper.toSource(find(userId, sourceId));
    }

    /** The calendar, or empty when it does not exist. */
    public Optional<CalendarSource> lookup(UUID userId, String sourceId) {
        return sources.findByUserIdAndId(userId, sourceId).map(mapper::toSource);
    }

    @Transactional
    public CalendarSource createLocal(UUID userId, String accountId, String name, String color, Boolean visible) {
        freshness.invalidate(userId);
        requireLocalAccount(userId, accountId);
        var now = clock.instant();
        return mapper.toSource(sources.save(SourceEntity.builder()
                .userId(userId)
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .name(name.trim())
                .color(color.trim())
                .visible(visible == null || visible)
                .createdAt(now)
                .updatedAt(now)
                .build()));
    }

    /** Null fields are left unchanged; a local calendar can move to another local account. */
    @Transactional
    public CalendarSource update(UUID userId, String sourceId, String name, String color, Boolean visible,
            String accountId) {
        freshness.invalidate(userId);
        var source = find(userId, sourceId);
        boolean google = source.getGoogleCalendarId() != null;
        boolean renames = name != null && !name.trim().equals(source.getName());
        boolean moves = accountId != null && !accountId.equals(source.getAccountId());
        if (google && (renames || moves)) {
            throw googleManaged("Google calendars keep the name and account Google gives them.");
        }
        if (moves) {
            requireLocalAccount(userId, accountId);
            source.setAccountId(accountId);
        }
        if (renames) source.setName(name.trim());
        if (color != null) source.setColor(color.trim());
        if (visible != null) source.setVisible(visible);
        source.setUpdatedAt(clock.instant());
        return mapper.toSource(source);
    }

    /** Deletes a local calendar and its events. */
    @Transactional
    public void delete(UUID userId, String sourceId) {
        freshness.invalidate(userId);
        var source = find(userId, sourceId);
        if (source.getGoogleCalendarId() != null) {
            throw googleManaged("Google calendars are removed by disconnecting their Google account.");
        }
        sources.delete(source);
    }

    private void requireLocalAccount(UUID userId, String accountId) {
        if (accounts.get(userId, accountId).isGoogle()) {
            throw googleManaged("Calendars of a Google account come from Google; add them there.");
        }
    }

    private SourceEntity find(UUID userId, String sourceId) {
        return sources.findByUserIdAndId(userId, sourceId)
                .orElseThrow(() -> ApiException.notFound("source_not_found", "This calendar does not exist."));
    }

    private static ApiException googleManaged(String message) {
        return ApiException.conflict("google_calendar_managed", message);
    }
}

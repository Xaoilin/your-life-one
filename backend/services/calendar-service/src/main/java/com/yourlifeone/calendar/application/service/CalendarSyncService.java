package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.model.AccountSyncResult;
import com.yourlifeone.calendar.application.model.GoogleCalendar;
import com.yourlifeone.calendar.application.port.GoogleCalendarApi;
import com.yourlifeone.calendar.config.CalendarProperties;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.commonservice.web.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Mirrors a user's Google accounts into the calendar tables. Mirroring is naturally idempotent, so
 * sync runs directly rather than through the write queue; one sync per user runs at a time.
 */
@Service
@RequiredArgsConstructor
public class CalendarSyncService {
    private static final Logger logger = LoggerFactory.getLogger(CalendarSyncService.class);
    private static final Duration LOCK_WAIT = Duration.ofSeconds(20);

    private final AccountService accounts;
    private final GoogleAccessTokens googleTokens;
    private final GoogleCalendarApi google;
    private final CalendarMirror mirror;
    private final CalendarProperties properties;
    private final Clock clock;
    private final CalendarSyncFreshness freshness;
    private final Map<UUID, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    /** @param accountId one account to sync, or null for every Google account */
    public List<AccountSyncResult> sync(UUID userId, String accountId) {
        var requested = freshness.start();
        var lock = userLocks.computeIfAbsent(userId, ignored -> new ReentrantLock());
        if (!tryLock(lock)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "sync_in_progress",
                    "Calendar is already syncing; try again shortly.");
        }
        try {
            var results = new ArrayList<AccountSyncResult>();
            for (var account : accounts.list(userId)) {
                if (account.isGoogle() && (accountId == null || accountId.equals(account.id()))) {
                    var shared = freshness.completedSince(userId, account.id(), requested);
                    if (shared.isPresent()) {
                        results.add(shared.get());
                    } else {
                        var started = freshness.start();
                        var result = syncAccount(userId, account);
                        freshness.complete(userId, started, result);
                        results.add(result);
                    }
                }
            }
            return results;
        } finally {
            lock.unlock();
        }
    }

    AccountSyncResult syncAccount(UUID userId, CalendarAccount account) {
        var result = AccountSyncResult.builder().accountId(account.id()).email(account.email());
        try {
            var accessToken = googleTokens.ensure(userId, account);
            var calendars = google.calendars(accessToken);
            var ownershipProblem = ownershipProblem(account, calendars);
            if (ownershipProblem != null) {
                throw new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, ownershipProblem);
            }
            var syncedAt = clock.instant();
            var from = syncedAt.minus(Duration.ofDays(properties.syncPastDays()));
            var to = syncedAt.plus(Duration.ofDays(properties.syncFutureDays()));
            int eventCount = 0;
            var failures = new ArrayList<String>();
            for (var source : mirror.reconcileCalendars(userId, account, calendars)) {
                try {
                    var events = google.events(accessToken, source.googleCalendarId(), from, to);
                    eventCount += mirror.mirrorEvents(userId, source, events, from, to);
                } catch (GoogleReconnectRequiredException exception) {
                    throw exception;
                } catch (ApiException exception) {
                    freshness.invalidate(userId);
                    // A calendar that failed keeps its stored events untouched until the next sync.
                    logger.warn("Calendar sync skipped one calendar: code={} message={}", exception.code(),
                            exception.getMessage());
                    failures.add(source.name());
                }
            }
            var syncError = failures.isEmpty() ? null
                    : "Could not refresh " + String.join(", ", failures) + "; its events were kept as they were.";
            accounts.recordSync(userId, account.id(), syncedAt, syncError);
            return result.status(syncError == null ? "synced" : "failed").message(syncError)
                    .syncedAt(syncError == null ? syncedAt : account.lastSyncedAt()).eventCount(eventCount).build();
        } catch (GoogleReconnectRequiredException exception) {
            googleTokens.discard(userId, account.email());
            accounts.recordAuthFailure(userId, account.id(), exception.authStatus(), exception.getMessage());
            return result.status(exception.authStatus().value()).message(exception.getMessage())
                    .syncedAt(account.lastSyncedAt()).build();
        } catch (ApiException exception) {
            freshness.invalidate(userId);
            logger.warn("Calendar sync failed: code={} message={}", exception.code(), exception.getMessage());
            return result.status("failed").message(exception.getMessage()).syncedAt(account.lastSyncedAt()).build();
        }
    }

    /** The primary calendar's ID is the Google account's address; another address means another account. */
    private static String ownershipProblem(CalendarAccount account, List<GoogleCalendar> calendars) {
        var primary = calendars.stream().filter(GoogleCalendar::primary).findFirst();
        if (primary.isEmpty()) return null;
        var email = primary.get().id().toLowerCase(Locale.ROOT);
        return email.equals(account.email()) ? null
                : "Google returned " + email + " instead of " + account.email() + ". Reconnect this account.";
    }

    private static boolean tryLock(ReentrantLock lock) {
        try {
            return lock.tryLock(LOCK_WAIT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

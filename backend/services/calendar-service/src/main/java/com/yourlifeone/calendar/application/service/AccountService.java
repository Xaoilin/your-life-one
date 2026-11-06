package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.config.CalendarProperties;
import com.yourlifeone.calendar.domain.AccountProvider;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.calendar.infrastructure.persistence.entity.AccountEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.SourceEntity;
import com.yourlifeone.calendar.infrastructure.persistence.mapper.CalendarPersistenceMapper;
import com.yourlifeone.calendar.infrastructure.persistence.repository.AccountRepository;
import com.yourlifeone.calendar.infrastructure.persistence.repository.SourceRepository;
import com.yourlifeone.commonservice.web.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calendar accounts. Exactly one account is primary whenever the user has any. */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accounts;
    private final SourceRepository sources;
    private final CalendarPersistenceMapper mapper;
    private final CalendarProperties properties;
    private final Clock clock;
    private final CalendarSyncFreshness freshness;

    public List<CalendarAccount> list(UUID userId) {
        return accounts.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream().map(mapper::toAccount).toList();
    }

    public CalendarAccount get(UUID userId, String accountId) {
        return mapper.toAccount(find(userId, accountId));
    }

    /** The account, or empty when it does not exist (for example, already deleted). */
    public Optional<CalendarAccount> lookup(UUID userId, String accountId) {
        return accounts.findByUserIdAndId(userId, accountId).map(mapper::toAccount);
    }

    /** A local account with one calendar, named from configuration. */
    @Transactional
    public CalendarAccount createLocal(UUID userId, String name, String email, Integer paletteIndex) {
        freshness.invalidate(userId);
        var now = clock.instant();
        var account = accounts.save(AccountEntity.builder()
                .userId(userId)
                .id(UUID.randomUUID().toString())
                .provider(AccountProvider.LOCAL)
                .name(name.trim())
                .email(email == null ? "" : email.trim())
                .primary(!accounts.existsByUserIdAndPrimaryTrue(userId))
                .paletteIndex(paletteIndex)
                .authStatus(AuthStatus.CONNECTED)
                .createdAt(now)
                .updatedAt(now)
                .build());
        sources.save(SourceEntity.builder()
                .userId(userId)
                .id(UUID.randomUUID().toString())
                .accountId(account.getId())
                .name(properties.defaultSourceName())
                .color(properties.defaultSourceColor())
                .visible(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return mapper.toAccount(account);
    }

    /** Null fields are left unchanged. Making an account primary makes every other account not primary. */
    @Transactional
    public CalendarAccount update(UUID userId, String accountId, String name, Boolean primary, Integer paletteIndex) {
        freshness.invalidate(userId);
        var account = find(userId, accountId);
        if (name != null) account.setName(name.trim());
        if (paletteIndex != null) account.setPaletteIndex(paletteIndex);
        if (Boolean.TRUE.equals(primary) && !account.isPrimary()) {
            accounts.findByUserIdOrderByCreatedAtAscIdAsc(userId).forEach(other -> other.setPrimary(false));
            account.setPrimary(true);
        }
        account.setUpdatedAt(clock.instant());
        return mapper.toAccount(account);
    }

    /** Deletes the account with its calendars and events; the oldest remaining account becomes primary. */
    @Transactional
    public void delete(UUID userId, String accountId) {
        freshness.invalidate(userId);
        var account = find(userId, accountId);
        accounts.delete(account);
        accounts.flush();
        if (account.isPrimary()) {
            accounts.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream().findFirst()
                    .ifPresent(next -> next.setPrimary(true));
        }
    }

    /** Adds a connected Google account, or marks an existing one connected again. */
    @Transactional
    public CalendarAccount registerGoogle(UUID userId, String email, String name) {
        freshness.invalidate(userId);
        var now = clock.instant();
        var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        var account = accounts.findByUserIdAndProviderAndEmail(userId, AccountProvider.GOOGLE, normalizedEmail)
                .orElseGet(() -> AccountEntity.builder()
                        .userId(userId)
                        .id(UUID.randomUUID().toString())
                        .provider(AccountProvider.GOOGLE)
                        .email(normalizedEmail)
                        .primary(!accounts.existsByUserIdAndPrimaryTrue(userId))
                        .createdAt(now)
                        .build());
        account.setName(name == null || name.isBlank() ? normalizedEmail : name.trim());
        account.setAuthStatus(AuthStatus.CONNECTED);
        account.setAuthError(null);
        account.setUpdatedAt(now);
        return mapper.toAccount(accounts.save(account));
    }

    @Transactional
    public void recordAuthFailure(UUID userId, String accountId, AuthStatus status, String message) {
        freshness.invalidate(userId);
        accounts.findByUserIdAndId(userId, accountId).ifPresent(account -> {
            account.setAuthStatus(status);
            account.setAuthError(message);
            account.setUpdatedAt(clock.instant());
        });
    }

    /** @param syncError null when every calendar synced */
    @Transactional
    public void recordSync(UUID userId, String accountId, Instant syncedAt, String syncError) {
        accounts.findByUserIdAndId(userId, accountId).ifPresent(account -> {
            account.setAuthStatus(AuthStatus.CONNECTED);
            account.setAuthError(null);
            if (syncError == null) account.setLastSyncedAt(syncedAt);
            account.setSyncError(syncError);
            account.setUpdatedAt(clock.instant());
        });
    }

    private AccountEntity find(UUID userId, String accountId) {
        return accounts.findByUserIdAndId(userId, accountId)
                .orElseThrow(() -> ApiException.notFound("account_not_found", "This calendar account does not exist."));
    }
}

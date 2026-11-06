package com.yourlifeone.calendar.infrastructure.persistence.importing;

import com.yourlifeone.calendar.infrastructure.crypto.GoogleCredentialCipher;
import com.yourlifeone.commonservice.persistence.DatabaseUnavailableException;
import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * TEMPORARY: copies the Google refresh tokens the Supabase {@code google-calendar-oauth} function kept
 * in plain text ({@code public.google_calendar_credentials}) into {@code calendar.google_credential},
 * encrypting each one. Flyway cannot encrypt with the service's key, so this runs in Java once per
 * process after the calendar schema is migrated. It is idempotent: revoked rows, unreadable rows and
 * accounts already stored here are skipped, and nothing happens when the table does not exist.
 * Remove it (and its test) once the Supabase table has been dropped.
 */
@Component
public class SupabaseGoogleCredentialImport implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SupabaseGoogleCredentialImport.class);
    private static final String SOURCE_TABLE = "public.google_calendar_credentials";

    private final SchemaMigrator migrator;
    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;
    private final GoogleCredentialCipher cipher;
    private volatile boolean imported;

    public SupabaseGoogleCredentialImport(SchemaMigrator migrator, JdbcClient jdbc,
            PlatformTransactionManager transactionManager, GoogleCredentialCipher cipher) {
        this.migrator = migrator;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.cipher = cipher;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread.ofVirtual().name("google-credential-import").start(this::ensureImported);
    }

    /**
     * Runs the import unless it already completed in this process. A failure is logged and retried on
     * the next call, so it never blocks Google access.
     */
    public void ensureImported() {
        if (imported) return;
        synchronized (this) {
            if (imported) return;
            try {
                migrator.ensureMigrated();
                var count = importCredentials();
                imported = true;
                if (count > 0) logger.info("Imported {} Google credentials from Supabase", count);
            } catch (DatabaseUnavailableException | DataAccessException | TransactionException exception) {
                logger.warn("Google credential import deferred: {}", exception.getClass().getSimpleName());
            }
        }
    }

    /** Imports every credential not yet stored, in one transaction. @return how many were imported */
    public int importCredentials() {
        var count = transaction.execute(status -> sourceTableExists() ? copy(pending()) : 0);
        return count == null ? 0 : count;
    }

    private boolean sourceTableExists() {
        return Boolean.TRUE.equals(jdbc.sql("SELECT to_regclass(?) IS NOT NULL").param(SOURCE_TABLE)
                .query(Boolean.class).single());
    }

    /** Unrevoked rows that fit the table's bounds and are not stored here yet. */
    private List<SupabaseGoogleCredential> pending() {
        return jdbc.sql("""
                        SELECT c.user_id, n.email, c.refresh_token,
                               left(c.scope, 2000) AS scope,
                               c.last_refresh_at,
                               nullif(left(c.last_refresh_failure_reason, 500), '') AS failure_reason,
                               CASE WHEN nullif(c.last_refresh_failure_reason, '') IS NULL THEN NULL
                                    ELSE coalesce(c.last_refresh_failure_at, now()) END AS failure_at,
                               coalesce(c.created_at, now()) AS created_at
                        FROM public.google_calendar_credentials c
                        CROSS JOIN LATERAL (SELECT lower(trim(c.google_email)) AS email) n
                        WHERE c.revoked_at IS NULL
                          AND octet_length(c.refresh_token) BETWEEN 1 AND 4096
                          AND length(n.email) BETWEEN 3 AND 320
                          AND n.email ~ '^[^@[:space:]]+@[^@[:space:]]+$'
                          AND NOT EXISTS (SELECT 1 FROM calendar.google_credential g
                                          WHERE g.user_id = c.user_id AND g.email = n.email)""")
                .query((rs, row) -> SupabaseGoogleCredential.builder()
                        .userId(rs.getObject("user_id", UUID.class))
                        .email(rs.getString("email"))
                        .refreshToken(rs.getString("refresh_token"))
                        .scope(rs.getString("scope"))
                        .lastRefreshAt(rs.getObject("last_refresh_at", OffsetDateTime.class))
                        .failureReason(rs.getString("failure_reason"))
                        .failureAt(rs.getObject("failure_at", OffsetDateTime.class))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build())
                .list();
    }

    private int copy(List<SupabaseGoogleCredential> credentials) {
        int count = 0;
        for (var credential : credentials) {
            var encrypted = cipher.encrypt(credential.refreshToken(), credential.userId(), credential.email());
            count += jdbc.sql("""
                            INSERT INTO calendar.google_credential (user_id, email, refresh_token_ciphertext,
                                refresh_token_nonce, key_version, scope, last_refresh_at, last_failure_reason,
                                last_failure_at, created_at, updated_at)
                            VALUES (:userId, :email, :ciphertext, :nonce, :keyVersion, CAST(:scope AS text),
                                    CAST(:lastRefreshAt AS timestamptz), CAST(:failureReason AS text),
                                    CAST(:failureAt AS timestamptz), :createdAt, now())
                            ON CONFLICT (user_id, email) DO NOTHING""")
                    .param("userId", credential.userId())
                    .param("email", credential.email())
                    .param("ciphertext", encrypted.ciphertext())
                    .param("nonce", encrypted.nonce())
                    .param("keyVersion", encrypted.keyVersion())
                    .param("scope", credential.scope())
                    .param("lastRefreshAt", credential.lastRefreshAt())
                    .param("failureReason", credential.failureReason())
                    .param("failureAt", credential.failureAt())
                    .param("createdAt", credential.createdAt())
                    .update();
        }
        return count;
    }
}

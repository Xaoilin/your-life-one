package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.application.model.AccountSyncResult;
import com.yourlifeone.calendar.application.model.EventDetails;
import com.yourlifeone.calendar.application.model.GoogleTokenGrant;
import com.yourlifeone.calendar.application.port.GoogleCalendarApi;
import com.yourlifeone.calendar.application.port.GoogleOAuthApi;
import com.yourlifeone.calendar.application.service.AccountService;
import com.yourlifeone.calendar.application.service.CalendarSyncFreshness;
import com.yourlifeone.calendar.application.service.EventService;
import com.yourlifeone.calendar.application.service.GoogleAccessTokens;
import com.yourlifeone.calendar.application.service.GoogleCredentialStore;
import com.yourlifeone.calendar.application.service.SourceService;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.domain.EventTiming;
import com.yourlifeone.calendar.infrastructure.persistence.model.EventComparisonValue;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real PostgreSQL proves scope, exact comparison, mutation invalidation and the measured query plans. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class EventComparisonsIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private AccountService accounts;
    @Autowired private SourceService sources;
    @Autowired private EventService events;
    @Autowired private EventRepository repository;
    @Autowired private CalendarSyncFreshness freshness;
    @Autowired private GoogleCredentialStore credentials;
    @Autowired private GoogleAccessTokens tokens;
    @Autowired private JsonMapper json;
    @Autowired private SchemaMigrator migrator;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private GoogleOAuthApi oauth;
    @MockitoBean private GoogleCalendarApi google;

    private final UUID user = UUID.randomUUID();
    private final List<UUID> fixtureUsers = new ArrayList<>(List.of(user));

    @BeforeEach
    void migrate() {
        migrator.ensureMigrated();
    }

    @AfterEach
    void removeOwnedFixtures() {
        var jdbc = JdbcClient.create(dataSource);
        for (var owner : fixtureUsers) {
            // Service calls commit independently; account deletion cascades only this fixture's sources/events.
            jdbc.sql("DELETE FROM calendar.account WHERE user_id = ?").param(owner).update();
            jdbc.sql("DELETE FROM calendar.google_credential WHERE user_id = ?").param(owner).update();
            freshness.invalidate(owner);
            assertEquals(0, jdbc.sql("""
                    SELECT (SELECT count(*) FROM calendar.account WHERE user_id = :owner)
                         + (SELECT count(*) FROM calendar.source WHERE user_id = :owner)
                         + (SELECT count(*) FROM calendar.event WHERE user_id = :owner)
                         + (SELECT count(*) FROM calendar.google_credential WHERE user_id = :owner)
                    """).param("owner", owner).query(Long.class).single());
        }
    }

    @Test
    void mutationsAndCredentialFailuresInvalidateCompletedSyncs() {
        var account = accounts.createLocal(user, "Invented", "", null);
        var source = sources.list(user).getFirst();
        assertInvalidates(() -> accounts.update(user, account.id(), "Renamed", null, null));
        assertInvalidates(() -> sources.update(user, source.id(), null, "#123456", false, null));
        assertInvalidates(() -> sources.createLocal(user, account.id(), "Other", "#123456", true));
        var details = EventDetails.builder().sourceId(source.id()).title("Invented")
                .timing(EventTiming.parse(true, "2026-10-20", "2026-10-20")).build();
        assertInvalidates(() -> events.create(user, details));
        var date = LocalDate.parse("2026-10-20");
        var event = events.list(user, date, date).getFirst();
        assertInvalidates(() -> events.update(user, event.id(), details));
        assertInvalidates(() -> events.delete(user, event.id()));
        assertInvalidates(() -> sources.delete(user, source.id()));
        assertInvalidates(() -> accounts.registerGoogle(user, "fixture@example.test", "Fixture"));
        assertInvalidates(() -> accounts.recordAuthFailure(user, account.id(), AuthStatus.NEEDS_RECONNECT, "fixture"));
        assertInvalidates(() -> credentials.save(user, "fixture@example.test", GoogleTokenGrant.builder()
                .refreshToken("invented-test-token").build()));
        assertInvalidates(() -> credentials.recordFailure(user, "fixture@example.test", AuthStatus.REVOKED, "fixture"));
        assertInvalidates(() -> credentials.delete(user, "fixture@example.test"));
        assertInvalidates(() -> tokens.discard(user, "fixture@example.test"));
        assertInvalidates(() -> accounts.delete(user, account.id()));
    }

    private void assertInvalidates(Runnable mutation) {
        var requested = freshness.start();
        freshness.complete(user, freshness.start(), AccountSyncResult.builder().accountId("fixture")
                .status("synced").build());
        assertTrue(freshness.completedSince(user, "fixture", requested).isPresent());
        mutation.run();
        assertTrue(freshness.completedSince(user, "fixture", requested).isEmpty());
    }

    @Test
    void comparisonIsScopedAndPreservesOutsideEventsAndMovedIdentity() {
        var jdbc = JdbcClient.create(dataSource);
        var account = accounts.createLocal(user, "Invented plan", "", null);
        var source = sources.list(user).getFirst();
        var otherSource = sources.createLocal(user, account.id(), "Excluded", "#123456", true);
        seed(jdbc, user, source.id(), 1483);
        seed(jdbc, user, otherSource.id(), 1483);
        var otherUser = UUID.randomUUID();
        fixtureUsers.add(otherUser);
        accounts.createLocal(otherUser, "Excluded user", "", null);
        seed(jdbc, otherUser, sources.list(otherUser).getFirst().id(), 1483);
        var fetched = new ArrayList<EventComparisonValue>();
        for (int index = 1; index <= 1483; index++) {
            fetched.add(EventComparisonValue.builder().id("g" + index).title("Invented " + index)
                    .description("x".repeat(1024)).allDay(false)
                    .startsAt("2026-10-20T10:00:00Z").endsAt("2026-10-20T11:00:00Z").build());
        }
        var from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        var to = OffsetDateTime.parse("2027-08-01T00:00:00Z");
        assertTrue(repository.differences(user, source.id(), fetched, from.toInstant(), to.toInstant()).isEmpty());
        capture(jdbc, "unchanged-1483", fetched, source.id(), from, to);
        jdbc.sql("UPDATE calendar.event SET starts_at = '2025-01-01', ends_at = '2025-01-02' "
                        + "WHERE user_id = ? AND source_id = ? AND google_event_id = 'g1'")
                .param(user).param(source.id()).update();
        jdbc.sql("UPDATE calendar.event SET starts_at = ?, ends_at = ? "
                        + "WHERE user_id = ? AND source_id = ? AND google_event_id = 'g2'")
                .param(to).param(to).param(user).param(source.id()).update();
        jdbc.sql("UPDATE calendar.event SET starts_at = ?, ends_at = ? "
                        + "WHERE user_id = ? AND source_id = ? AND google_event_id = 'g3'")
                .param(from).param(from).param(user).param(source.id()).update();
        fetched.remove(2);
        fetched.remove(1);
        var differences = repository.differences(user, source.id(), fetched, from.toInstant(), to.toInstant());
        assertEquals(2, differences.size());
        assertTrue(differences.stream().anyMatch(diff -> "g1".equals(diff.googleId())
                && (source.id() + "-g1").equals(diff.storedId())));
        assertTrue(differences.stream().anyMatch(diff -> diff.googleId() == null
                && (source.id() + "-g3").equals(diff.storedId())));
        capture(jdbc, "moved-and-boundaries", fetched, source.id(), from, to);
        repository.findByUserIdAndSourceIdAndIdIn(user, source.id(), List.of(source.id() + "-g1"));
        captureBodyAndDelete(jdbc, source.id(), source.id() + "-g1");
        assertEquals(1483, jdbc.sql("SELECT count(*) FROM calendar.event WHERE user_id = ? AND source_id = ?")
                .param(otherUser).param(sources.list(otherUser).getFirst().id()).query(Integer.class).single());
        assertEquals(1483, jdbc.sql("SELECT count(*) FROM calendar.event WHERE user_id = ? AND source_id = ?")
                .param(user).param(otherSource.id()).query(Integer.class).single());
    }

    private void seed(JdbcClient jdbc, UUID owner, String sourceId, int count) {
        jdbc.sql("""
                INSERT INTO calendar.event (user_id, id, source_id, google_event_id, title, description,
                    all_day, starts_at, ends_at)
                SELECT :owner, :source || '-g' || n, :source, 'g' || n, 'Invented ' || n, repeat('x', 1024),
                    false, '2026-10-20T10:00:00Z'::timestamptz, '2026-10-20T11:00:00Z'::timestamptz
                FROM generate_series(1, :count) n
                """).param("owner", owner).param("source", sourceId).param("count", count).update();
    }

    private void capture(JdbcClient jdbc, String label, List<EventComparisonValue> fetched, String source,
            OffsetDateTime from, OffsetDateTime to) {
        if (!Boolean.getBoolean("kan330.evidence")) return;
        captureSql(jdbc, label, EventComparisonsImpl.DIFFERENCES, Map.of("userId", user, "sourceId", source,
                "fetched", json.writeValueAsString(fetched), "from", from, "to", to));
        System.out.println("KAN-330 INDEXES " + json.writeValueAsString(jdbc.sql(
                        "SELECT indexdef FROM pg_indexes WHERE schemaname = 'calendar' "
                                + "AND tablename IN ('event','source','account') ORDER BY indexname")
                .query(String.class).list()));
    }

    private void captureBodyAndDelete(JdbcClient jdbc, String source, String id) {
        if (!Boolean.getBoolean("kan330.evidence")) return;
        jdbc.sql("CREATE EXTENSION IF NOT EXISTS pg_stat_statements").update();
        var transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            repository.deleteMirrored(user, source, List.of(id));
            status.setRollbackOnly();
        });
        var queries = jdbc.sql("""
                SELECT query FROM pg_stat_statements WHERE query NOT ILIKE '%pg_stat_statements%'
                  AND query ~* '^(select|delete).*calendar[.]event.*source_id.*[.]id in'
                """).query(String.class).list();
        assertEquals(2, queries.size());
        for (var sql : queries) {
            var named = sql.replace("$1", ":userId").replace("$2", ":sourceId").replace("$3", ":id");
            captureSql(jdbc, sql.startsWith("delete") ? "scoped-delete" : "changed-body", named,
                    Map.of("userId", user, "sourceId", source, "id", id));
        }
    }

    private void captureSql(JdbcClient jdbc, String label, String sql, Map<String, ?> parameters) {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setTimeout(5);
        var plan = transaction.execute(status -> {
            var statement = jdbc.sql("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + sql);
            parameters.forEach(statement::param);
            status.setRollbackOnly();
            return statement.query(String.class).single();
        });
        System.out.println("KAN-330 PLAN " + label + " " + plan.replace("\n", ""));
        System.out.println("KAN-330 QUERY " + label + " " + sql);
    }
}

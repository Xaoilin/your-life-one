package com.yourlifeone.knowledge.infrastructure.persistence;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one-time imports of the React app's knowledge, lifestyle and project records
 * ({@code public.yourlifeone_records}), each run against its own schema so they cannot disturb the other
 * tests. They use their own connections: the service's two-connection pool may be busy migrating.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ReactAppImportTest {
    private static final UUID USER = UUID.randomUUID();

    @Autowired
    private DataSource dataSource;

    private DriverManagerDataSource direct() {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        return new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
    }

    private JdbcClient jdbc() {
        return JdbcClient.create(direct());
    }

    private void migrate(String domain, String schema) {
        Flyway.configure().dataSource(direct()).schemas(schema).createSchemas(true)
                .locations("classpath:db/migration/" + domain).load().migrate();
    }

    private void insert(JdbcClient jdbc, String collection, String id, String payload, boolean deleted) {
        jdbc.sql("""
                CREATE TABLE IF NOT EXISTS public.yourlifeone_records (user_id uuid, collection text, record_id text,
                    payload jsonb, deleted_at timestamptz, created_at timestamptz DEFAULT now(),
                    updated_at timestamptz DEFAULT now())""").update();
        jdbc.sql("INSERT INTO public.yourlifeone_records (user_id, collection, record_id, payload, deleted_at)"
                        + " VALUES (?, ?, ?, ?::jsonb, " + (deleted ? "now()" : "NULL") + ")")
                .params(USER, collection, id, payload).update();
    }

    private static int count(JdbcClient jdbc, String table) {
        return jdbc.sql("SELECT count(*) FROM " + table + " WHERE user_id = ?").param(USER).query(Integer.class).single();
    }

    @Test
    void importsTopicsAndTheEntriesWhoseTopicExists() {
        var jdbc = jdbc();
        insert(jdbc, "knowledgeTopics", "t1", """
                {"id":"t1","name":"Prayer","description":"Salah","icon":"🕌","color":"#22c55e","sortOrder":2,
                 "createdAt":"2026-04-01T10:00:00.000Z","updatedAt":"2026-04-02T10:00:00.000Z"}""", false);
        insert(jdbc, "knowledgeEntries", "e1", """
                {"id":"e1","topicId":"t1","title":"On time","content":"Pray on time",
                 "sources":[{"type":"quran","surah":4,"ayahStart":103},"bad"],"tags":["salah","salah"]}""", false);
        insert(jdbc, "knowledgeEntries", "orphan", "{\"topicId\":\"gone\",\"title\":\"Lost\"}", false);
        insert(jdbc, "knowledgeEntries", "deleted", "{\"topicId\":\"t1\",\"title\":\"Deleted\"}", true);

        migrate("knowledge", "knowledge_import_test");

        assertEquals(1, count(jdbc, "knowledge_import_test.topic"));
        assertEquals(1, count(jdbc, "knowledge_import_test.entry"));
        assertEquals("2|1|{salah}", jdbc.sql("SELECT t.sort_order || '|' || jsonb_array_length(e.sources) || '|' || e.tags::text"
                + " FROM knowledge_import_test.topic t JOIN knowledge_import_test.entry e ON e.topic_id = t.id")
                .query(String.class).single());
    }

    @Test
    void importsLifestyleItemsMergingTheOlderSingleSource() {
        var jdbc = jdbc();
        insert(jdbc, "lifestyleItems", "l1", """
                {"id":"l1","type":"haram","title":"Backbiting","notes":"","status":"avoiding","sortOrder":3,
                 "source":"https://quran.com/49/12","sources":["https://quran.com/4/36"]}""", false);
        insert(jdbc, "lifestyleItems", "l2", "{\"type\":\"halal\",\"title\":\"Charity\",\"status\":\"bogus\"}", false);

        migrate("lifestyle", "lifestyle_import_test");

        assertEquals(List.of("l1|3|{https://quran.com/49/12,https://quran.com/4/36}|avoiding", "l2|0|{}|want-to-start"),
                jdbc.sql("SELECT id || '|' || sort_order || '|' || sources::text || '|' || status"
                        + " FROM lifestyle_import_test.item ORDER BY id").query(String.class).list());
    }

    @Test
    void importsProjectsAndTheirPagesKeepingOneOverviewEach() {
        var jdbc = jdbc();
        insert(jdbc, "projects", "p1", """
                {"id":"p1","catalogKey":"catalog:agentboard","name":"Agent Board","kind":"web_app","status":"archived",
                 "statusBeforeArchive":"active","isPinned":true,"sortOrder":4,"tags":["desk"],
                 "links":[{"id":"l","kind":"repository","label":"Repo","url":"https://github.com/x/y"}],
                 "setupSteps":[],"runRecipes":[],"preview":{"icon":"AB","accentColor":"#4f9cff","backgroundColor":"#112033"},
                 "summary":"Boards","verifiedAt":"2026-09-01T00:00:00Z"}""", false);
        insert(jdbc, "projects", "p2", "{\"id\":\"p2\",\"name\":\"Plain\"}", false);
        insert(jdbc, "projectPages", "o1", """
                {"projectId":"p1","title":"Overview","content":"# Agent Board","isOverview":true,
                 "createdAt":"2026-09-01T00:00:00Z"}""", false);
        insert(jdbc, "projectPages", "o2", """
                {"projectId":"p1","title":"Second","content":"","isOverview":true,"createdAt":"2026-09-02T00:00:00Z"}""", false);
        insert(jdbc, "projectPages", "lost", "{\"projectId\":\"gone\",\"title\":\"Lost\"}", false);

        migrate("projects", "projects_import_test");

        assertEquals(List.of("p1|catalog:agentboard|archived|active|false|4", "p2|custom:p2|active||false|0"),
                jdbc.sql("SELECT id || '|' || catalog_key || '|' || status || '|' || coalesce(status_before_archive, '')"
                        + " || '|' || is_pinned || '|' || sort_order FROM projects_import_test.project ORDER BY id")
                        .query(String.class).list());
        assertEquals(List.of("o1|true", "o2|false"), jdbc.sql("SELECT id || '|' || is_overview"
                + " FROM projects_import_test.page ORDER BY id").query(String.class).list());
    }
}

package com.yourlifeone.life.infrastructure.persistence;

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
 * The one-time imports of the React app's life-admin records ({@code public.yourlifeone_records}), each run
 * against its own schema so they cannot disturb the other tests. They use their own connections: the
 * service's two-connection pool may be busy migrating in the background.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ReactAppImportTest {
    private static final UUID USER = UUID.randomUUID();

    @Autowired
    private DataSource dataSource;

    private JdbcClient jdbc() {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        return JdbcClient.create(new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword()));
    }

    private void migrate(String domain, String schema) {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        var direct = new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
        Flyway.configure().dataSource(direct).schemas(schema).createSchemas(true)
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
    void importsTheFastFoodJournalKeepingEveryLiveEntry() {
        var jdbc = jdbc();
        insert(jdbc, "healthFastFoodEntries", "e1", """
                {"id":"e1","date":"2026-04-20","notes":"Not worth it","order":"Big Arch meal","venue":"McDonalds",
                 "rating":"bad","symptoms":["nauseous","sluggish","unknown"],"createdAt":"2026-04-21T16:14:43.639Z"}""", false);
        insert(jdbc, "healthFastFoodEntries", "e2", "{\"venue\":\" \",\"rating\":\"great\",\"date\":\"soon\"}", false);
        insert(jdbc, "healthFastFoodEntries", "gone", "{\"venue\":\"KFC\",\"rating\":\"bad\"}", true);

        migrate("health", "health_import_test");

        assertEquals(2, count(jdbc, "health_import_test.fast_food_entry"));
        assertEquals("McDonalds|bad|{nauseous,sluggish}", jdbc.sql("SELECT venue || '|' || rating || '|' || symptoms::text"
                + " FROM health_import_test.fast_food_entry WHERE id = 'e1'").query(String.class).single());
        assertEquals("Unknown venue|mixed", jdbc.sql("SELECT venue || '|' || rating FROM "
                + "health_import_test.fast_food_entry WHERE id = 'e2'").query(String.class).single());
    }

    @Test
    void importsEveryJobApplicationAndItsHistoryInOrder() {
        var jdbc = jdbc();
        insert(jdbc, "employment", "singleton", """
                {"seedVersion":1,"applications":[
                  {"id":"job-b","company":"Beta","role":"Engineer","status":"offer","workType":"contract",
                   "remoteRegion":"unknown","remoteStatus":"confirmed","remoteEvidence":"","nextAction":"Reply",
                   "notes":"","url":"https://mail.google.com/mail/#all/1","createdAt":"2026-09-14T18:30:38.581553+00:00",
                   "updatedAt":"2026-09-14T18:30:38.581553+00:00","history":[
                     {"id":"h1","kind":"contact","summary":"Recruiter","details":"","evidenceUrl":"https://mail.example/1"},
                     {"id":"h2","kind":"offer","summary":"Offer","details":"","evidenceUrl":"https://mail.example/1"},
                     {"kind":"note","summary":"No id","details":""}]},
                  {"id":"job-a","company":"Alpha","role":"Staff","status":"applied","applicationDate":"2026-08-29",
                   "workType":"unknown","remoteRegion":"uk","remoteStatus":"needs_verification",
                   "remoteEvidence":"Listed as UK remote","nextAction":"Wait","notes":"","history":[]}]}""", false);

        migrate("jobs", "jobs_import_test");

        assertEquals(List.of("job-b", "job-a"), jdbc.sql("SELECT id FROM jobs_import_test.application WHERE user_id = ?"
                + " ORDER BY position").param(USER).query(String.class).list());
        // Confirmed remote work for an unknown region cannot be shown as confirmed.
        assertEquals("needs_verification", jdbc.sql("SELECT remote_status FROM jobs_import_test.application"
                + " WHERE id = 'job-b'").query(String.class).single());
        assertEquals("2026-09-14 18:30:38.581+00", jdbc.sql("SELECT to_char(updated_at AT TIME ZONE 'UTC', "
                + "'YYYY-MM-DD HH24:MI:SS.MS') || '+00' FROM jobs_import_test.application WHERE id = 'job-b'")
                .query(String.class).single());
        // The repeated evidence link is kept once; the entry without an ID keeps its place.
        assertEquals(List.of("h1:contact", "history-3:note"), jdbc.sql("SELECT id || ':' || kind FROM "
                + "jobs_import_test.history_entry WHERE application_id = 'job-b' ORDER BY position")
                .query(String.class).list());
    }

    @Test
    void importsItemsAndNeedsWithDimensionsAndSpecifications() {
        var jdbc = jdbc();
        insert(jdbc, "inventoryItems", "rail", """
                {"id":"rail","name":"Rail slides","category":"component","subcategory":"mechanical_hardware",
                 "trackingMode":"counted","quantity":2,"unit":"pairs","dimensions":{"unit":"mm","length":550},
                 "specifications":{"asin":"B07Z4MTG1X","mount":"Side mount"},"condition":"new","tags":["desk"],
                 "notes":"","projectCatalogKeys":["magnus"],"imageUrl":"https://m.media-amazon.com/x.jpg",
                 "lastVerifiedAt":"2026-08-03T10:00:00.000Z","createdAt":"2026-08-03T10:00:00.000Z",
                 "updatedAt":"2026-08-03T11:25:35.960925+00:00"}""", false);
        insert(jdbc, "inventoryNeeds", "jacks", """
                {"id":"jacks","name":"Arm jacks","unit":"pcs","status":"ordered","category":"tool",
                 "subcategory":"workshop_equipment","priority":"normal","requiredQuantity":2,
                 "specifications":{"packCount":"2"},"notes":"","orderedAt":"2026-08-03T10:30:32.328Z"}""", false);

        migrate("inventory", "inventory_import_test");

        assertEquals("550|mm|2", jdbc.sql("SELECT dimension_length::int || '|' || dimension_unit || '|' || quantity::int"
                + " FROM inventory_import_test.item WHERE id = 'rail'").query(String.class).single());
        assertEquals(2, count(jdbc, "inventory_import_test.item_specification"));
        assertEquals("ordered|1", jdbc.sql("SELECT n.status || '|' || count(s.*) FROM inventory_import_test.need n"
                + " JOIN inventory_import_test.need_specification s ON s.need_id = n.id GROUP BY n.status")
                .query(String.class).single());
    }

    @Test
    void importsTripsWithTheirPartsAndSkipsPartsOfMissingTrips() {
        var jdbc = jdbc();
        insert(jdbc, "trips", "saudi", """
                {"id":"saudi","name":"Trip To Saudi","notes":"","status":"planning","endDate":"2026-07-09",
                 "summary":"","startDate":"2026-06-17","budgetTotal":0,"budgetCurrency":"GBP"}""", false);
        insert(jdbc, "tripLegs", "riyadh", """
                {"id":"riyadh","city":"Riyadh","tripId":"saudi","country":"Saudi Arabia","endDate":"2026-07-09",
                 "sortOrder":0,"startDate":"2026-06-17"}""", false);
        insert(jdbc, "tripBookings", "flight", """
                {"id":"flight","kind":"transport","mode":"flight","legId":"riyadh","title":"Flight","tripId":"saudi",
                 "toLabel":"Bahrain","arriveAt":"2026-07-02T11:00","departAt":"2026-07-02T09:00","fromLabel":"Riyadh",
                 "budgetDate":"2026-07-02","budgetAmount":20000,"budgetStatus":"planned"}""", false);
        insert(jdbc, "tripBookings", "flat", """
                {"id":"flat","city":"Riyadh","kind":"stay","legId":"riyadh","title":"Flat","tripId":"saudi",
                 "country":"Saudi Arabia","checkInDate":"2026-06-17","checkOutDate":"2026-07-02","budgetAmount":100000,
                 "propertyName":"2 bedroom flat"}""", false);
        insert(jdbc, "tripBudgetEntries", "cost", """
                {"id":"cost","date":"2026-06-17","title":"General","amount":50000,"status":"planned",
                 "tripId":"saudi","category":"other"}""", false);
        insert(jdbc, "tripBudgetEntries", "orphan", """
                {"id":"orphan","title":"Other trip","amount":1,"status":"planned","tripId":"missing","category":"other"}""",
                false);

        migrate("trips", "trips_import_test");

        assertEquals(1, count(jdbc, "trips_import_test.trip"));
        assertEquals(1, count(jdbc, "trips_import_test.leg"));
        assertEquals(2, count(jdbc, "trips_import_test.booking"));
        assertEquals(1, count(jdbc, "trips_import_test.budget_entry"));
        assertEquals("2026-07-02T09:00", jdbc.sql("SELECT depart_at FROM trips_import_test.booking WHERE id = 'flight'")
                .query(String.class).single());
    }
}

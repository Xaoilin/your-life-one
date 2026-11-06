package com.yourlifeone.life.search;

import com.yourlifeone.commonservice.testing.PostgresQueryCounts;
import com.yourlifeone.commonservice.testing.PostgresPlanEvidence;
import com.yourlifeone.life.inventory.infrastructure.persistence.repository.InventoryItemRepository;
import com.yourlifeone.life.inventory.infrastructure.persistence.repository.InventoryNeedRepository;
import com.yourlifeone.life.jobs.infrastructure.persistence.repository.JobApplicationRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.jpa.repository.Query;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.life.infrastructure.persistence.LifeSchemas;
import com.yourlifeone.life.inventory.application.service.InventoryQueries;
import com.yourlifeone.life.inventory.domain.InventorySearch;
import com.yourlifeone.life.jobs.application.service.JobApplicationQueries;
import com.yourlifeone.life.jobs.domain.ApplicationSearch;
import com.yourlifeone.life.inventory.domain.InventoryItem;
import com.yourlifeone.life.inventory.domain.InventoryNeed;
import com.yourlifeone.life.inventory.domain.InventorySnapshot;
import com.yourlifeone.life.jobs.domain.ApplicationPage;
import com.yourlifeone.life.jobs.domain.HistoryEntry;
import com.yourlifeone.life.jobs.domain.JobApplication;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/** Real PostgreSQL counters prove that limit one bounds transfer, including children, as data grows. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class BoundedSearchIntegrationTest {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private LifeSchemas schemas;
    @Autowired
    private InventoryQueries inventory;
    @Autowired
    private JobApplicationQueries jobs;

    private JdbcClient jdbc;
    private PostgresQueryCounts counts;

    @BeforeEach
    void setUp() {
        schemas.ensureMigrated();
        jdbc = JdbcClient.create(dataSource);
        counts = new PostgresQueryCounts(dataSource);
    }

    @Test
    void inventoryLimitOneLoadsOnlySelectedParentsAndTheirSpecifications() {
        var user = UUID.randomUUID();
        seedInventory(user, 1, 40);
        seedInventory(UUID.randomUUID(), 1, 40);
        assertInventoryBound(user);
        seedInventory(user, 41, 340);
        assertInventoryBound(user);
    }

    private void assertInventoryBound(UUID user) {
        counts.reset();
        var result = inventory.search(user, InventorySearch.builder().query("child-only%").limit(1).build());
        assertThat(result.items()).extracting("id").containsExactly("item1");
        assertThat(result.needs()).extracting("id").containsExactly("need1");
        assertThat(result.items().getFirst().specifications()).hasSize(5);
        assertThat(result.needs().getFirst().specifications()).hasSize(5);
        assertBound("inventory", 4, 12);
        fetchPlans(user);
        var parameters = parameters(user);
        plan("item", InventoryItemRepository.class, "searchIds", parameters);
        parameters.remove("category");
        parameters.remove("location");
        plan("need", InventoryNeedRepository.class, "searchIds", parameters);
    }

    @Test
    void employmentLimitOneKeepsExactTotalWithoutLoadingUnselectedHistory() {
        var user = UUID.randomUUID();
        seedJobs(user, 1, 64);
        seedJobs(UUID.randomUUID(), 1, 64);
        assertJobsBound(user, 64);
        seedJobs(user, 65, 364);
        assertJobsBound(user, 364);
    }

    private void assertJobsBound(UUID user, int total) {
        counts.reset();
        var result = jobs.search(user, ApplicationSearch.builder().query("child-only%").limit(1).offset(1).build());
        assertThat(result.applications()).extracting("id").containsExactly("job2");
        assertThat(result.total()).isEqualTo(total);
        assertThat(result.applications().getFirst().history()).hasSize(4);
        assertBound("jobs", 3, 6);
        fetchPlans(user);
        var parameters = parameters(user);
        parameters.remove("project");
        parameters.remove("category");
        parameters.remove("location");
        parameters.put("status", null);
        parameters.put("workType", null);
        parameters.put("remoteStatus", null);
        parameters.put("offset", 1);
        plan("jobs-ids", JobApplicationRepository.class, "searchIds", parameters);
        parameters.remove("limit");
        parameters.remove("offset");
        plan("jobs-count", JobApplicationRepository.class, "countMatches", parameters);
    }

    private void assertBound(String domain, long calls, long rows) {
        var executed = counts.selects().entrySet().stream().filter(entry -> !entry.getKey().contains("pg_catalog."))
                .mapToLong(entry -> entry.getValue()).sum();
        var transferred = jdbc.sql("SELECT coalesce(sum(rows), 0) FROM pg_stat_statements "
                        + "WHERE query ~* '^select' AND query NOT ILIKE '%pg_stat_statements%' AND query NOT ILIKE '%pg_catalog.%'")
                .query(Long.class).single();
        System.out.println("KAN-328 " + domain + " selects=" + executed + " returned_rows=" + transferred);
        counts.selects().forEach((query, count) -> System.out.println("KAN-328 SQL calls=" + count + " " + query));
        assertThat(executed).isLessThanOrEqualTo(calls);
        assertThat(transferred).isLessThanOrEqualTo(rows);
    }


    private void fetchPlans(UUID user) {
        if (!Boolean.getBoolean("kan328.plans")) return;
        counts.selects().keySet().stream().filter(sql -> sql.contains(" left join ")).forEach(sql -> {
            var id = sql.contains("inventory.item ") ? "item1" : sql.contains("inventory.need ") ? "need1" : "job2";
            PostgresPlanEvidence.capture(jdbc, "selected-" + id,
                    sql.replace("$1", ":userId").replace("$2", ":id"), Map.of("userId", user, "id", id));
        });
    }

    private Map<String, Object> parameters(UUID user) {
        var parameters = new HashMap<String, Object>();
        parameters.put("userId", user);
        parameters.put("needle", "child-only%");
        parameters.put("project", null);
        parameters.put("category", null);
        parameters.put("location", null);
        parameters.put("limit", 1);
        return parameters;
    }

    private void plan(String label, Class<?> repository, String method, Map<String, Object> parameters) {
        if (!Boolean.getBoolean("kan328.plans")) return;
        jdbc.sql("ANALYZE inventory.item,inventory.need,inventory.item_specification,inventory.need_specification, "
                + "jobs.application,jobs.history_entry").update();
        PostgresPlanEvidence.catalogue(jdbc);
        var query = Arrays.stream(repository.getMethods()).filter(candidate -> candidate.getName().equals(method))
                .findFirst().orElseThrow().getAnnotation(Query.class).value();
        PostgresPlanEvidence.capture(jdbc, label, query, parameters);
        var table = label.equals("item") ? "inventory.item" : label.equals("need") ? "inventory.need" : "jobs.application";
        PostgresPlanEvidence.capture(jdbc, label + "-old-parents", "SELECT * FROM " + table
                + " WHERE user_id=:userId ORDER BY " + (table.startsWith("jobs") ? "position" : "name"),
                Map.of("userId", parameters.get("userId")));
    }

    private void seedInventory(UUID user, int first, int last) {
        jdbc.sql("""
                INSERT INTO inventory.item (user_id,id,name,category,tracking_mode,quantity,unit,condition,
                    last_verified_at,created_at,updated_at)
                SELECT ?, 'item'||n, 'Item '||lpad(n::text,4,'0'), 'component','counted',2,'pcs','new',now(),now(),now()
                FROM generate_series(?,?) n
                """).params(user, first, last).update();
        jdbc.sql("""
                INSERT INTO inventory.need (user_id,id,name,required_quantity,unit,priority,status,created_at,updated_at)
                SELECT ?, 'need'||n, 'Need '||lpad(n::text,4,'0'),1,'pcs','high','needed',now(),now()
                FROM generate_series(?,?) n
                """).params(user, first, last).update();
        jdbc.sql("""
                INSERT INTO inventory.item_specification
                SELECT ?, 'item'||n, s, 'spec'||s, 'child-only% '||s
                FROM generate_series(?,?) n CROSS JOIN generate_series(0,4) s
                """).params(user, first, last).update();
        jdbc.sql("""
                INSERT INTO inventory.need_specification
                SELECT ?, 'need'||n, s, 'spec'||s, 'child-only% '||s
                FROM generate_series(?,?) n CROSS JOIN generate_series(0,4) s
                """).params(user, first, last).update();
    }

    private void seedJobs(UUID user, int first, int last) {
        jdbc.sql("""
                INSERT INTO jobs.application (user_id,id,position,company,role,work_type,remote_region,
                    remote_status,remote_evidence,status,next_action,created_at,updated_at)
                SELECT ?, 'job'||n,n,'Company '||n,'Engineer','permanent','uk','confirmed',
                    'Evidence','applied','Follow up',now(),now() FROM generate_series(?,?) n
                """).params(user, first, last).update();
        jdbc.sql("""
                INSERT INTO jobs.history_entry (user_id,application_id,position,id,kind,summary,details)
                SELECT ?, 'job'||n,s,'entry'||s,'application','child-only%','details'
                FROM generate_series(?,?) n CROSS JOIN generate_series(0,3) s
                """).params(user, first, last).update();
    }

    @Test
    void sqlSearchMatchesThePreviousJavaSemanticsIncludingChildAndCrossFieldText() {
        var user = UUID.randomUUID();
        seedInventory(user, 1, 5);
        seedJobs(user, 1, 5);
        seedInventory(UUID.randomUUID(), 1, 5);
        seedJobs(UUID.randomUUID(), 1, 5);
        jdbc.sql("UPDATE inventory.item SET name='ALpha',location='Shelf',brand='Maker',model='Model', "
                + "tags=ARRAY['tag-only','عربي'],notes='ΟΣ İ',project_catalog_keys=ARRAY['project-only'] WHERE user_id=? AND id='item1'")
                .param(user).update();
        jdbc.sql("UPDATE inventory.item SET name='alpha', archived_at=now() WHERE user_id=? AND id='item2'")
                .param(user).update();
        jdbc.sql("UPDATE inventory.need SET status='dismissed' WHERE user_id=? AND id='need2'").param(user).update();
        jdbc.sql("UPDATE inventory.need SET project_catalog_key='project-only',status='ordered',notes='ΟΣ İ' "
                + "WHERE user_id=? AND id='need1'").param(user).update();
        jdbc.sql("UPDATE jobs.application SET status='interview',work_type='contract', "
                + "remote_status='needs_verification',compensation='Rate',url='https://example.test/role', "
                + "notes='عربي ΟΣ İ' WHERE user_id=? AND id='job1'").param(user).update();
        jdbc.sql("UPDATE jobs.history_entry SET evidence_url='https://evidence.test/'||id "
                + "WHERE user_id=? AND application_id='job1'").param(user).update();
        for (var needle : List.of("", " ChILD-only% ", "child-only_", "spec2", "tag-only", "عربي", "project-only",
                "ΟΣ", "ος", "οσ", "İ", "i̇", "Maker\nModel", "spec4\nchild-only% 0", "child-only% 0\nchild-only% 1", "new", "ordered")) {
            for (var filtered : List.of(false, true)) {
                var search = InventorySearch.builder().query(needle).limit(2)
                        .category(filtered ? "component" : null).location(filtered ? "Shelf" : null)
                        .projectCatalogKey(filtered ? "project-only" : null).build();
                assertThat(inventory.search(user, search)).as("inventory %s filtered=%s", needle, filtered)
                        .isEqualTo(legacyInventory(user, search));
            }
        }
        for (var needle : List.of("", " CHILD-only% ", "child-only_", "details", "Engineer", "Rate", "عربي",
                "ΟΣ", "ος", "οσ", "İ", "i̇", "evidence.test", "application\nchild-only%\ndetails", "Company 1\nEngineer", "needs_verification")) {
            for (int offset : new int[] {0, 1, 10}) {
                for (var filtered : List.of(false, true)) {
                    var search = ApplicationSearch.builder().query(needle).limit(1).offset(offset)
                            .status(filtered ? "interview" : null).workType(filtered ? "contract" : null)
                            .remoteStatus(filtered ? "needs_verification" : null).build();
                    assertThat(jobs.search(user, search)).as("jobs %s offset=%s filtered=%s", needle, offset, filtered)
                            .isEqualTo(legacyJobs(user, search));
                }
            }
        }
        assertThat(inventory.search(UUID.randomUUID(), InventorySearch.builder().query("").limit(1).build()).items())
                .isEmpty();
        assertThat(jobs.search(UUID.randomUUID(), ApplicationSearch.builder().query("").limit(1).build()).total())
                .isZero();
    }

    // Frozen pre-KAN-328 matching oracle; intentionally independent of the SQL predicates.
    private InventorySnapshot legacyInventory(UUID userId, InventorySearch search) {
        var needle = search.query().trim().toLowerCase(Locale.ROOT);
        var matchingItems = inventory.all(userId).items().stream()
                .filter(item -> item.archivedAt() == null)
                .filter(item -> search.projectCatalogKey() == null
                        || item.projectCatalogKeys().contains(search.projectCatalogKey()))
                .filter(item -> search.category() == null || search.category().equals(item.category()))
                .filter(item -> search.location() == null || search.location().equals(item.location()))
                .filter(item -> needle.isEmpty() || itemText(item).contains(needle))
                .sorted(Comparator.comparing(item -> item.name().toLowerCase(Locale.ROOT))).limit(search.limit()).toList();
        var matchingNeeds = inventory.all(userId).needs().stream()
                .filter(need -> Set.of("needed", "ordered").contains(need.status()))
                .filter(need -> search.projectCatalogKey() == null
                        || search.projectCatalogKey().equals(need.projectCatalogKey()))
                .filter(need -> needle.isEmpty() || needText(need).contains(needle))
                .sorted(Comparator.comparing(need -> need.name().toLowerCase(Locale.ROOT))).limit(search.limit()).toList();
        return new InventorySnapshot(matchingItems, matchingNeeds);
    }

    private ApplicationPage legacyJobs(UUID userId, ApplicationSearch search) {
        var needle = search.query().trim().toLowerCase(Locale.ROOT);
        var matches = jobs.all(userId).stream()
                .filter(application -> search.status() == null || search.status().equals(application.status()))
                .filter(application -> search.workType() == null || search.workType().equals(application.workType()))
                .filter(application -> search.remoteStatus() == null
                        || search.remoteStatus().equals(application.remoteStatus()))
                .filter(application -> needle.isEmpty() || searchableText(application).contains(needle))
                .toList();
        var page = matches.stream().skip(search.offset()).limit(search.limit()).toList();
        return new ApplicationPage(page, matches.size(), search.limit(), search.offset());
    }

    private static String itemText(InventoryItem item) {
        var fields = Stream.of(item.name(), item.category(), item.subcategory(), item.unit(), item.brand(),
                item.model(), item.location(), item.notes());
        return text(Stream.of(fields, item.tags().stream(), item.projectCatalogKeys().stream(),
                item.specifications().keySet().stream(), item.specifications().values().stream()));
    }

    private static String needText(InventoryNeed need) {
        var fields = Stream.of(need.name(), need.category(), need.subcategory(), need.unit(), need.notes(),
                need.projectCatalogKey(), need.status(), need.priority());
        return text(Stream.of(fields, need.specifications().keySet().stream(), need.specifications().values().stream()));
    }

    private static String text(Stream<Stream<String>> parts) {
        return String.join("\n", parts.flatMap(part -> part).filter(Objects::nonNull).toList()).toLowerCase(Locale.ROOT);
    }
    private static String searchableText(JobApplication application) {
        var fields = Stream.of(application.company(), application.role(), application.url(), application.status(),
                application.workType(), application.remoteRegion(), application.remoteEvidence(),
                application.remoteCaveat(), application.compensation(), application.nextAction(), application.notes());
        var history = application.history().stream().flatMap(BoundedSearchIntegrationTest::historyText);
        return Stream.concat(fields, history).filter(Objects::nonNull).reduce("", (text, field) -> text + "\n" + field)
                .toLowerCase(Locale.ROOT);
    }

    private static Stream<String> historyText(HistoryEntry entry) {
        return Stream.of(entry.kind(), entry.summary(), entry.details(), entry.evidenceUrl());
    }
}

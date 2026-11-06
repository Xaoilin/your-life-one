package com.yourlifeone.knowledge.projects.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.knowledge.testing.KnowledgeRequests;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The project catalogue and wiki through the full request path against a real PostgreSQL and RabbitMQ. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ProjectsApiIntegrationTest {
    private static final String BASE = "/api/knowledge/v1/projects";
    private static final String INVENTORY_APPROVALS = "public.yourlifeone_inventory_oauth_clients";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private KnowledgeRequests requests;

    @BeforeEach
    void setUp() {
        requests = new KnowledgeRequests(mockMvc);
    }

    private static String project(String id, String name, boolean pinned) {
        return """
                {"id":"%s","overviewPageId":"%s-overview","name":"%s","summary":"Built by YourLife","kind":"web_app",
                 "links":[{"kind":"repository","label":"","url":"https://github.com/x/%s"},{"kind":"demo","url":"ftp://no"}],
                 "setupSteps":[],"runRecipes":[{"executable":"/usr/bin/npm"},{"executable":"npm","args":["run","dev"],
                 "environment":{"PORT":"5173","API_KEY":"x"},"workingDirectory":"../up"}],
                 "preview":{"icon":"AB","accentColor":"#4F9CFF","backgroundColor":"bad"},"status":"active",
                 "tags":["Magnus Desk","desk"],"isPinned":%s}""".formatted(id, id, name, id, pinned);
    }

    private void create(UUID user, String id, String name, boolean pinned) throws Exception {
        requests.as(user, post(BASE).contentType(MediaType.APPLICATION_JSON).content(project(id, name, pinned)))
                .andExpect(status().isOk());
    }

    @Test
    void createsAProjectWithItsOverviewPageAndTidiedReference() throws Exception {
        var user = UUID.randomUUID();
        requests.as(user, post(BASE).contentType(MediaType.APPLICATION_JSON).content(project("p1", "Agent Board", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].catalogKey").value("custom:p1"))
                .andExpect(jsonPath("$.projects[0].links.length()").value(1))
                .andExpect(jsonPath("$.projects[0].links[0].id").value("custom:p1:link:1"))
                .andExpect(jsonPath("$.projects[0].links[0].label").value("Link 1"))
                .andExpect(jsonPath("$.projects[0].runRecipes.length()").value(1))
                .andExpect(jsonPath("$.projects[0].runRecipes[0].displayCommand").value("npm run dev"))
                .andExpect(jsonPath("$.projects[0].runRecipes[0].environment.PORT").value("5173"))
                .andExpect(jsonPath("$.projects[0].runRecipes[0].environment.API_KEY").doesNotExist())
                .andExpect(jsonPath("$.projects[0].preview.accentColor").value("#4f9cff"))
                .andExpect(jsonPath("$.projects[0].preview.backgroundColor").value("#171827"))
                .andExpect(jsonPath("$.projects[0].isPinned").value(false))
                .andExpect(jsonPath("$.pages[0].isOverview").value(true))
                .andExpect(jsonPath("$.pages[0].content", containsString("# Agent Board\n\nBuilt by YourLife")));
        requests.as(user, post(BASE).contentType(MediaType.APPLICATION_JSON).content(project("p1", "Again", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("project_exists"));
        requests.as(UUID.randomUUID(), get(BASE)).andExpect(jsonPath("$.projects.length()").value(0));
    }

    @Test
    void pinningArchivingAndReorderingKeepEachSectionNumberedFromZero() throws Exception {
        var user = UUID.randomUUID();
        create(user, "a", "Alpha", false);
        create(user, "b", "Beta", false);
        create(user, "c", "Gamma", true);

        requests.as(user, put(BASE + "/a/pinned").contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":true}"))
                .andExpect(jsonPath("$.projects[0].id").value("c"))
                .andExpect(jsonPath("$.projects[1].id").value("a"))
                .andExpect(jsonPath("$.projects[1].sortOrder").value(1))
                .andExpect(jsonPath("$.projects[2].id").value("b"))
                .andExpect(jsonPath("$.projects[2].sortOrder").value(0));
        requests.as(user, put(BASE + "/a/archived").contentType(MediaType.APPLICATION_JSON).content("{\"archived\":true}"))
                .andExpect(jsonPath("$.projects[2].id").value("a"))
                .andExpect(jsonPath("$.projects[2].status").value("archived"))
                .andExpect(jsonPath("$.projects[2].statusBeforeArchive").value("active"))
                .andExpect(jsonPath("$.projects[2].isPinned").value(false));
        requests.as(user, put(BASE + "/a/pinned").contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":true}"))
                .andExpect(status().isConflict());
        requests.as(user, put(BASE + "/a/archived").contentType(MediaType.APPLICATION_JSON).content("{\"archived\":false}"))
                .andExpect(jsonPath("$.projects[?(@.id=='a')].status").value("active"))
                .andExpect(jsonPath("$.projects[?(@.id=='a')].sortOrder").value(1));

        requests.as(user, post(BASE + "/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section\":\"projects\",\"ids\":[\"a\",\"b\"]}"))
                .andExpect(jsonPath("$.projects[1].id").value("a"));
        requests.as(user, post(BASE + "/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section\":\"projects\",\"ids\":[\"a\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("project_order_changed"));
    }

    @Test
    void statusAndReferenceChangesKeepThePlaceAndArchivingHasItsOwnOperation() throws Exception {
        var user = UUID.randomUUID();
        create(user, "a", "Alpha", true);
        requests.as(user, put(BASE + "/a/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"blocked\"}"))
                .andExpect(jsonPath("$.projects[0].status").value("blocked"))
                .andExpect(jsonPath("$.projects[0].isPinned").value(true));
        requests.as(user, put(BASE + "/a/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"archived\"}"))
                .andExpect(status().isConflict());
        requests.as(user, put(BASE + "/a/reference").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Alpha 2","summary":"","kind":"cli","links":[],"setupSteps":[{"title":"Install"}],
                         "runRecipes":[],"tags":["x"]}"""))
                .andExpect(jsonPath("$.projects[0].name").value("Alpha 2"))
                .andExpect(jsonPath("$.projects[0].catalogKey").value("custom:a"))
                .andExpect(jsonPath("$.projects[0].setupSteps[0].id").value("custom:a:setup:1"))
                .andExpect(jsonPath("$.projects[0].preview.icon").value("folder"));
    }

    @Test
    void wikiPagesKeepOneOverviewThatCannotBeDeleted() throws Exception {
        var user = UUID.randomUUID();
        create(user, "a", "Alpha", false);
        requests.as(user, put(BASE + "/pages/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"a\",\"title\":\" \",\"content\":\"Notes\",\"isOverview\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Untitled Page"));
        requests.as(user, delete(BASE + "/pages/a-overview")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("overview_page_required"));
        requests.as(user, put(BASE + "/pages/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"a\",\"title\":\"Notes\",\"content\":\"Notes\",\"isOverview\":true}"))
                .andExpect(jsonPath("$.isOverview").value(true));
        requests.as(user, get(BASE))
                .andExpect(jsonPath("$.pages[?(@.id=='a-overview')].isOverview").value(false));
        requests.as(user, delete(BASE + "/pages/a-overview")).andExpect(status().isNoContent());
        requests.as(user, put(BASE + "/pages/x").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"missing\",\"title\":\"X\",\"content\":\"\",\"isOverview\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resolvesProjectsByNameKeyOrTagForApprovedInventoryAgentsOnly() throws Exception {
        var user = UUID.randomUUID();
        create(user, "a", "Magnus Saddle", false);
        create(user, "b", "Agent Board", false);
        var jdbc = JdbcClient.create(dataSource);
        jdbc.sql("CREATE TABLE IF NOT EXISTS " + INVENTORY_APPROVALS
                + " (user_id uuid, client_id text, revoked_at timestamptz)").update();
        jdbc.sql("INSERT INTO " + INVENTORY_APPROVALS + " (user_id, client_id) VALUES (?, 'codex')").param(user).update();

        requests.as(user, get(BASE + "/resolve").param("query", "Desk"))
                .andExpect(jsonPath("$.projects.length()").value(2))
                .andExpect(jsonPath("$.projects[0].name").value("Agent Board"));
        requests.as(user, get(BASE + "/resolve").param("query", "custom:b"))
                .andExpect(jsonPath("$.projects.length()").value(1));
        requests.asAgent(user, "codex", get(BASE + "/resolve").param("query", "saddle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].catalogKey").value("custom:a"));
        requests.asAgent(user, "stranger", get(BASE + "/resolve").param("query", "saddle"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("agent_not_approved"));
        requests.asAgent(user, "codex", get(BASE)).andExpect(status().isForbidden());
        requests.as(user, get(BASE + "/resolve").param("query", " ! ")).andExpect(status().isBadRequest());
    }

    @Test
    void concurrentCreatesOfOneProjectMakeOneProject() throws Exception {
        var user = UUID.randomUUID();
        var results = KnowledgeRequests.concurrently(8, attempt -> requests.as(user, post(BASE)
                .header(IdempotencyKeys.HEADER, "create-" + attempt).contentType(MediaType.APPLICATION_JSON)
                .content(project("same", "Same", false))));

        var created = results.stream().filter(result -> result.getResponse().getStatus() == 200).count();
        assertEquals(1, created);
        requests.as(user, get(BASE)).andExpect(jsonPath("$.projects.length()").value(1))
                .andExpect(jsonPath("$.pages.length()").value(1));
    }

    @Test
    void readsProjectsImportedFromTheReactApp() throws Exception {
        var user = UUID.randomUUID();
        JdbcClient.create(dataSource).sql("""
                INSERT INTO projects.project (user_id, id, catalog_key, name, kind, status, sort_order, links,
                                              run_recipes, preview, created_at, updated_at)
                VALUES (?, 'imported', 'catalog:imported', 'Imported', 'cli', 'active', 0,
                        '[{"id":"l","kind":"repository","label":"Repo","url":"https://github.com/x/y","legacy":true}]',
                        '[{"id":"r","label":"Run","executable":"npm","args":["start"],"mode":"service"}]',
                        '{"icon":"IM","accentColor":"#123456","backgroundColor":"#654321"}', now(), now())""")
                .param(user).update();

        requests.as(user, get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].links[0].url").value("https://github.com/x/y"))
                .andExpect(jsonPath("$.projects[0].runRecipes[0].args[0]").value("start"));
    }

    @Test
    void deletingAProjectNeedsConfirmationAndRenumbersItsSection() throws Exception {
        var user = UUID.randomUUID();
        create(user, "a", "Alpha", false);
        create(user, "b", "Beta", false);

        requests.as(user, delete(BASE + "/a")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("confirmation_required"));
        requests.as(user, delete(BASE + "/a").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects.length()").value(1))
                .andExpect(jsonPath("$.projects[0].sortOrder").value(0))
                .andExpect(jsonPath("$.pages.length()").value(1));
        requests.as(user, delete(BASE + "/a").param("confirm", "true")).andExpect(status().isNotFound());
    }
}

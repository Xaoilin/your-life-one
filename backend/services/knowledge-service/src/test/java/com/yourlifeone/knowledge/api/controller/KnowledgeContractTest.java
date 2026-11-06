package com.yourlifeone.knowledge.api.controller;

import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Provider side of the contract with the React app and its agent tools: responses keep the shape of
 * their fixtures in {@code contracts/knowledge-service}, which the app also uses as mocks.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class KnowledgeContractTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/knowledge-service");
    private static final String BASE = "/api/knowledge/v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private final UUID user = UUID.randomUUID();

    private void call(String name, String method, String path, MockHttpServletRequestBuilder request) throws Exception {
        call(name, method, path, request, null);
    }

    private void call(String name, String method, String path, MockHttpServletRequestBuilder request, String clientId)
            throws Exception {
        var response = mockMvc.perform(request.with(jwt().jwt(token -> {
            token.subject(user.toString());
            if (clientId != null) token.claim("client_id", clientId);
        }))).andReturn().getResponse();
        CONTRACT.check(name, method, path, response.getStatus(), response.getContentAsString());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void knowledgeBaseResponsesKeepTheirShape() throws Exception {
        var knowledge = BASE + "/knowledge";
        call("topic-saved", "PUT", knowledge + "/topics/prayer", json(put(knowledge + "/topics/prayer"), """
                {"name":"Prayer","description":"Salah and its conditions","icon":"🕌","color":"#22c55e"}"""));
        call("entry-saved", "PUT", knowledge + "/entries/on-time", json(put(knowledge + "/entries/on-time"), """
                {"topicId":"prayer","title":"Praying on time","content":"Pray each prayer in its window.",
                 "sources":[{"type":"quran","surah":4,"ayahStart":103},
                            {"type":"hadith","collection":"Bukhari","hadithNumber":"527"}],"tags":["salah"]}"""));
        call("knowledge", "GET", knowledge, get(knowledge));
        call("progress", "GET", BASE + "/progress", get(BASE + "/progress"));
        call("entry-invalid", "PUT", knowledge + "/entries/bad", json(put(knowledge + "/entries/bad"), """
                {"topicId":"prayer","title":"Bad","content":"","sources":[{"type":"poem"}],"tags":[]}"""));
        call("entry-deleted", "DELETE", knowledge + "/entries/on-time", delete(knowledge + "/entries/on-time"));
        call("topic-deleted", "DELETE", knowledge + "/topics/prayer", delete(knowledge + "/topics/prayer"));
    }

    @Test
    void lifestyleResponsesKeepTheirShape() throws Exception {
        var lifestyle = BASE + "/lifestyle";
        call("lifestyle-item-saved", "PUT", lifestyle + "/items/backbiting", json(put(lifestyle + "/items/backbiting"), """
                {"type":"haram","title":"Backbiting","notes":"Guard the tongue","status":"avoiding",
                 "sources":["https://quran.com/49/12"]}"""));
        call("lifestyle-item-moved", "POST", lifestyle + "/items/backbiting/move",
                json(post(lifestyle + "/items/backbiting/move"), "{\"type\":\"major-sin\",\"position\":0}"));
        call("lifestyle-order-saved", "POST", lifestyle + "/order", json(post(lifestyle + "/order"),
                "{\"type\":\"major-sin\",\"ids\":[\"backbiting\"]}"));
        call("lifestyle-order-changed", "POST", lifestyle + "/order", json(post(lifestyle + "/order"),
                "{\"type\":\"major-sin\",\"ids\":[]}"));
        call("lifestyle", "GET", lifestyle, get(lifestyle));
        call("lifestyle-item-deleted", "DELETE", lifestyle + "/items/backbiting", delete(lifestyle + "/items/backbiting"));
    }

    @Test
    void projectResponsesKeepTheirShape() throws Exception {
        var projects = BASE + "/projects";
        call("project-created", "POST", projects, json(post(projects), """
                {"id":"agentboard","overviewPageId":"agentboard-overview","catalogKey":"catalog:agentboard",
                 "name":"Agent Board","summary":"Whiteboards for agents","kind":"web_app",
                 "links":[{"id":"catalog:agentboard:repo","kind":"repository","label":"GitHub repository",
                           "url":"https://example.com/repositories/whiteboarding"}],
                 "setupSteps":[{"id":"catalog:agentboard:setup:node","title":"Install workspace dependencies",
                                "description":"Requires Node.js 22 or newer and npm.","displayCode":"npm install"}],
                 "runRecipes":[{"id":"catalog:agentboard:run:dev","label":"Web development server",
                                "displayCommand":"npm run dev","executable":"npm","args":["run","dev"],
                                "workingDirectory":"web","environment":{"PORT":"5173"},"localUrl":"http://localhost:5173",
                                "prerequisites":["Node.js 22+","npm"],"mode":"service"}],
                 "preview":{"icon":"AB","accentColor":"#4f9cff","backgroundColor":"#112033",
                            "coverImageUrl":"https://example.com/cover.png"},
                 "verifiedAt":"2026-09-20T10:00:00Z","status":"active","tags":["whiteboarding"],"isPinned":true}"""));
        call("project-reference-updated", "PUT", projects + "/agentboard/reference",
                json(put(projects + "/agentboard/reference"), """
                {"name":"Agent Board","summary":"Whiteboards for AI agents","kind":"web_app","links":[],"setupSteps":[],
                 "runRecipes":[],"preview":{"icon":"AB","accentColor":"#4f9cff","backgroundColor":"#112033"},
                 "tags":["whiteboarding"]}"""));
        call("project-status-updated", "PUT", projects + "/agentboard/status",
                json(put(projects + "/agentboard/status"), "{\"status\":\"blocked\"}"));
        call("project-pinned", "PUT", projects + "/agentboard/pinned",
                json(put(projects + "/agentboard/pinned"), "{\"pinned\":false}"));
        call("project-archived", "PUT", projects + "/agentboard/archived",
                json(put(projects + "/agentboard/archived"), "{\"archived\":true}"));
        call("projects-reordered", "POST", projects + "/order", json(post(projects + "/order"),
                "{\"section\":\"archived\",\"ids\":[\"agentboard\"]}"));
        call("project-order-changed", "POST", projects + "/order", json(post(projects + "/order"),
                "{\"section\":\"projects\",\"ids\":[\"agentboard\"]}"));
        call("project-page-saved", "PUT", projects + "/pages/notes", json(put(projects + "/pages/notes"), """
                {"projectId":"agentboard","title":"Notes","content":"# Notes","isOverview":false}"""));
        call("project-page-deleted", "DELETE", projects + "/pages/notes", delete(projects + "/pages/notes"));
        call("overview-page-required", "DELETE", projects + "/pages/agentboard-overview",
                delete(projects + "/pages/agentboard-overview"));
        call("projects", "GET", projects, get(projects));
        call("project-deleted", "DELETE", projects + "/agentboard",
                delete(projects + "/agentboard").param("confirm", "true"));
        call("projects-resolved", "GET", projects + "/resolve", get(projects + "/resolve").param("query", "agent"));
    }

    @Test
    void refusedAgentsAndRateLimitedRequestsKeepTheirShape() throws Exception {
        JdbcClient.create(dataSource).sql("CREATE TABLE IF NOT EXISTS public.yourlifeone_inventory_oauth_clients"
                + " (user_id uuid, client_id text, revoked_at timestamptz)").update();
        call("agent-not-approved", "GET", BASE + "/projects/resolve",
                get(BASE + "/projects/resolve").param("query", "agent"), "unapproved-agent");
        var limited = UUID.randomUUID();
        var response = mockMvc.perform(get(BASE + "/knowledge").with(jwt().jwt(token -> token.subject(limited.toString()))))
                .andReturn().getResponse();
        for (int request = 0; request < 200 && response.getStatus() != 429; request++) {
            response = mockMvc.perform(get(BASE + "/knowledge").with(jwt().jwt(token -> token.subject(limited.toString()))))
                    .andReturn().getResponse();
        }
        CONTRACT.check("rate-limited", "GET", BASE + "/knowledge", response.getStatus(), response.getContentAsString());
    }
}

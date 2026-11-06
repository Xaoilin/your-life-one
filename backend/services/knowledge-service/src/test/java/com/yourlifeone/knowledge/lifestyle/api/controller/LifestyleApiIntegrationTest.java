package com.yourlifeone.knowledge.lifestyle.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.knowledge.testing.KnowledgeRequests;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The lifestyle tracker through the full request path against a real PostgreSQL and RabbitMQ. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class LifestyleApiIntegrationTest {
    private static final String BASE = "/api/knowledge/v1/lifestyle";

    @Autowired
    private MockMvc mockMvc;

    private KnowledgeRequests requests;

    @BeforeEach
    void setUp() {
        requests = new KnowledgeRequests(mockMvc);
    }

    private static String item(String type, String title, String status) {
        return """
                {"type":"%s","title":"%s","notes":"","status":"%s","sources":["https://quran.com/4/36"]}"""
                .formatted(type, title, status);
    }

    private void save(UUID user, String id, String type, String title, String status) throws Exception {
        requests.as(user, put(BASE + "/items/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(item(type, title, status))).andExpect(status().isOk());
    }

    @Test
    void newItemsGoToTheEndOfTheirColumnAndStatusesMustFitTheColumn() throws Exception {
        var user = UUID.randomUUID();
        save(user, "a", "haram", "Backbiting", "struggling");
        save(user, "b", "haram", "Lying", "avoiding");
        save(user, "c", "halal", "Charity", "practicing");

        requests.as(user, get(BASE))
                .andExpect(jsonPath("$.items[?(@.id=='b')].sortOrder").value(1))
                .andExpect(jsonPath("$.items[?(@.id=='c')].sortOrder").value(0));
        requests.as(user, put(BASE + "/items/d").contentType(MediaType.APPLICATION_JSON)
                        .content(item("haram", "Envy", "practicing")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_status"));
        requests.as(UUID.randomUUID(), get(BASE)).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void movingToTheOtherFamilyRestartsTheStatusAndRenumbersBothColumns() throws Exception {
        var user = UUID.randomUUID();
        save(user, "a", "haram", "Backbiting", "mastered");
        save(user, "b", "haram", "Lying", "avoiding");
        save(user, "c", "wajib-both", "Prayer", "consistent");

        requests.as(user, post(BASE + "/items/a/move").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"wajib-both\",\"position\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='a')].status").value("want-to-start"))
                .andExpect(jsonPath("$.items[?(@.id=='a')].sortOrder").value(0))
                .andExpect(jsonPath("$.items[?(@.id=='c')].sortOrder").value(1))
                .andExpect(jsonPath("$.items[?(@.id=='b')].sortOrder").value(0));
        requests.as(user, post(BASE + "/items/b/move").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"major-sin\"}"))
                .andExpect(jsonPath("$.items[?(@.id=='b')].status").value("avoiding"));
    }

    @Test
    void reorderingNeedsExactlyTheColumnsItems() throws Exception {
        var user = UUID.randomUUID();
        save(user, "a", "halal", "Charity", "practicing");
        save(user, "b", "halal", "Fasting", "sometimes");

        requests.as(user, post(BASE + "/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"halal\",\"ids\":[\"b\",\"a\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='b')].sortOrder").value(0));
        requests.as(user, post(BASE + "/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"halal\",\"ids\":[\"a\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("lifestyle_order_changed"));
        requests.as(user, delete(BASE + "/items/a")).andExpect(status().isNoContent());
        requests.as(user, delete(BASE + "/items/a")).andExpect(status().isNotFound());
    }

    @Test
    void concurrentCreatesOfOneItemMakeOneItem() throws Exception {
        var user = UUID.randomUUID();
        var results = KnowledgeRequests.concurrently(8, attempt -> requests.as(user, put(BASE + "/items/same")
                .header(IdempotencyKeys.HEADER, "item-" + attempt).contentType(MediaType.APPLICATION_JSON)
                .content(item("halal", "Charity", "practicing"))));

        for (var result : results) assertEquals(200, result.getResponse().getStatus());
        requests.as(user, get(BASE)).andExpect(jsonPath("$.items.length()").value(1));
    }
}

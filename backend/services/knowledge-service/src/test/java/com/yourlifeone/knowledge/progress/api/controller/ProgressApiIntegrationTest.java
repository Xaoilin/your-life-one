package com.yourlifeone.knowledge.progress.api.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The counts the planner's badges read, for the app's own tokens only. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ProgressApiIntegrationTest {
    private static final String BASE = "/api/knowledge/v1";

    @Autowired
    private MockMvc mockMvc;

    private KnowledgeRequests requests;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        requests = new KnowledgeRequests(mockMvc);
    }

    private void save(String path, String body) throws Exception {
        requests.as(user, put(BASE + path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private void item(String id, String type, String status) throws Exception {
        save("/lifestyle/items/" + id, """
                {"type":"%s","title":"%s","notes":"","status":"%s","sources":[]}""".formatted(type, id, status));
    }

    @Test
    void countsEntriesTopicsAndLifestyleProgress() throws Exception {
        save("/knowledge/topics/prayer", "{\"name\":\"Prayer\",\"description\":\"\",\"icon\":\"🕌\",\"color\":\"#22c55e\"}");
        save("/knowledge/entries/on-time", """
                {"topicId":"prayer","title":"On time","content":"Pray in the window.","sources":[],"tags":[]}""");
        item("backbiting", "haram", "mastered");
        item("lying", "haram", "avoiding");
        item("charity", "halal", "consistent");
        item("fasting", "halal", "sometimes");

        requests.as(user, get(BASE + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").value(1))
                .andExpect(jsonPath("$.topics").value(1))
                .andExpect(jsonPath("$.haramMastered").value(1))
                .andExpect(jsonPath("$.halalConsistent").value(1))
                .andExpect(jsonPath("$.lifestyleItems").value(4));
        requests.as(UUID.randomUUID(), get(BASE + "/progress")).andExpect(jsonPath("$.lifestyleItems").value(0));
    }

    @Test
    void agentsAreRefused() throws Exception {
        requests.asAgent(user, "some-agent", get(BASE + "/progress"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("agent_not_approved"));
    }
}

package com.yourlifeone.knowledge.knowledge.api.controller;

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

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The knowledge base through the full request path against a real PostgreSQL and RabbitMQ. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class KnowledgeApiIntegrationTest {
    private static final String BASE = "/api/knowledge/v1/knowledge";
    private static final String TOPIC = """
            {"name":" Prayer ","description":"Salah","icon":"🕌","color":"#22c55e"}""";
    private static final String ENTRY = """
            {"topicId":"prayer","title":" Praying on time ","content":"Pray each prayer in its window.",
             "sources":[{"type":"quran","surah":4,"ayahStart":103},{"type":"hadith","collection":"Bukhari",
             "hadithNumber":"527","author":" "}],"tags":["salah"," salah ","time"]}""";

    @Autowired
    private MockMvc mockMvc;

    private KnowledgeRequests requests;

    @BeforeEach
    void setUp() {
        requests = new KnowledgeRequests(mockMvc);
    }

    @Test
    void savesTopicsAndEntriesForEachUserAndDeletesATopicWithItsEntries() throws Exception {
        var user = UUID.randomUUID();
        var other = UUID.randomUUID();
        requests.as(user, put(BASE + "/topics/prayer").contentType(MediaType.APPLICATION_JSON).content(TOPIC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prayer"))
                .andExpect(jsonPath("$.sortOrder").value(0));
        requests.as(user, put(BASE + "/topics/fasting").contentType(MediaType.APPLICATION_JSON)
                .content(TOPIC.replace("Prayer", "Fasting"))).andExpect(jsonPath("$.sortOrder").value(1));
        requests.as(user, put(BASE + "/entries/on-time").contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Praying on time"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.sources[1].author").value(nullValue()));

        requests.as(user, get(BASE))
                .andExpect(jsonPath("$.topics[0].id").value("prayer"))
                .andExpect(jsonPath("$.topics[1].id").value("fasting"))
                .andExpect(jsonPath("$.entries[0].sources[0].surah").value(4));
        requests.as(other, get(BASE)).andExpect(jsonPath("$.topics.length()").value(0));

        requests.as(other, delete(BASE + "/topics/prayer")).andExpect(status().isNotFound());
        requests.as(user, delete(BASE + "/topics/prayer")).andExpect(status().isNoContent());
        requests.as(user, get(BASE))
                .andExpect(jsonPath("$.topics.length()").value(1))
                .andExpect(jsonPath("$.entries.length()").value(0));
    }

    @Test
    void refusesAnEntryWithoutItsTopicOrWithAnUnknownSourceType() throws Exception {
        var user = UUID.randomUUID();
        requests.as(user, put(BASE + "/entries/e1").contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("topic_not_found"));
        requests.as(user, put(BASE + "/topics/prayer").contentType(MediaType.APPLICATION_JSON).content(TOPIC));
        requests.as(user, put(BASE + "/entries/e1").contentType(MediaType.APPLICATION_JSON)
                        .content(ENTRY.replace("\"hadith\"", "\"poem\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_source_type"));
    }

    @Test
    void aRepeatedWriteReplaysAndAReusedKeyWithAnotherRequestIsRefused() throws Exception {
        var user = UUID.randomUUID();
        requests.as(user, put(BASE + "/topics/prayer").header(IdempotencyKeys.HEADER, "topic-1")
                .contentType(MediaType.APPLICATION_JSON).content(TOPIC)).andExpect(status().isOk());
        requests.as(user, put(BASE + "/topics/prayer").header(IdempotencyKeys.HEADER, "topic-1")
                        .contentType(MediaType.APPLICATION_JSON).content(TOPIC))
                .andExpect(status().isOk())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));
        requests.as(user, put(BASE + "/topics/prayer").header(IdempotencyKeys.HEADER, "topic-1")
                        .contentType(MediaType.APPLICATION_JSON).content(TOPIC.replace("Prayer", "Other")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"));
    }

    @Test
    void concurrentCreatesOfOneTopicMakeOneTopic() throws Exception {
        var user = UUID.randomUUID();
        var results = KnowledgeRequests.concurrently(8, attempt -> requests.as(user, put(BASE + "/topics/same")
                .header(IdempotencyKeys.HEADER, "same-" + attempt).contentType(MediaType.APPLICATION_JSON).content(TOPIC)));

        for (var result : results) assertEquals(200, result.getResponse().getStatus());
        requests.as(user, get(BASE)).andExpect(jsonPath("$.topics.length()").value(1))
                .andExpect(jsonPath("$.topics[0].sortOrder").value(0));
    }
}

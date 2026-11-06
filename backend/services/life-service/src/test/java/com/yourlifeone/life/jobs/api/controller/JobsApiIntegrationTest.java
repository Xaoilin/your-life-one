package com.yourlifeone.life.jobs.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.life.testing.LifeRequests;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The job tracker through the full request path against a real PostgreSQL and RabbitMQ. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class JobsApiIntegrationTest {
    private static final String JOBS = "/api/life/v1/jobs";
    private static final String APPLICATIONS = JOBS + "/applications";
    private static final String APPLICATION = """
            {"id":"acme-engineer","company":" Acme ","role":"Engineer","url":"https://jobs.example/acme/1",
             "status":"applied","applicationDate":"2026-09-01","history":[
              {"kind":"application","summary":"Applied on the careers site","evidenceUrl":"https://mail.example/1"}]}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private LifeRequests requests;

    @BeforeEach
    void setUp() {
        requests = new LifeRequests(mockMvc);
    }

    private String add(UUID user, String body) throws Exception {
        return requests.as(user, post(APPLICATIONS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void addsWithDefaultsAndReportsARepeatAsADuplicate() throws Exception {
        var user = UUID.randomUUID();

        requests.as(user, post(APPLICATIONS).contentType(MediaType.APPLICATION_JSON).content(APPLICATION))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.application.company").value("Acme"))
                .andExpect(jsonPath("$.application.remoteStatus").value("needs_verification"))
                .andExpect(jsonPath("$.application.nextAction").isNotEmpty())
                .andExpect(jsonPath("$.application.history[0].details").value(""));
        // Same link, company and role under another ID: the existing record, unchanged.
        requests.as(user, post(APPLICATIONS).contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION.replace("acme-engineer", "acme-again").replace(" Acme ", "ACME")))
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.applicationId").value("acme-engineer"));

        requests.as(user, get(JOBS)).andExpect(jsonPath("$.applications.length()").value(1));
    }

    @Test
    void patchesOnlyTheGivenFieldsClearsOptionalOnesAndRefusesAStaleVersion() throws Exception {
        var user = UUID.randomUUID();
        add(user, APPLICATION);
        var updatedAt = JdbcClient.create(dataSource).sql("SELECT to_char(updated_at AT TIME ZONE 'UTC', "
                        + "'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') FROM jobs.application WHERE user_id = ?")
                .param(user).query(String.class).single();

        requests.as(user, patch(APPLICATIONS + "/acme-engineer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"interview\",\"clear\":[\"url\"],\"expectedUpdatedAt\":\"" + updatedAt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("interview"))
                .andExpect(jsonPath("$.application.url").doesNotExist())
                .andExpect(jsonPath("$.application.history.length()").value(1))
                .andExpect(jsonPath("$.application.applicationDate").value("2026-09-01"));
        requests.as(user, patch(APPLICATIONS + "/acme-engineer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"offer\",\"expectedUpdatedAt\":\"" + updatedAt + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("application_changed"));
        requests.as(user, patch(APPLICATIONS + "/acme-engineer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clear\":[\"company\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("not_clearable"));
        requests.as(user, patch(APPLICATIONS + "/acme-engineer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remoteStatus\":\"confirmed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("unconfirmed_remote_region"));
    }

    @Test
    void appendsEvidenceOnceAndSearchesPageByPage() throws Exception {
        var user = UUID.randomUUID();
        add(user, APPLICATION);
        add(user, "{\"company\":\"Globex\",\"role\":\"Staff Engineer\",\"status\":\"lead\"}");
        var evidence = "{\"kind\":\"contact\",\"date\":\"2026-09-10\",\"summary\":\"Recruiter replied\","
                + "\"evidenceUrl\":\"https://mail.example/2\"}";

        requests.as(user, post(APPLICATIONS + "/acme-engineer/history").contentType(MediaType.APPLICATION_JSON)
                .content(evidence)).andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.application.history.length()").value(2));
        requests.as(user, post(APPLICATIONS + "/acme-engineer/history").contentType(MediaType.APPLICATION_JSON)
                .content(evidence)).andExpect(jsonPath("$.duplicate").value(true));

        requests.as(user, get(APPLICATIONS).param("query", "RECRUITER replied"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.applications[0].id").value("acme-engineer"));
        requests.as(user, get(APPLICATIONS).param("limit", "1").param("offset", "1"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.applications[0].company").value("Globex"));
        requests.as(user, get(APPLICATIONS).param("status", "lead")).andExpect(jsonPath("$.total").value(1));
        requests.as(user, get(APPLICATIONS).param("limit", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void removesOnlyWhenConfirmed() throws Exception {
        var user = UUID.randomUUID();
        add(user, APPLICATION);

        requests.as(user, delete(APPLICATIONS + "/acme-engineer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("confirmation_required"));
        requests.as(user, delete(APPLICATIONS + "/acme-engineer").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").doesNotExist());
        requests.as(user, get(APPLICATIONS + "/acme-engineer")).andExpect(status().isNotFound());
        assertEquals(0, JdbcClient.create(dataSource).sql("SELECT count(*) FROM jobs.history_entry WHERE user_id = ?")
                .param(user).query(Integer.class).single());
    }

    @Test
    void concurrentDuplicateSubmissionsWriteOnce() throws Exception {
        var user = UUID.randomUUID();
        var sameKey = LifeRequests.concurrently(8, attempt -> requests.as(user, post(APPLICATIONS)
                .contentType(MediaType.APPLICATION_JSON).content(APPLICATION)
                .header(IdempotencyKeys.HEADER, "add-acme-1")));
        var sameRecordDifferentKeys = LifeRequests.concurrently(8, attempt -> requests.as(user, post(APPLICATIONS)
                .contentType(MediaType.APPLICATION_JSON).content(APPLICATION)
                .header(IdempotencyKeys.HEADER, "add-acme-" + (attempt + 2))));

        for (var response : sameKey) assertEquals(200, response.getResponse().getStatus());
        for (var response : sameRecordDifferentKeys) assertEquals(200, response.getResponse().getStatus());
        assertEquals(1, sameKey.stream().map(LifeRequests::body).distinct().count());
        var jdbc = JdbcClient.create(dataSource);
        assertEquals(1, jdbc.sql("SELECT count(*) FROM jobs.application WHERE user_id = ?").param(user)
                .query(Integer.class).single());
        assertEquals(1, jdbc.sql("SELECT count(*) FROM jobs.history_entry WHERE user_id = ?").param(user)
                .query(Integer.class).single());
    }

    @Test
    void aReusedKeyWithAnotherRequestIsRefusedAndARepeatIsReplayed() throws Exception {
        var user = UUID.randomUUID();

        requests.as(user, post(APPLICATIONS).contentType(MediaType.APPLICATION_JSON).content(APPLICATION)
                .header(IdempotencyKeys.HEADER, "add-1")).andExpect(status().isOk());
        requests.as(user, post(APPLICATIONS).contentType(MediaType.APPLICATION_JSON).content(APPLICATION)
                        .header(IdempotencyKeys.HEADER, "add-1"))
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));
        requests.as(user, post(APPLICATIONS).contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION.replace("Engineer", "Manager")).header(IdempotencyKeys.HEADER, "add-1"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"));
    }
}

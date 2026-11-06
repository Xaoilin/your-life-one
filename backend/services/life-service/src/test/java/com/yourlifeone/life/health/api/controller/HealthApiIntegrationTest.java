package com.yourlifeone.life.health.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The fast-food journal through the full request path against a real PostgreSQL and RabbitMQ. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class HealthApiIntegrationTest {
    private static final String ENTRIES = "/api/life/v1/health/fast-food";
    private static final String ENTRY = """
            {"venue":" McDonalds ","date":"2026-04-20","order":"Big Arch meal","rating":"bad",
             "symptoms":["nauseous","sluggish","nauseous"],"notes":"Not worth it"}""";

    @Autowired
    private MockMvc mockMvc;

    private ResultActions perform(MockHttpServletRequestBuilder request, UUID as) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(as.toString()))));
    }

    @Test
    void savesReplacesListsAndDeletesEachUsersOwnEntries() throws Exception {
        var user = UUID.randomUUID();
        var other = UUID.randomUUID();
        var id = UUID.randomUUID().toString();

        perform(put(ENTRIES + "/" + id).contentType(MediaType.APPLICATION_JSON).content(ENTRY), user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue").value("McDonalds"))
                .andExpect(jsonPath("$.symptoms.length()").value(2));
        perform(put(ENTRIES + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(ENTRY.replace("\"bad\"", "\"awful\"")), user)
                .andExpect(status().isOk());

        perform(get(ENTRIES), user)
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].rating").value("awful"))
                .andExpect(jsonPath("$.entries[0].date").value("2026-04-20"));
        perform(get(ENTRIES), other).andExpect(jsonPath("$.entries.length()").value(0));

        perform(delete(ENTRIES + "/" + id), other).andExpect(status().isNotFound());
        perform(delete(ENTRIES + "/" + id), user).andExpect(status().isNoContent());
        perform(get(ENTRIES), user).andExpect(jsonPath("$.entries.length()").value(0));
    }

    @Test
    void refusesValuesThatAreNotConfigured() throws Exception {
        var user = UUID.randomUUID();

        perform(put(ENTRIES + "/e1").contentType(MediaType.APPLICATION_JSON)
                .content(ENTRY.replace("\"bad\"", "\"great\"")), user)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_rating"));
        perform(put(ENTRIES + "/e1").contentType(MediaType.APPLICATION_JSON)
                .content(ENTRY.replace("\"sluggish\"", "\"happy\"")), user)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_symptoms"));
    }

    @Test
    void aRepeatedDeleteWithTheSameKeyReplaysInsteadOfFailing() throws Exception {
        var user = UUID.randomUUID();
        perform(put(ENTRIES + "/e2").contentType(MediaType.APPLICATION_JSON).content(ENTRY), user)
                .andExpect(status().isOk());

        perform(delete(ENTRIES + "/e2").header(IdempotencyKeys.HEADER, "delete-e2"), user)
                .andExpect(status().isNoContent());
        perform(delete(ENTRIES + "/e2").header(IdempotencyKeys.HEADER, "delete-e2"), user)
                .andExpect(status().isNoContent())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));
    }
}

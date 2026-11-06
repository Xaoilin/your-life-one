package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.WriteCommand;
import com.yourlifeone.commonservice.command.WriteCommandReply;
import com.yourlifeone.commonservice.command.retention.ProcessedCommandCleanup;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.prayer.api.command.CreateOutcomeHandler;
import com.yourlifeone.prayer.api.request.CreateOutcomeRequest;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.domain.Fixtures;
import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.PrayerName;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Writes travel through a real RabbitMQ to the consumer and are applied once, at every level:
 * the API replays a repeated Idempotency-Key, the consumer ignores a redelivered message, and the
 * database refuses a duplicate outcome. Same test configuration as {@link PrayerApiIntegrationTest},
 * so both share one cached context. London on 25 Sep 2026 (BST): Maghrib opens at 18:12Z, Isha at 19:18Z.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class PrayerIdempotentWritesTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String DHUHR = "{\"date\":\"2026-09-25\",\"prayer\":\"Dhuhr\",\"status\":\"on_time\",\"source\":\"dashboard\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ProcessedCommandCleanup cleanup;

    @MockitoBean
    private ScheduleSource scheduleSource;

    @MockitoBean
    private Clock clock;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void noonOnTheTwentyFifthInLondon() throws Exception {
        setTime("2026-09-25T12:00:00Z");
        when(scheduleSource.fetch(anyString(), anyString(), any())).thenAnswer(call -> Fixtures
                .london(call.getArgument(2, LocalDate.class)).toBuilder()
                .id(null)
                .city(call.getArgument(0))
                .country(call.getArgument(1))
                .build());
        perform(get("/api/prayer/v1/dashboard?city=London&country=United Kingdom"), null);
    }

    private void setTime(String instant) {
        when(clock.instant()).thenReturn(Instant.parse(instant));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String idempotencyKey) throws Exception {
        if (idempotencyKey != null) {
            request.header(IdempotencyKeys.HEADER, idempotencyKey);
        }
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(user.toString()))));
    }

    private ResultActions record(String body, String idempotencyKey) throws Exception {
        return perform(post("/api/prayer/v1/outcomes").contentType(MediaType.APPLICATION_JSON).content(body),
                idempotencyKey);
    }

    private long outcomesToday() {
        return jdbc.sql("SELECT count(*) FROM prayer.outcome WHERE user_id = :user AND prayer_date = DATE '2026-09-25'")
                .param("user", user)
                .query(Long.class)
                .single();
    }

    @Test
    void apiLevelARepeatedKeyReplaysTheFirstResponseWithoutWritingAgain() throws Exception {
        var first = record(DHUHR, "dhuhr-1")
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(IdempotencyKeys.REPLAYED_HEADER))
                .andExpect(jsonPath("$.firstReward").value(true))
                .andReturn().getResponse().getContentAsString();

        var replay = record(DHUHR, "dhuhr-1")
                .andExpect(status().isCreated())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(JSON.readTree(first), JSON.readTree(replay));
        assertEquals(1, outcomesToday());
    }

    @Test
    void apiLevelTheSameKeyForADifferentRequestIsRefused() throws Exception {
        record(DHUHR, "dhuhr-1").andExpect(status().isCreated());

        record(DHUHR.replace("on_time", "late"), "dhuhr-1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"));
    }

    @Test
    void apiLevelAnInvalidKeyIsRefused() throws Exception {
        record(DHUHR, "has space")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_idempotency_key"));
        assertEquals(0, outcomesToday());
    }

    @Test
    void messageLevelARedeliveredCommandIsAppliedOnce() {
        var request = CreateOutcomeRequest.builder().date(LocalDate.parse("2026-09-25"))
                .prayer(PrayerName.DHUHR).status(OutcomeStatus.ON_TIME).source("dashboard").build();
        var payload = JSON.valueToTree(request);
        var command = WriteCommand.builder()
                .messageId(IdempotencyKeys.messageId(user, "redelivered"))
                .type(CreateOutcomeHandler.TYPE)
                .userId(user)
                .fingerprint(IdempotencyKeys.fingerprint(CreateOutcomeHandler.TYPE, payload))
                .payload(payload)
                .build();

        var first = send(command);
        var redelivered = send(command);

        assertEquals(201, first.status());
        assertEquals(first, redelivered);
        assertEquals(1, outcomesToday());
    }

    private WriteCommandReply send(WriteCommand command) {
        return rabbit.convertSendAndReceiveAsType("", "yourlife.prayer.commands", command,
                new ParameterizedTypeReference<WriteCommandReply>() {
                });
    }

    @Test
    void databaseLevelTheSamePrayerCannotBeRecordedTwice() throws Exception {
        record(DHUHR, "dhuhr-1").andExpect(status().isCreated());

        record(DHUHR.replace("on_time", "late"), "dhuhr-2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("outcome_exists"));
        assertEquals(1, outcomesToday());
    }

    @Test
    void aRefusedWriteIsNotRememberedSoItsKeyWorksOnceThePrayerStarts() throws Exception {
        var isha = DHUHR.replace("Dhuhr", "Isha");
        record(isha, "isha-1").andExpect(jsonPath("$.code").value("prayer_not_started"));

        setTime("2026-09-25T18:12:00Z");

        record(isha, "isha-1")
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(IdempotencyKeys.REPLAYED_HEADER));
    }

    @Test
    void aRepeatedUndoReplaysItsSuccessInsteadOfNotFound() throws Exception {
        var created = record(DHUHR, "dhuhr-1").andReturn().getResponse().getContentAsString();
        var id = JSON.readTree(created).path("outcome").path("id").asString();

        perform(delete("/api/prayer/v1/outcomes/" + id), "undo-1").andExpect(status().isNoContent());
        perform(delete("/api/prayer/v1/outcomes/" + id), "undo-1")
                .andExpect(status().isNoContent())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));
        perform(delete("/api/prayer/v1/outcomes/" + id), "undo-2").andExpect(status().isNotFound());
    }

    @Test
    void preferencesSavedTwiceWithOneKeyAreSavedOnce() throws Exception {
        var body = "{\"enabled\":true,\"reminderEnabled\":false,\"reminderMinutes\":30}";
        perform(put("/api/prayer/v1/preferences").contentType(MediaType.APPLICATION_JSON).content(body), "prefs-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderMinutes").value(30));
        perform(put("/api/prayer/v1/preferences").contentType(MediaType.APPLICATION_JSON).content(body), "prefs-1")
                .andExpect(status().isOk())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"))
                .andExpect(jsonPath("$.reminderMinutes").value(30));
    }

    @Test
    void writesWithoutAKeyStillWork() throws Exception {
        record(DHUHR, null).andExpect(status().isCreated());
        assertEquals(1, outcomesToday());
    }

    @Test
    void processedWritesAreForgottenAfterTheRetentionAndOnlyThen() throws Exception {
        record(DHUHR, "recent").andExpect(status().isCreated());
        var old = UUID.randomUUID();
        jdbc.sql("INSERT INTO prayer.processed_command (message_id, user_id, command_type, fingerprint,"
                        + " response_status, processed_at) VALUES (:id, :user, 'prayer.create-outcome', 'f', 201,"
                        + " now() - interval '31 days')")
                .param("id", old)
                .param("user", user)
                .update();

        cleanup.deleteExpired();

        assertEquals(0, processedRows(old));
        assertEquals(1, processedRows(IdempotencyKeys.messageId(user, "recent")));
        record(DHUHR, "recent").andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));
    }

    private long processedRows(UUID messageId) {
        return jdbc.sql("SELECT count(*) FROM prayer.processed_command WHERE message_id = :id")
                .param("id", messageId)
                .query(Long.class)
                .single();
    }
}

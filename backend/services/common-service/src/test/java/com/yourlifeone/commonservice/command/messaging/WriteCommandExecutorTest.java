package com.yourlifeone.commonservice.command.messaging;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.WriteCommand;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommand;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommandRepository;
import com.yourlifeone.commonservice.web.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WriteCommandExecutorTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID USER = UUID.fromString("11111111-1111-4111-8111-111111111111");

    record Note(String text) {
    }

    /** Saves a note and counts how often it really ran. */
    static class SaveNote implements WriteCommandHandler<Note> {
        final AtomicInteger runs = new AtomicInteger();

        @Override
        public String type() {
            return "save-note";
        }

        @Override
        public Class<Note> payloadType() {
            return Note.class;
        }

        @Override
        public HttpStatus successStatus() {
            return HttpStatus.CREATED;
        }

        @Override
        public Object handle(UUID userId, Note payload) {
            runs.incrementAndGet();
            if (payload.text().isEmpty()) {
                throw ApiException.badRequest("empty_note", "A note needs text.");
            }
            return payload.text().equals("silent") ? null : new Note(payload.text() + " saved");
        }
    }

    @Mock
    private ProcessedCommandRepository processed;

    private final SaveNote saveNote = new SaveNote();
    private WriteCommandExecutor executor;

    @BeforeEach
    void executor() {
        executor = new WriteCommandExecutor(List.of(saveNote), processed, JSON);
    }

    private static WriteCommand command(String type, String text) {
        var payload = JSON.valueToTree(new Note(text));
        return WriteCommand.builder()
                .messageId(IdempotencyKeys.messageId(USER, "key-1"))
                .type(type)
                .userId(USER)
                .fingerprint(IdempotencyKeys.fingerprint(type, payload))
                .payload(payload)
                .build();
    }

    @Test
    void runsTheHandlerOnceAndStoresItsResult() {
        var command = command("save-note", "hello");
        when(processed.claim(command)).thenReturn(true);

        var reply = executor.execute(command);

        assertEquals(201, reply.status());
        assertEquals("hello saved", reply.body().get("text").asString());
        assertEquals(1, saveNote.runs.get());
        verify(processed).complete(command.messageId(), 201, "{\"text\":\"hello saved\"}");
    }

    @Test
    void aRedeliveredCommandReturnsTheStoredResultWithoutWritingAgain() {
        var command = command("save-note", "hello");
        when(processed.claim(command)).thenReturn(false);
        when(processed.find(command.messageId())).thenReturn(Optional.of(ProcessedCommand.builder()
                .fingerprint(command.fingerprint()).responseStatus(201).responseBody("{\"text\":\"hello saved\"}")
                .build()));

        var reply = executor.execute(command);

        assertEquals(201, reply.status());
        assertEquals("hello saved", reply.body().get("text").asString());
        assertEquals(0, saveNote.runs.get());
        verify(processed, never()).complete(any(), anyInt(), anyString());
    }

    @Test
    void aProcessedKeyWithADifferentRequestIsRefused() {
        var command = command("save-note", "changed");
        when(processed.claim(command)).thenReturn(false);
        when(processed.find(command.messageId())).thenReturn(Optional.of(ProcessedCommand.builder()
                .fingerprint("the-original-request").responseStatus(201).responseBody("{}").build()));

        var reply = executor.execute(command);

        assertEquals(422, reply.status());
        assertEquals("idempotency_key_reused", reply.errorCode());
        assertEquals(0, saveNote.runs.get());
    }

    @Test
    void aResponseWithoutABodyIsStoredAsNull() {
        var command = command("save-note", "silent");
        when(processed.claim(command)).thenReturn(true);

        var reply = executor.execute(command);

        assertNull(reply.body());
        verify(processed).complete(command.messageId(), 201, null);
    }

    @Test
    void aRefusedWritePropagatesSoTheTransactionRecordsNothing() {
        var command = command("save-note", "");
        when(processed.claim(command)).thenReturn(true);

        var error = assertThrows(ApiException.class, () -> executor.execute(command));

        assertEquals("empty_note", error.code());
        verify(processed, never()).complete(any(), anyInt(), anyString());
    }

    @Test
    void anUnknownCommandTypeIsRefused() {
        var command = command("delete-everything", "x");
        when(processed.claim(command)).thenReturn(true);

        var error = assertThrows(ApiException.class, () -> executor.execute(command));

        assertEquals("unknown_command", error.code());
    }
}

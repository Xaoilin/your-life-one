package com.yourlifeone.commonservice.command.messaging;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.WriteCommand;
import com.yourlifeone.commonservice.command.WriteCommandProperties;
import com.yourlifeone.commonservice.command.WriteCommandReply;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommand;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommandRepository;
import com.yourlifeone.commonservice.web.ApiException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WriteCommandGatewayTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID USER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String TYPE = "save-note";
    private static final Note NOTE = new Note("hello");

    record Note(String text) {
    }

    @Mock
    private RabbitTemplate rabbit;

    @Mock
    private ProcessedCommandRepository processed;

    private WriteCommandGateway gateway;

    @BeforeEach
    void gateway() {
        var properties = new WriteCommandProperties("yourlife.test.commands", "test.processed_command", Duration.ofSeconds(3), 1, null);
        gateway = new WriteCommandGateway(rabbit, processed, properties, JSON);
    }

    @SuppressWarnings("unchecked")
    private void replyWith(WriteCommandReply reply) {
        when(rabbit.convertSendAndReceiveAsType(eq(""), eq("yourlife.test.commands"), any(WriteCommand.class),
                any(MessagePostProcessor.class), any(ParameterizedTypeReference.class))).thenReturn(reply);
    }

    @SuppressWarnings("unchecked")
    private WriteCommand published() {
        var command = ArgumentCaptor.forClass(WriteCommand.class);
        verify(rabbit).convertSendAndReceiveAsType(eq(""), eq("yourlife.test.commands"), command.capture(),
                any(MessagePostProcessor.class), any(ParameterizedTypeReference.class));
        return command.getValue();
    }

    @Test
    void publishesTheWriteAndReturnsTheConsumersResult() {
        replyWith(WriteCommandReplies.success(HttpStatus.CREATED, JSON.valueToTree(new Note("saved"))));

        var result = gateway.send(TYPE, USER, "key-1", NOTE, Note.class);

        assertEquals(HttpStatus.CREATED, result.status());
        assertEquals(new Note("saved"), result.body());
        assertFalse(result.replayed());
        var command = published();
        assertEquals(IdempotencyKeys.messageId(USER, "key-1"), command.messageId());
        assertEquals(TYPE, command.type());
        assertEquals(USER, command.userId());
        assertEquals("hello", command.payload().get("text").asString());
    }

    @Test
    void replaysACompletedRequestWithoutPublishingAgain() {
        var fingerprint = IdempotencyKeys.fingerprint(TYPE, JSON.valueToTree(NOTE));
        when(processed.find(IdempotencyKeys.messageId(USER, "key-1"))).thenReturn(Optional.of(ProcessedCommand.builder()
                .fingerprint(fingerprint).responseStatus(201).responseBody("{\"text\":\"saved\"}").build()));

        var result = gateway.send(TYPE, USER, "key-1", NOTE, Note.class);

        assertEquals(HttpStatus.CREATED, result.status());
        assertEquals(new Note("saved"), result.body());
        assertTrue(result.replayed());
        verifyNoInteractions(rabbit);
    }

    @Test
    void refusesTheSameKeyForADifferentRequest() {
        when(processed.find(IdempotencyKeys.messageId(USER, "key-1"))).thenReturn(Optional.of(ProcessedCommand.builder()
                .fingerprint("another-request").responseStatus(201).responseBody("{}").build()));

        var error = assertThrows(ApiException.class, () -> gateway.send(TYPE, USER, "key-1", NOTE, Note.class));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, error.status());
        assertEquals("idempotency_key_reused", error.code());
        verifyNoInteractions(rabbit);
    }

    @Test
    void withoutAKeyEveryCallIsANewWrite() {
        replyWith(WriteCommandReplies.success(HttpStatus.OK, null));

        var result = gateway.send(TYPE, USER, null, NOTE, Note.class);

        assertNull(result.body());
        verify(processed, never()).find(any());
        var first = published().messageId();
        assertEquals(4, first.version());
    }

    @Test
    void aRefusedWriteBecomesTheSameApiError() {
        replyWith(WriteCommandReplies.failure(HttpStatus.CONFLICT, "prayer_not_started", "Fajr has not started yet."));

        var error = assertThrows(ApiException.class, () -> gateway.send(TYPE, USER, "key-1", NOTE, Note.class));

        assertEquals(HttpStatus.CONFLICT, error.status());
        assertEquals("prayer_not_started", error.code());
        assertEquals("Fajr has not started yet.", error.getMessage());
    }

    @Test
    void noReplyInTimeIsARetryableServiceUnavailable() {
        replyWith(null);

        var error = assertThrows(ApiException.class, () -> gateway.send(TYPE, USER, "key-1", NOTE, Note.class));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.status());
        assertEquals("write_timeout", error.code());
    }

    @Test
    @SuppressWarnings("unchecked")
    void anUnreachableBrokerIsARetryableServiceUnavailable() {
        when(rabbit.convertSendAndReceiveAsType(eq(""), eq("yourlife.test.commands"), any(WriteCommand.class),
                any(MessagePostProcessor.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new AmqpConnectException(new RuntimeException("connection refused")));

        var error = assertThrows(ApiException.class, () -> gateway.send(TYPE, USER, "key-1", NOTE, Note.class));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.status());
        assertEquals("write_queue_unavailable", error.code());
    }

    @Test
    void anInvalidKeyIsRefusedBeforeAnythingIsSent() {
        var error = assertThrows(ApiException.class, () -> gateway.send(TYPE, USER, "has space", NOTE, Note.class));

        assertEquals("invalid_idempotency_key", error.code());
        verifyNoInteractions(rabbit, processed);
    }

    @Test
    @SuppressWarnings("unchecked")
    void theMessageCarriesItsIdAndExpiresWithTheRequest() {
        replyWith(WriteCommandReplies.success(HttpStatus.OK, null));
        gateway.send(TYPE, USER, "key-1", NOTE, Note.class);
        var postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertSendAndReceiveAsType(eq(""), eq("yourlife.test.commands"), any(WriteCommand.class),
                postProcessor.capture(), any(ParameterizedTypeReference.class));

        var message = postProcessor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));

        assertEquals(IdempotencyKeys.messageId(USER, "key-1").toString(), message.getMessageProperties().getMessageId());
        assertEquals("3000", message.getMessageProperties().getExpiration());
    }

    @Test
    void aWriteWithFetchedContextIsFingerprintedByTheRequestAlone() {
        replyWith(WriteCommandReplies.success(HttpStatus.OK, null));

        gateway.sendWithContext(TYPE, USER, "key-1", NOTE, () -> new Note("hello, with context"), Note.class);

        var command = published();
        assertEquals(IdempotencyKeys.fingerprint(TYPE, JSON.valueToTree(NOTE)), command.fingerprint());
        assertEquals("hello, with context", command.payload().get("text").asString());
    }

    @Test
    void aReplayedWriteWithContextNeverFetchesTheContext() {
        var fingerprint = IdempotencyKeys.fingerprint(TYPE, JSON.valueToTree(NOTE));
        when(processed.find(IdempotencyKeys.messageId(USER, "key-1"))).thenReturn(Optional.of(ProcessedCommand.builder()
                .fingerprint(fingerprint).responseStatus(200).responseBody("{\"text\":\"saved\"}").build()));

        var result = gateway.sendWithContext(TYPE, USER, "key-1", NOTE, () -> {
            throw new AssertionError("the context must not be fetched for a replay");
        }, Note.class);

        assertTrue(result.replayed());
        verifyNoInteractions(rabbit);
    }
}

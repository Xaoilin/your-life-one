package com.yourlifeone.commonservice.command.messaging;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.WriteCommand;
import com.yourlifeone.commonservice.command.WriteCommandProperties;
import com.yourlifeone.commonservice.command.WriteCommandReply;
import com.yourlifeone.commonservice.command.WriteCommandResult;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommand;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommandRepository;
import com.yourlifeone.commonservice.web.ApiException;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The API side of a write: replays a completed request with the same idempotency key, otherwise
 * publishes the write to the service's queue and waits for the consumer's reply.
 */
public class WriteCommandGateway {
    private static final ParameterizedTypeReference<WriteCommandReply> REPLY = new ParameterizedTypeReference<>() {
    };

    private final RabbitTemplate rabbit;
    private final ProcessedCommandRepository processed;
    private final WriteCommandProperties properties;
    private final JsonMapper json;

    public WriteCommandGateway(RabbitTemplate rabbit, ProcessedCommandRepository processed,
            WriteCommandProperties properties, JsonMapper json) {
        this.rabbit = rabbit;
        this.processed = processed;
        this.properties = properties;
        this.json = json;
    }

    /**
     * @param idempotencyKey the caller's {@code Idempotency-Key}; null means every call is a new write
     * @throws ApiException when the write is refused, or 503 when it could not be confirmed in time
     */
    public <T> WriteCommandResult<T> send(String type, UUID userId, String idempotencyKey, Object payload,
            Class<T> responseType) {
        JsonNode payloadJson = json.valueToTree(payload);
        return dispatch(type, userId, idempotencyKey, payloadJson, () -> payloadJson, responseType);
    }

    /**
     * Sends a write whose command carries, besides the caller's request, data the API fetched for it (for
     * example from another service, so the consumer never reads that service's tables). The idempotency
     * fingerprint covers only {@code request}, so a retry is still recognised when that data has changed
     * since, and {@code payload} is built (and the data fetched) only when the write is not a replay.
     *
     * @param request what the caller asked for
     * @param payload builds the handler's input
     */
    public <T> WriteCommandResult<T> sendWithContext(String type, UUID userId, String idempotencyKey, Object request,
            Supplier<?> payload, Class<T> responseType) {
        return dispatch(type, userId, idempotencyKey, json.valueToTree(request), () -> json.valueToTree(payload.get()),
                responseType);
    }

    private <T> WriteCommandResult<T> dispatch(String type, UUID userId, String idempotencyKey, JsonNode request,
            Supplier<JsonNode> payload, Class<T> responseType) {
        var messageId = messageId(userId, idempotencyKey);
        var fingerprint = IdempotencyKeys.fingerprint(type, request);
        if (idempotencyKey != null) {
            var stored = processed.find(messageId);
            if (stored.isPresent()) {
                return replay(stored.get(), fingerprint, responseType);
            }
        }
        var command = WriteCommand.builder()
                .messageId(messageId)
                .type(type)
                .userId(userId)
                .fingerprint(fingerprint)
                .payload(payload.get())
                .build();
        return result(publish(command), responseType, false);
    }

    private static UUID messageId(UUID userId, String idempotencyKey) {
        return idempotencyKey == null
                ? UUID.randomUUID()
                : IdempotencyKeys.messageId(userId, IdempotencyKeys.validated(idempotencyKey));
    }

    private <T> WriteCommandResult<T> replay(ProcessedCommand stored, String fingerprint, Class<T> responseType) {
        if (!stored.fingerprint().equals(fingerprint)) {
            return result(WriteCommandExecutor.idempotencyKeyReused(), responseType, true);
        }
        var body = stored.responseBody() == null ? null : json.readTree(stored.responseBody());
        return result(WriteCommandReplies.success(HttpStatus.valueOf(stored.responseStatus()), body), responseType, true);
    }

    private WriteCommandReply publish(WriteCommand command) {
        WriteCommandReply reply;
        try {
            reply = rabbit.convertSendAndReceiveAsType("", properties.queue(), command,
                    message -> withIdentity(message, command), REPLY);
        } catch (AmqpException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "write_queue_unavailable",
                    "Saving is unavailable right now; try again.", exception.getMessage(), exception);
        }
        if (reply == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "write_timeout",
                    "The save was not confirmed in time; retry with the same Idempotency-Key.");
        }
        return reply;
    }

    /** An unconsumed write expires with its request, so it can never be applied after the caller gave up. */
    private Message withIdentity(Message message, WriteCommand command) {
        message.getMessageProperties().setMessageId(command.messageId().toString());
        message.getMessageProperties().setExpiration(String.valueOf(properties.replyTimeout().toMillis()));
        return message;
    }

    private <T> WriteCommandResult<T> result(WriteCommandReply reply, Class<T> responseType, boolean replayed) {
        if (reply.failed()) {
            throw new ApiException(HttpStatus.valueOf(reply.status()), reply.errorCode(), reply.errorMessage());
        }
        var body = reply.body() == null ? null : json.treeToValue(reply.body(), responseType);
        return new WriteCommandResult<>(HttpStatus.valueOf(reply.status()), body, replayed);
    }
}

package com.yourlifeone.commonservice.command.messaging;

import com.yourlifeone.commonservice.command.WriteCommand;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.commonservice.command.WriteCommandReply;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommand;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommandRepository;
import com.yourlifeone.commonservice.web.ApiException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Performs a write exactly once: the command is claimed and its handler runs in one transaction,
 * so a redelivered or retried command returns the stored result instead of writing again.
 */
public class WriteCommandExecutor {
    private final Map<String, WriteCommandHandler<?>> handlers;
    private final ProcessedCommandRepository processed;
    private final JsonMapper json;

    public WriteCommandExecutor(List<WriteCommandHandler<?>> handlers, ProcessedCommandRepository processed,
            JsonMapper json) {
        this.handlers = handlers.stream().collect(Collectors.toMap(WriteCommandHandler::type, Function.identity()));
        this.processed = processed;
        this.json = json;
    }

    /** Throws {@link ApiException} for a refused write; the transaction then records nothing. */
    @Transactional
    public WriteCommandReply execute(WriteCommand command) {
        if (!processed.claim(command)) {
            return storedReply(command);
        }
        var handler = handlerFor(command.type());
        var body = json.valueToTree(perform(handler, command));
        var reply = WriteCommandReplies.success(handler.successStatus(), body.isNull() ? null : body);
        processed.complete(command.messageId(), reply.status(), reply.body() == null ? null : reply.body().toString());
        return reply;
    }

    private <P> Object perform(WriteCommandHandler<P> handler, WriteCommand command) {
        var payload = json.treeToValue(command.payload(), handler.payloadType());
        return handler.handle(command.userId(), payload);
    }

    private WriteCommandHandler<?> handlerFor(String type) {
        var handler = handlers.get(type);
        if (handler == null) {
            throw ApiException.badRequest("unknown_command", "This write is not supported.");
        }
        return handler;
    }

    private WriteCommandReply storedReply(WriteCommand command) {
        var stored = processed.find(command.messageId())
                .orElseThrow(() -> new IllegalStateException("claimed command has no stored result"));
        if (!stored.fingerprint().equals(command.fingerprint())) {
            return idempotencyKeyReused();
        }
        return WriteCommandReplies.success(HttpStatus.valueOf(stored.responseStatus()), storedBody(stored));
    }

    private JsonNode storedBody(ProcessedCommand stored) {
        return stored.responseBody() == null ? null : json.readTree(stored.responseBody());
    }

    static WriteCommandReply idempotencyKeyReused() {
        return WriteCommandReplies.failure(HttpStatus.UNPROCESSABLE_CONTENT, "idempotency_key_reused",
                "This Idempotency-Key was already used for a different request.");
    }
}

package com.yourlifeone.knowledge.shared;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.http.HttpStatus;

/**
 * A write the command queue performs, declared as a bean in each domain's command-handler
 * configuration.
 *
 * @param successStatus the status of a successful response
 * @param run performs the write for the user and returns the response body (null for none)
 */
public record CommandHandler<P>(String type, Class<P> payloadType, HttpStatus successStatus,
        BiFunction<UUID, P, Object> run) implements WriteCommandHandler<P> {
    public static <P> CommandHandler<P> ok(String type, Class<P> payloadType, BiFunction<UUID, P, Object> run) {
        return new CommandHandler<>(type, payloadType, HttpStatus.OK, run);
    }

    public static <P> CommandHandler<P> noContent(String type, Class<P> payloadType, BiFunction<UUID, P, Object> run) {
        return new CommandHandler<>(type, payloadType, HttpStatus.NO_CONTENT, run);
    }

    @Override
    public Object handle(UUID userId, P payload) {
        return run.apply(userId, payload);
    }
}

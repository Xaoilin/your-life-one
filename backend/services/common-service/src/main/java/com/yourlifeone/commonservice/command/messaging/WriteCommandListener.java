package com.yourlifeone.commonservice.command.messaging;

import com.yourlifeone.commonservice.changes.ChangePublisher;
import com.yourlifeone.commonservice.command.WriteCommand;
import com.yourlifeone.commonservice.command.WriteCommandReply;
import com.yourlifeone.commonservice.web.ApiException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * Consumes this service's write commands and replies with the outcome. Every command gets a reply,
 * so a failure never leaves a message redelivering forever; callers retry with the same key.
 * Counts each write as {@code yourlife.writes} by command and outcome: how much each part of the app is used.
 * A saved write is announced as a change event, so the user's other open tabs reload it.
 */
public class WriteCommandListener {
    private static final Logger logger = LoggerFactory.getLogger(WriteCommandListener.class);
    private final WriteCommandExecutor executor;
    private final MeterRegistry meters;
    private final ChangePublisher changes;

    public WriteCommandListener(WriteCommandExecutor executor, MeterRegistry meters) {
        this(executor, meters, null);
    }

    /** @param changes announces saved writes; null announces nothing */
    public WriteCommandListener(WriteCommandExecutor executor, MeterRegistry meters, ChangePublisher changes) {
        this.executor = executor;
        this.meters = meters;
        this.changes = changes;
    }

    @RabbitListener(queues = "${app.write-commands.queue}", concurrency = "${app.write-commands.consumers:1}")
    public WriteCommandReply receive(WriteCommand command) {
        var reply = perform(command);
        count(command, reply);
        if (!reply.failed() && changes != null) {
            changes.publish(command.userId(), command.type());
        }
        return reply;
    }

    private WriteCommandReply perform(WriteCommand command) {
        try {
            return executor.execute(command);
        } catch (ApiException exception) {
            return WriteCommandReplies.failure(exception);
        } catch (DataAccessResourceFailureException | TransientDataAccessException
                | CannotCreateTransactionException exception) {
            logger.error("Write command failed: type={} reason=database_unavailable", command.type(), exception);
            return WriteCommandReplies.failure(HttpStatus.SERVICE_UNAVAILABLE, "database_unavailable",
                    "The database is unavailable.");
        } catch (RuntimeException exception) {
            logger.error("Write command failed: type={} reason=unexpected", command.type(), exception);
            return WriteCommandReplies.failure(HttpStatus.INTERNAL_SERVER_ERROR, "write_failed",
                    "The write could not be completed.");
        }
    }

    /** saved, refused (4xx: the user's write was not allowed) or failed (5xx). */
    private void count(WriteCommand command, WriteCommandReply reply) {
        var outcome = !reply.failed() ? "saved" : reply.status() < 500 ? "refused" : "failed";
        Counter.builder("yourlife.writes")
                .description("Writes by command and outcome")
                .tag("command", String.valueOf(command.type()))
                .tag("outcome", outcome)
                .register(meters)
                .increment();
    }
}

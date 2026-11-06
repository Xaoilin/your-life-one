package com.yourlifeone.commonservice.command.messaging;

import com.yourlifeone.commonservice.changes.ChangePublisher;
import com.yourlifeone.commonservice.command.WriteCommand;
import com.yourlifeone.commonservice.web.ApiException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Every command gets a reply, so no failure leaves a message redelivering forever. */
@ExtendWith(MockitoExtension.class)
class WriteCommandListenerTest {
    private static final UUID USER = UUID.randomUUID();
    private static final WriteCommand COMMAND = WriteCommand.builder().type("save-note").userId(USER).build();

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @Mock
    private WriteCommandExecutor executor;

    @Mock
    private ChangePublisher changes;

    private WriteCommandListener listener;

    @BeforeEach
    void setUp() {
        listener = new WriteCommandListener(executor, meters, changes);
    }

    private double writes(String outcome) {
        var counter = meters.find("yourlife.writes").tags("command", "save-note", "outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void countsEachWriteByCommandAndOutcome() {
        when(executor.execute(COMMAND)).thenReturn(WriteCommandReplies.success(HttpStatus.OK, null))
                .thenThrow(ApiException.conflict("outcome_exists", "Already recorded."))
                .thenThrow(new DataAccessResourceFailureException("down"));

        listener.receive(COMMAND);
        listener.receive(COMMAND);
        listener.receive(COMMAND);

        assertEquals(1, writes("saved"));
        assertEquals(1, writes("refused"));
        assertEquals(1, writes("failed"));
    }

    @Test
    void passesTheExecutorsReplyThrough() {
        var reply = WriteCommandReplies.success(HttpStatus.OK, null);
        when(executor.execute(COMMAND)).thenReturn(reply);

        assertEquals(reply, listener.receive(COMMAND));
    }

    @Test
    void aRefusedWriteRepliesWithItsApiError() {
        when(executor.execute(COMMAND)).thenThrow(ApiException.conflict("outcome_exists", "Already recorded."));

        var reply = listener.receive(COMMAND);

        assertEquals(409, reply.status());
        assertEquals("outcome_exists", reply.errorCode());
        assertEquals("Already recorded.", reply.errorMessage());
    }

    @Test
    void anUnreachableDatabaseRepliesServiceUnavailable() {
        when(executor.execute(COMMAND)).thenThrow(new DataAccessResourceFailureException("down"));

        assertEquals("database_unavailable", listener.receive(COMMAND).errorCode());
    }

    @Test
    void noTransactionRepliesServiceUnavailable() {
        when(executor.execute(COMMAND)).thenThrow(new CannotCreateTransactionException("no connection"));

        assertEquals(503, listener.receive(COMMAND).status());
    }

    @Test
    void anyOtherFailureRepliesWithAGenericError() {
        when(executor.execute(COMMAND)).thenThrow(new DataIntegrityViolationException("bug"));

        var reply = listener.receive(COMMAND);

        assertEquals(500, reply.status());
        assertEquals("write_failed", reply.errorCode());
    }

    @Test
    void announcesASavedWriteAsAChange() {
        when(executor.execute(COMMAND)).thenReturn(WriteCommandReplies.success(HttpStatus.OK, null));

        listener.receive(COMMAND);

        verify(changes).publish(USER, "save-note");
    }

    @Test
    void aRefusedWriteChangesNothingToAnnounce() {
        when(executor.execute(COMMAND)).thenThrow(ApiException.conflict("outcome_exists", "Already recorded."));

        listener.receive(COMMAND);

        verify(changes, never()).publish(USER, "save-note");
    }
}

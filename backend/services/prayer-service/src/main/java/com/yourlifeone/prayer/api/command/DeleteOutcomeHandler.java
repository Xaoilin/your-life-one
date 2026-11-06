package com.yourlifeone.prayer.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.prayer.application.service.OutcomeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Removes an outcome, including its reward receipt, from the command queue. */
@Component
@RequiredArgsConstructor
public class DeleteOutcomeHandler implements WriteCommandHandler<DeleteOutcomeCommand> {
    public static final String TYPE = "prayer.delete-outcome";

    private final OutcomeService outcomes;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<DeleteOutcomeCommand> payloadType() {
        return DeleteOutcomeCommand.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.NO_CONTENT;
    }

    @Override
    public Object handle(UUID userId, DeleteOutcomeCommand command) {
        outcomes.delete(userId, command.outcomeId());
        return null;
    }
}

package com.yourlifeone.prayer.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.prayer.api.mapper.PrayerResponseMapper;
import com.yourlifeone.prayer.application.service.OutcomeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Corrects an outcome's status from the command queue. */
@Component
@RequiredArgsConstructor
public class CorrectOutcomeHandler implements WriteCommandHandler<CorrectOutcomeCommand> {
    public static final String TYPE = "prayer.correct-outcome";

    private final OutcomeService outcomes;
    private final PrayerResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<CorrectOutcomeCommand> payloadType() {
        return CorrectOutcomeCommand.class;
    }

    @Override
    public Object handle(UUID userId, CorrectOutcomeCommand command) {
        return responses.toResponse(outcomes.correct(userId, command.outcomeId(), command.status(), command.source(), command.timing()));
    }
}

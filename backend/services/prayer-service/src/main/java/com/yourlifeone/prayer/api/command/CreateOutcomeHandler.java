package com.yourlifeone.prayer.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.prayer.api.mapper.PrayerResponseMapper;
import com.yourlifeone.prayer.api.request.CreateOutcomeRequest;
import com.yourlifeone.prayer.application.service.OutcomeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Records a prayer outcome from the command queue. */
@Component
@RequiredArgsConstructor
public class CreateOutcomeHandler implements WriteCommandHandler<CreateOutcomeRequest> {
    public static final String TYPE = "prayer.create-outcome";

    private final OutcomeService outcomes;
    private final PrayerResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<CreateOutcomeRequest> payloadType() {
        return CreateOutcomeRequest.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.CREATED;
    }

    @Override
    public Object handle(UUID userId, CreateOutcomeRequest request) {
        var change = outcomes.record(userId, request.date(), request.prayer(), request.status(), request.source(),
                request.taskId(), request.timing());
        return responses.toResponse(change);
    }
}

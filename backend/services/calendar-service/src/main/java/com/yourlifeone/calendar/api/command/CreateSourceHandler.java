package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.mapper.CalendarResponseMapper;
import com.yourlifeone.calendar.api.request.CreateSourceRequest;
import com.yourlifeone.calendar.application.service.SourceService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Adds a local calendar to a local account. */
@Component
@RequiredArgsConstructor
public class CreateSourceHandler implements WriteCommandHandler<CreateSourceRequest> {
    public static final String TYPE = "calendar.create-source";

    private final SourceService sources;
    private final CalendarResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<CreateSourceRequest> payloadType() {
        return CreateSourceRequest.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.CREATED;
    }

    @Override
    public Object handle(UUID userId, CreateSourceRequest command) {
        return responses.toResponse(sources.createLocal(userId, command.accountId(), command.name(), command.color(),
                command.visible()));
    }
}

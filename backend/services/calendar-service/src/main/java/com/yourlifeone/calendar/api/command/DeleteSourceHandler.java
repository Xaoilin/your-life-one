package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.application.service.SourceService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Deletes a local calendar and its events. */
@Component
@RequiredArgsConstructor
public class DeleteSourceHandler implements WriteCommandHandler<DeleteSourceCommand> {
    public static final String TYPE = "calendar.delete-source";

    private final SourceService sources;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<DeleteSourceCommand> payloadType() {
        return DeleteSourceCommand.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.NO_CONTENT;
    }

    @Override
    public Object handle(UUID userId, DeleteSourceCommand command) {
        sources.delete(userId, command.sourceId());
        return null;
    }
}

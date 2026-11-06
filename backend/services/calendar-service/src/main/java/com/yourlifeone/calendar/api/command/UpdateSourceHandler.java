package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.mapper.CalendarResponseMapper;
import com.yourlifeone.calendar.application.service.SourceService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Changes a calendar's colour or visibility, or renames or moves a local calendar. */
@Component
@RequiredArgsConstructor
public class UpdateSourceHandler implements WriteCommandHandler<UpdateSourceCommand> {
    public static final String TYPE = "calendar.update-source";

    private final SourceService sources;
    private final CalendarResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<UpdateSourceCommand> payloadType() {
        return UpdateSourceCommand.class;
    }

    @Override
    public Object handle(UUID userId, UpdateSourceCommand command) {
        var changes = command.changes();
        return responses.toResponse(sources.update(userId, command.sourceId(), changes.name(), changes.color(),
                changes.visible(), changes.accountId()));
    }
}

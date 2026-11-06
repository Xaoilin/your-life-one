package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.application.service.EventService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Deletes an event, from Google first when it is on a Google calendar. */
@Component
@RequiredArgsConstructor
public class DeleteEventHandler implements WriteCommandHandler<DeleteEventCommand> {
    public static final String TYPE = "calendar.delete-event";

    private final EventService events;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<DeleteEventCommand> payloadType() {
        return DeleteEventCommand.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.NO_CONTENT;
    }

    @Override
    public Object handle(UUID userId, DeleteEventCommand command) {
        events.delete(userId, command.eventId());
        return null;
    }
}

package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.mapper.CalendarResponseMapper;
import com.yourlifeone.calendar.api.mapper.EventRequestMapper;
import com.yourlifeone.calendar.application.service.EventService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Changes an event, in Google first when it is on a Google calendar. */
@Component
@RequiredArgsConstructor
public class UpdateEventHandler implements WriteCommandHandler<UpdateEventCommand> {
    public static final String TYPE = "calendar.update-event";

    private final EventService events;
    private final EventRequestMapper requests;
    private final CalendarResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<UpdateEventCommand> payloadType() {
        return UpdateEventCommand.class;
    }

    @Override
    public Object handle(UUID userId, UpdateEventCommand command) {
        return responses.toResponse(events.update(userId, command.eventId(), requests.toDetails(command.event())));
    }
}

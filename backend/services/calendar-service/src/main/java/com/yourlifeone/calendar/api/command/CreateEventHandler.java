package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.mapper.CalendarResponseMapper;
import com.yourlifeone.calendar.api.mapper.EventRequestMapper;
import com.yourlifeone.calendar.api.request.EventRequest;
import com.yourlifeone.calendar.application.service.EventService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Creates an event, in Google first when the calendar is a Google calendar. */
@Component
@RequiredArgsConstructor
public class CreateEventHandler implements WriteCommandHandler<EventRequest> {
    public static final String TYPE = "calendar.create-event";

    private final EventService events;
    private final EventRequestMapper requests;
    private final CalendarResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<EventRequest> payloadType() {
        return EventRequest.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.CREATED;
    }

    @Override
    public Object handle(UUID userId, EventRequest command) {
        return responses.toResponse(events.create(userId, requests.toDetails(command)));
    }
}

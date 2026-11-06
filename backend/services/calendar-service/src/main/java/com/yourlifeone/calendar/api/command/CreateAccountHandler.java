package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.mapper.CalendarResponseMapper;
import com.yourlifeone.calendar.api.request.CreateAccountRequest;
import com.yourlifeone.calendar.application.service.AccountService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Adds a local account with its first calendar. */
@Component
@RequiredArgsConstructor
public class CreateAccountHandler implements WriteCommandHandler<CreateAccountRequest> {
    public static final String TYPE = "calendar.create-account";

    private final AccountService accounts;
    private final CalendarResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<CreateAccountRequest> payloadType() {
        return CreateAccountRequest.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.CREATED;
    }

    @Override
    public Object handle(UUID userId, CreateAccountRequest request) {
        return responses.toResponse(accounts.createLocal(userId, request.name(), request.email(), request.paletteIndex()));
    }
}

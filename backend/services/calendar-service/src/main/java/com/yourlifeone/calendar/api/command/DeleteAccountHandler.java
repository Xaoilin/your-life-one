package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.application.service.AccountService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Deletes an account with its calendars and events. */
@Component
@RequiredArgsConstructor
public class DeleteAccountHandler implements WriteCommandHandler<DeleteAccountCommand> {
    public static final String TYPE = "calendar.delete-account";

    private final AccountService accounts;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<DeleteAccountCommand> payloadType() {
        return DeleteAccountCommand.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.NO_CONTENT;
    }

    @Override
    public Object handle(UUID userId, DeleteAccountCommand command) {
        accounts.delete(userId, command.accountId());
        return null;
    }
}

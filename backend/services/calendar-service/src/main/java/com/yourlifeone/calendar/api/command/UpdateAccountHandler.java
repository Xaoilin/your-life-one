package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.mapper.CalendarResponseMapper;
import com.yourlifeone.calendar.application.service.AccountService;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Renames an account, recolours it, or makes it primary. */
@Component
@RequiredArgsConstructor
public class UpdateAccountHandler implements WriteCommandHandler<UpdateAccountCommand> {
    public static final String TYPE = "calendar.update-account";

    private final AccountService accounts;
    private final CalendarResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<UpdateAccountCommand> payloadType() {
        return UpdateAccountCommand.class;
    }

    @Override
    public Object handle(UUID userId, UpdateAccountCommand command) {
        var changes = command.changes();
        return responses.toResponse(accounts.update(userId, command.accountId(), changes.name(), changes.primary(),
                changes.paletteIndex()));
    }
}

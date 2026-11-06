package com.yourlifeone.prayer.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.prayer.api.mapper.ReminderResponseMapper;
import com.yourlifeone.prayer.application.service.ReminderSnoozeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Snoozes a reminder from the command queue. */
@Component
@RequiredArgsConstructor
public class SnoozeReminderHandler implements WriteCommandHandler<SnoozeReminderCommand> {
    public static final String TYPE = "prayer.snooze-reminder";

    private final ReminderSnoozeService snoozes;
    private final ReminderResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<SnoozeReminderCommand> payloadType() {
        return SnoozeReminderCommand.class;
    }

    @Override
    public Object handle(UUID userId, SnoozeReminderCommand command) {
        return responses.toResponse(snoozes.snooze(userId, command.key()));
    }
}

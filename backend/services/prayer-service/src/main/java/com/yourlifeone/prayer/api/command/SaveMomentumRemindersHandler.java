package com.yourlifeone.prayer.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.prayer.api.mapper.ReminderResponseMapper;
import com.yourlifeone.prayer.api.request.MomentumRemindersRequest;
import com.yourlifeone.prayer.application.service.MomentumReminderService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves momentum reminder preferences from the command queue. */
@Component
@RequiredArgsConstructor
public class SaveMomentumRemindersHandler implements WriteCommandHandler<MomentumRemindersRequest> {
    public static final String TYPE = "prayer.save-momentum-reminders";

    private final MomentumReminderService momentum;
    private final ReminderResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<MomentumRemindersRequest> payloadType() {
        return MomentumRemindersRequest.class;
    }

    @Override
    public Object handle(UUID userId, MomentumRemindersRequest request) {
        return responses.toMomentumResponse(momentum.save(userId, responses.toDomain(request.pillars())));
    }
}

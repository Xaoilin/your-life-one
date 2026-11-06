package com.yourlifeone.prayer.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.prayer.api.mapper.PrayerResponseMapper;
import com.yourlifeone.prayer.api.request.PreferencesRequest;
import com.yourlifeone.prayer.application.service.PrayerProfileService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves prayer preferences from the command queue. */
@Component
@RequiredArgsConstructor
public class UpdatePreferencesHandler implements WriteCommandHandler<PreferencesRequest> {
    public static final String TYPE = "prayer.update-preferences";

    private final PrayerProfileService profiles;
    private final PrayerResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<PreferencesRequest> payloadType() {
        return PreferencesRequest.class;
    }

    @Override
    public Object handle(UUID userId, PreferencesRequest request) {
        var profile = profiles.updatePreferences(userId, request.enabled(), request.reminderEnabled(),
                request.reminderMinutes());
        return responses.toPreferences(profile);
    }
}

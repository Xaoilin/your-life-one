package com.yourlifeone.profile.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.profile.api.mapper.GlobalSettingsResponseMapper;
import com.yourlifeone.profile.api.request.GlobalSettingsRequest;
import com.yourlifeone.profile.application.service.GlobalSettingsService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves global settings from the command queue. */
@Component
@RequiredArgsConstructor
public class UpdateGlobalSettingsHandler implements WriteCommandHandler<GlobalSettingsRequest> {
    public static final String TYPE = "profile.update-settings";

    private final GlobalSettingsService settings;
    private final GlobalSettingsResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<GlobalSettingsRequest> payloadType() {
        return GlobalSettingsRequest.class;
    }

    @Override
    public Object handle(UUID userId, GlobalSettingsRequest request) {
        var saved = settings.update(userId, request.city(), request.country(), request.timeZone());
        return responses.toResponse(saved);
    }
}

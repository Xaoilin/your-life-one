package com.yourlifeone.profile.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.profile.api.mapper.AppPreferencesMapper;
import com.yourlifeone.profile.api.request.AppPreferencesRequest;
import com.yourlifeone.profile.application.service.AppPreferencesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves app preferences from the command queue. */
@Component
@RequiredArgsConstructor
public class UpdateAppPreferencesHandler implements WriteCommandHandler<AppPreferencesRequest> {
    public static final String TYPE = "profile.update-preferences";

    private final AppPreferencesService preferences;
    private final AppPreferencesMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<AppPreferencesRequest> payloadType() {
        return AppPreferencesRequest.class;
    }

    @Override
    public Object handle(UUID userId, AppPreferencesRequest request) {
        return mapper.toResponse(preferences.update(userId, mapper.toDomain(request)));
    }
}

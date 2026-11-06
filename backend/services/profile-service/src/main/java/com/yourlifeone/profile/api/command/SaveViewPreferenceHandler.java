package com.yourlifeone.profile.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.profile.api.mapper.ViewPreferenceMapper;
import com.yourlifeone.profile.application.service.ViewPreferencesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaveViewPreferenceHandler implements WriteCommandHandler<SaveViewPreferenceCommand> {
    public static final String TYPE = "profile.save-view-preference";
    private final ViewPreferencesService preferences;
    private final ViewPreferenceMapper mapper;

    @Override
    public String type() { return TYPE; }

    @Override
    public Class<SaveViewPreferenceCommand> payloadType() { return SaveViewPreferenceCommand.class; }

    @Override
    public Object handle(UUID userId, SaveViewPreferenceCommand request) {
        return mapper.toResponse(preferences.save(userId, request.key(), request.value()));
    }
}

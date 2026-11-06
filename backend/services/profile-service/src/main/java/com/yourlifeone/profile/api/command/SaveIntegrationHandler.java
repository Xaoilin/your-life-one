package com.yourlifeone.profile.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.profile.api.mapper.IntegrationResponseMapper;
import com.yourlifeone.profile.application.service.IntegrationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves an integration record from the command queue. */
@Component
@RequiredArgsConstructor
public class SaveIntegrationHandler implements WriteCommandHandler<SaveIntegrationCommand> {
    public static final String TYPE = "profile.save-integration";

    private final IntegrationService integrations;
    private final IntegrationResponseMapper responses;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<SaveIntegrationCommand> payloadType() {
        return SaveIntegrationCommand.class;
    }

    @Override
    public Object handle(UUID userId, SaveIntegrationCommand command) {
        return responses.toResponse(integrations.save(userId, command.provider(), command.status(),
                command.configuredAt(), command.lastError()));
    }
}

package com.yourlifeone.life.health.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.health.api.mapper.FastFoodMapper;
import com.yourlifeone.life.health.application.service.FastFoodService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves a journal entry from the command queue. */
@Component
@RequiredArgsConstructor
public class SaveFastFoodEntryHandler implements WriteCommandHandler<SaveFastFoodEntryCommand> {
    public static final String TYPE = "health.save-fast-food-entry";

    private final FastFoodService entries;
    private final FastFoodMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<SaveFastFoodEntryCommand> payloadType() {
        return SaveFastFoodEntryCommand.class;
    }

    @Override
    public Object handle(UUID userId, SaveFastFoodEntryCommand command) {
        return mapper.toResponse(entries.save(userId, mapper.toDomain(command.id(), command.entry())));
    }
}

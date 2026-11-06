package com.yourlifeone.life.inventory.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.inventory.api.mapper.InventoryMapper;
import com.yourlifeone.life.inventory.application.service.InventoryNeedService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves one whole need from the command queue. */
@Component
@RequiredArgsConstructor
public class SaveNeedHandler implements WriteCommandHandler<SaveNeedCommand> {
    public static final String TYPE = "inventory.save-need";

    private final InventoryNeedService needs;
    private final InventoryMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<SaveNeedCommand> payloadType() {
        return SaveNeedCommand.class;
    }

    @Override
    public Object handle(UUID userId, SaveNeedCommand command) {
        return mapper.toResponse(needs.save(userId, mapper.toDomain(command.id(), command.need())));
    }
}

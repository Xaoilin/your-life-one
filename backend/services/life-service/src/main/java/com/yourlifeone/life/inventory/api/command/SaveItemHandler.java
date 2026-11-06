package com.yourlifeone.life.inventory.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.inventory.api.mapper.InventoryMapper;
import com.yourlifeone.life.inventory.application.service.InventoryItemService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Saves one whole item from the command queue. */
@Component
@RequiredArgsConstructor
public class SaveItemHandler implements WriteCommandHandler<SaveItemCommand> {
    public static final String TYPE = "inventory.save-item";

    private final InventoryItemService items;
    private final InventoryMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<SaveItemCommand> payloadType() {
        return SaveItemCommand.class;
    }

    @Override
    public Object handle(UUID userId, SaveItemCommand command) {
        return mapper.toResponse(items.save(userId, mapper.toDomain(command.id(), command.item())));
    }
}

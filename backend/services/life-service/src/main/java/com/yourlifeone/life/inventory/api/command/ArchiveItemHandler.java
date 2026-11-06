package com.yourlifeone.life.inventory.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.inventory.api.mapper.InventoryMapper;
import com.yourlifeone.life.inventory.application.service.InventoryItemService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Archives an item from the command queue; archiving an archived item keeps its first time. */
@Component
@RequiredArgsConstructor
public class ArchiveItemHandler implements WriteCommandHandler<ArchiveItemCommand> {
    public static final String TYPE = "inventory.archive-item";

    private final InventoryItemService items;
    private final InventoryMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<ArchiveItemCommand> payloadType() {
        return ArchiveItemCommand.class;
    }

    @Override
    public Object handle(UUID userId, ArchiveItemCommand command) {
        return mapper.toResponse(items.archive(userId, command.id()));
    }
}

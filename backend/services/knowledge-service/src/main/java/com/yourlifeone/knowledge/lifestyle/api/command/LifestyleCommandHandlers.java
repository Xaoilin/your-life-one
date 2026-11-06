package com.yourlifeone.knowledge.lifestyle.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.knowledge.lifestyle.api.mapper.LifestyleMapper;
import com.yourlifeone.knowledge.lifestyle.api.request.ReorderColumnRequest;
import com.yourlifeone.knowledge.lifestyle.application.service.LifestyleService;
import com.yourlifeone.knowledge.shared.CommandHandler;
import com.yourlifeone.knowledge.shared.IdCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The lifestyle tracker writes the command queue performs. Moves and reorders answer with every item. */
@Configuration(proxyBeanMethods = false)
public class LifestyleCommandHandlers {
    public static final String SAVE_ITEM = "lifestyle.save-item";
    public static final String MOVE_ITEM = "lifestyle.move-item";
    public static final String REORDER_COLUMN = "lifestyle.reorder-column";
    public static final String DELETE_ITEM = "lifestyle.delete-item";

    @Bean
    WriteCommandHandler<SaveItemCommand> saveLifestyleItemHandler(LifestyleService lifestyle, LifestyleMapper mapper) {
        return CommandHandler.ok(SAVE_ITEM, SaveItemCommand.class, (user, command) -> mapper.toResponse(
                lifestyle.save(user, mapper.toDomain(command.id(), command.item()))));
    }

    @Bean
    WriteCommandHandler<MoveItemCommand> moveLifestyleItemHandler(LifestyleService lifestyle, LifestyleMapper mapper) {
        return CommandHandler.ok(MOVE_ITEM, MoveItemCommand.class, (user, command) -> mapper.toItemsResponse(
                lifestyle.move(user, command.id(), command.move().type(), command.move().position())));
    }

    @Bean
    WriteCommandHandler<ReorderColumnRequest> reorderLifestyleColumnHandler(LifestyleService lifestyle,
            LifestyleMapper mapper) {
        return CommandHandler.ok(REORDER_COLUMN, ReorderColumnRequest.class, (user, request) -> mapper.toItemsResponse(
                lifestyle.reorder(user, request.type(), request.ids())));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteLifestyleItemHandler(LifestyleService lifestyle) {
        return CommandHandler.noContent(DELETE_ITEM, IdCommand.class, (user, command) -> {
            lifestyle.delete(user, command.id());
            return null;
        });
    }
}

package com.yourlifeone.finance.equity.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.finance.api.response.DeletedResponse;
import com.yourlifeone.finance.equity.api.mapper.EquityMapper;
import com.yourlifeone.finance.equity.application.service.EquityService;
import com.yourlifeone.finance.shared.CommandHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The equity writes the command queue performs; their types are also the change events' routing keys. */
@Configuration(proxyBeanMethods = false)
public class EquityCommandHandlers {
    public static final String SAVE_POSITION = "equity.save-position";
    public static final String DELETE_POSITION = "equity.delete-position";

    @Bean
    WriteCommandHandler<SavePositionCommand> savePositionHandler(EquityService equity, EquityMapper mapper) {
        return CommandHandler.ok(SAVE_POSITION, SavePositionCommand.class, (user, command) -> mapper.toResponse(
                equity.save(user, command.id(), command.request().position(), command.request().expectedUpdatedAt())));
    }

    @Bean
    WriteCommandHandler<DeletePositionCommand> deletePositionHandler(EquityService equity) {
        return CommandHandler.ok(DELETE_POSITION, DeletePositionCommand.class, (user, command) ->
                new DeletedResponse(equity.delete(user, command.id(), command.expectedUpdatedAt())));
    }
}

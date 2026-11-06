package com.yourlifeone.planner.gamification.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.planner.gamification.api.mapper.MomentumMapper;
import com.yourlifeone.planner.gamification.application.service.MomentumService;
import com.yourlifeone.planner.shared.CommandHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The daily-momentum writes the command queue performs. */
@Configuration(proxyBeanMethods = false)
public class MomentumCommandHandlers {
    public static final String SAVE_PILLAR = "momentum.save-pillar";

    @Bean
    WriteCommandHandler<SaveMomentumCommand> saveMomentumHandler(MomentumService momentum, MomentumMapper mapper) {
        return CommandHandler.ok(SAVE_PILLAR, SaveMomentumCommand.class, (user, command) -> mapper.toResponse(
                momentum.save(user, command.pillar(), command.momentum().state(), command.momentum().expectedVersion())));
    }
}

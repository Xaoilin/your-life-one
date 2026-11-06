package com.yourlifeone.planner.gamification.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.planner.gamification.api.mapper.GamificationMapper;
import com.yourlifeone.planner.gamification.api.mapper.PrayerRewardMapper;
import com.yourlifeone.planner.gamification.application.service.GamificationService;
import com.yourlifeone.planner.gamification.application.service.PrayerRewardService;
import com.yourlifeone.planner.shared.AllowedValues;
import com.yourlifeone.planner.shared.CommandHandler;
import com.yourlifeone.planner.tasks.config.TasksProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The progress writes the command queue performs. */
@Configuration(proxyBeanMethods = false)
public class GamificationCommandHandlers {
    public static final String RESET = "gamification.reset";
    public static final String SYNC_PRAYER_REWARDS = "rewards.sync-prayers";

    @Bean
    WriteCommandHandler<ResetCommand> resetProgressHandler(GamificationService gamification, GamificationMapper mapper) {
        return CommandHandler.ok(RESET, ResetCommand.class, (user, command) -> mapper.toResponse(
                gamification.reset(user)));
    }

    @Bean
    WriteCommandHandler<SyncPrayerRewardsCommand> syncPrayerRewardsHandler(PrayerRewardService rewards,
            PrayerRewardMapper mapper, TasksProperties tasks) {
        return CommandHandler.ok(SYNC_PRAYER_REWARDS, SyncPrayerRewardsCommand.class, (user, command) -> mapper.toResponse(
                rewards.sync(user, command.date(), AllowedValues.optional("prayer", command.prayer(), tasks.prayerNames()),
                        command.outcomes(), command.knowledge())));
    }
}

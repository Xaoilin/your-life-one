package com.yourlifeone.knowledge.knowledge.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.knowledge.knowledge.api.mapper.KnowledgeMapper;
import com.yourlifeone.knowledge.knowledge.application.service.EntryService;
import com.yourlifeone.knowledge.knowledge.application.service.TopicService;
import com.yourlifeone.knowledge.shared.CommandHandler;
import com.yourlifeone.knowledge.shared.IdCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The knowledge-base writes the command queue performs; each names one topic or entry. */
@Configuration(proxyBeanMethods = false)
public class KnowledgeCommandHandlers {
    public static final String SAVE_TOPIC = "knowledge.save-topic";
    public static final String DELETE_TOPIC = "knowledge.delete-topic";
    public static final String SAVE_ENTRY = "knowledge.save-entry";
    public static final String DELETE_ENTRY = "knowledge.delete-entry";

    @Bean
    WriteCommandHandler<SaveTopicCommand> saveTopicHandler(TopicService topics, KnowledgeMapper mapper) {
        return CommandHandler.ok(SAVE_TOPIC, SaveTopicCommand.class, (user, command) -> mapper.toResponse(
                topics.save(user, mapper.toDomain(command.id(), command.topic()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteTopicHandler(TopicService topics) {
        return CommandHandler.noContent(DELETE_TOPIC, IdCommand.class, (user, command) -> {
            topics.delete(user, command.id());
            return null;
        });
    }

    @Bean
    WriteCommandHandler<SaveEntryCommand> saveEntryHandler(EntryService entries, KnowledgeMapper mapper) {
        return CommandHandler.ok(SAVE_ENTRY, SaveEntryCommand.class, (user, command) -> mapper.toResponse(
                entries.save(user, mapper.toDomain(command.id(), command.entry()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteEntryHandler(EntryService entries) {
        return CommandHandler.noContent(DELETE_ENTRY, IdCommand.class, (user, command) -> {
            entries.delete(user, command.id());
            return null;
        });
    }
}

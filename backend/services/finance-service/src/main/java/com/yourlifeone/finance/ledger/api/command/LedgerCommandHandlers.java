package com.yourlifeone.finance.ledger.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.finance.api.response.DeletedResponse;
import com.yourlifeone.finance.ledger.api.mapper.LedgerMapper;
import com.yourlifeone.finance.ledger.application.service.AccountService;
import com.yourlifeone.finance.ledger.application.service.BudgetService;
import com.yourlifeone.finance.ledger.application.service.SavingsGoalService;
import com.yourlifeone.finance.ledger.application.service.TransactionService;
import com.yourlifeone.finance.shared.CommandHandler;
import com.yourlifeone.finance.shared.IdCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The ledger writes the command queue performs. Their types are also the change events' routing keys. */
@Configuration(proxyBeanMethods = false)
public class LedgerCommandHandlers {
    public static final String SAVE_ACCOUNT = "finance.save-account";
    public static final String DELETE_ACCOUNT = "finance.delete-account";
    public static final String SAVE_TRANSACTION = "finance.save-transaction";
    public static final String DELETE_TRANSACTION = "finance.delete-transaction";
    public static final String SAVE_BUDGET = "finance.save-budget";
    public static final String DELETE_BUDGET = "finance.delete-budget";
    public static final String SAVE_SAVINGS_GOAL = "finance.save-savings-goal";
    public static final String DELETE_SAVINGS_GOAL = "finance.delete-savings-goal";

    @Bean
    WriteCommandHandler<SaveAccountCommand> saveAccountHandler(AccountService accounts, LedgerMapper mapper) {
        return CommandHandler.ok(SAVE_ACCOUNT, SaveAccountCommand.class, (user, command) -> mapper.toResponse(
                accounts.save(user, mapper.toDomain(command.id(), command.request()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteAccountHandler(AccountService accounts, LedgerMapper mapper) {
        return CommandHandler.ok(DELETE_ACCOUNT, IdCommand.class, (user, command) -> mapper.toResponse(
                accounts.delete(user, command.id())));
    }

    @Bean
    WriteCommandHandler<SaveTransactionCommand> saveTransactionHandler(TransactionService transactions,
            LedgerMapper mapper) {
        return CommandHandler.ok(SAVE_TRANSACTION, SaveTransactionCommand.class, (user, command) -> mapper.toResponse(
                transactions.save(user, mapper.toDomain(command.id(), command.request()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteTransactionHandler(TransactionService transactions, LedgerMapper mapper) {
        return CommandHandler.ok(DELETE_TRANSACTION, IdCommand.class, (user, command) -> mapper.toResponse(
                transactions.delete(user, command.id())));
    }

    @Bean
    WriteCommandHandler<SaveBudgetCommand> saveBudgetHandler(BudgetService budgets, LedgerMapper mapper) {
        return CommandHandler.ok(SAVE_BUDGET, SaveBudgetCommand.class, (user, command) -> mapper.toResponse(
                budgets.save(user, mapper.toDomain(command.id(), command.request()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteBudgetHandler(BudgetService budgets) {
        return CommandHandler.ok(DELETE_BUDGET, IdCommand.class, (user, command) ->
                new DeletedResponse(budgets.delete(user, command.id())));
    }

    @Bean
    WriteCommandHandler<SaveSavingsGoalCommand> saveSavingsGoalHandler(SavingsGoalService goals, LedgerMapper mapper) {
        return CommandHandler.ok(SAVE_SAVINGS_GOAL, SaveSavingsGoalCommand.class, (user, command) -> mapper.toResponse(
                goals.save(user, mapper.toDomain(command.id(), command.request()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteSavingsGoalHandler(SavingsGoalService goals) {
        return CommandHandler.ok(DELETE_SAVINGS_GOAL, IdCommand.class, (user, command) ->
                new DeletedResponse(goals.delete(user, command.id())));
    }
}

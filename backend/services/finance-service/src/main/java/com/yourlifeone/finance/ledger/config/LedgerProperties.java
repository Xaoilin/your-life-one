package com.yourlifeone.finance.ledger.config;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The manual ledger ({@code app.finance.ledger.*}). Amounts are integer pence.
 *
 * @param transferCategory the category of a transfer between two accounts
 * @param maxAmount the largest transaction amount, budget limit or goal amount
 * @param maxBalance the largest balance an account may reach, either way
 */
@ConfigurationProperties("app.finance.ledger")
public record LedgerProperties(List<String> accountTypes, List<String> transactionTypes,
        List<String> expenseCategories, List<String> incomeCategories, String transferCategory, long maxAmount,
        long maxBalance, int maxTags, int maxTagLength, int maxNameLength, int maxDescriptionLength) {
    public LedgerProperties {
        accountTypes = copy(accountTypes);
        transactionTypes = copy(transactionTypes);
        expenseCategories = copy(expenseCategories);
        incomeCategories = copy(incomeCategories);
    }

    /** Every category a transaction may have. */
    public List<String> transactionCategories() {
        return Stream.of(expenseCategories, incomeCategories, List.of(transferCategory)).flatMap(List::stream).toList();
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}

package com.yourlifeone.calendar.infrastructure.persistence.entity;

import com.yourlifeone.calendar.domain.AccountProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountProviderConverter implements AttributeConverter<AccountProvider, String> {
    @Override
    public String convertToDatabaseColumn(AccountProvider provider) {
        return provider == null ? null : provider.value();
    }

    @Override
    public AccountProvider convertToEntityAttribute(String value) {
        return value == null ? null : AccountProvider.fromValue(value);
    }
}

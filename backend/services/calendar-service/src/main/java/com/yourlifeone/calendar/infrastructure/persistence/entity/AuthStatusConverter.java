package com.yourlifeone.calendar.infrastructure.persistence.entity;

import com.yourlifeone.calendar.domain.AuthStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuthStatusConverter implements AttributeConverter<AuthStatus, String> {
    @Override
    public String convertToDatabaseColumn(AuthStatus status) {
        return status == null ? null : status.value();
    }

    @Override
    public AuthStatus convertToEntityAttribute(String value) {
        return value == null ? null : AuthStatus.fromValue(value);
    }
}

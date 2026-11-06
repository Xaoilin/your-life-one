package com.yourlifeone.prayer.infrastructure.persistence.entity;

import com.yourlifeone.prayer.domain.OutcomeStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores outcome statuses as their API values ({@code on_time}), matching the column's check constraint. */
@Converter(autoApply = true)
public class OutcomeStatusConverter implements AttributeConverter<OutcomeStatus, String> {
    @Override
    public String convertToDatabaseColumn(OutcomeStatus status) {
        return status == null ? null : status.value();
    }

    @Override
    public OutcomeStatus convertToEntityAttribute(String value) {
        return value == null ? null : OutcomeStatus.fromValue(value);
    }
}

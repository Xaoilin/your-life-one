package com.yourlifeone.prayer.infrastructure.persistence.entity;

import com.yourlifeone.prayer.domain.OutcomeTiming;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores outcome timing as their API values ({@code within_window}), matching the column's check constraint. */
@Converter(autoApply = true)
public class OutcomeTimingConverter implements AttributeConverter<OutcomeTiming, String> {
    @Override
    public String convertToDatabaseColumn(OutcomeTiming status) {
        return status == null ? null : status.value();
    }

    @Override
    public OutcomeTiming convertToEntityAttribute(String value) {
        return value == null ? null : OutcomeTiming.fromValue(value);
    }
}

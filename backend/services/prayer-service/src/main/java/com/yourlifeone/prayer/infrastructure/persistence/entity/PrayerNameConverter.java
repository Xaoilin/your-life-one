package com.yourlifeone.prayer.infrastructure.persistence.entity;

import com.yourlifeone.prayer.domain.PrayerName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores prayer names as their display labels ({@code Fajr}), matching the column's check constraint. */
@Converter(autoApply = true)
public class PrayerNameConverter implements AttributeConverter<PrayerName, String> {
    @Override
    public String convertToDatabaseColumn(PrayerName prayer) {
        return prayer == null ? null : prayer.label();
    }

    @Override
    public PrayerName convertToEntityAttribute(String label) {
        return label == null ? null : PrayerName.fromLabel(label);
    }
}

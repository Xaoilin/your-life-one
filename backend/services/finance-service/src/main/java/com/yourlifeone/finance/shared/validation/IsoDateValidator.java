package com.yourlifeone.finance.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Checks {@link IsoDate}: the strict ISO format refuses dates such as 2026-02-30. */
public class IsoDateValidator implements ConstraintValidator<IsoDate, String> {
    private static final Pattern FORMAT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final String YEAR_ZERO = "0000-";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (!FORMAT.matcher(value).matches() || value.startsWith(YEAR_ZERO)) return false;
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}

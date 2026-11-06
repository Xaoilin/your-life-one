package com.yourlifeone.finance.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A real calendar date written {@code YYYY-MM-DD}, from year 0001. Null is valid (add {@code @NotNull}). */
@Documented
@Constraint(validatedBy = IsoDateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsoDate {
    String message() default "must be a YYYY-MM-DD date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

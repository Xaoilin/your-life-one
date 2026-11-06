package com.yourlifeone.finance.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** An HTTP or HTTPS source address without embedded credentials. Null is valid (add {@code @NotNull}). */
@Documented
@Constraint(validatedBy = WebUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WebUrl {
    String message() default "must be an HTTP or HTTPS source URL without embedded credentials";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

package com.yourlifeone.life.shared;

import com.yourlifeone.commonservice.web.ApiException;
import java.util.Collection;
import java.util.List;

/** Checks a value against the configured options, with an error the caller can act on. */
public final class AllowedValues {
    private AllowedValues() {
    }

    public static String require(String field, String value, Collection<String> allowed) {
        if (value == null || !allowed.contains(value)) {
            throw ApiException.badRequest("invalid_" + snake(field), field + " must be one of " + allowed + ".");
        }
        return value;
    }

    /** Null stays null; anything else must be allowed. */
    public static String optional(String field, String value, Collection<String> allowed) {
        return value == null ? null : require(field, value, allowed);
    }

    public static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String snake(String field) {
        return field.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}

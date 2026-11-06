package com.yourlifeone.life.shared;

import com.yourlifeone.commonservice.web.ApiException;
import java.util.regex.Pattern;

/**
 * Record IDs are chosen by the caller (the React app uses UUIDs; older job records use slugs), so a
 * retried create can never make a second record. An ID is 1-256 visible characters.
 */
public final class RecordIds {
    private static final Pattern VALID = Pattern.compile("[\\x21-\\x7E]{1,256}");

    private RecordIds() {
    }

    public static String require(String id) {
        if (id == null || !VALID.matcher(id).matches()) {
            throw ApiException.badRequest("invalid_id", "IDs must be 1-256 visible characters.");
        }
        return id;
    }
}

package com.yourlifeone.finance.shared.validation;

/**
 * Bounds of what the app and AI agents may send, as the Supabase finance and equity tools accepted it.
 * Constants so they can be used in validation annotations.
 */
public final class Limits {
    /** A record ID inside a document (an account, loan, source, grant...). */
    public static final int MAX_ID = 256;
    /** Notes, labels and other short text. */
    public static final int MAX_TEXT = 4_000;
    /** Longer text such as an equity detail's body. */
    public static final int MAX_LONG_TEXT = 16_000;
    public static final int MAX_URL = 2_048;
    public static final int MAX_COMPANY = 200;
    /** Expected updatedAt values echoed back by the app. */
    public static final int MAX_VERSION = 64;
    /** 10 billion pounds, in pence. */
    public static final long MAX_PENCE = 1_000_000_000_000L;
    public static final long MIN_SIGNED_PENCE = -MAX_PENCE;
    public static final long MAX_COUNT = 10_000_000L;
    public static final long MAX_QUANTITY = 1_000_000_000_000L;
    public static final String MAX_MONEY = "1000000000000";
    public static final String MAX_APR_PERCENT = "1000";
    public static final String MAX_USD_TO_GBP = "1000000";
    /** A calendar month, {@code YYYY-MM}, from year 0001. */
    public static final String MONTH = "^(?!0000)\\d{4}-(0[1-9]|1[0-2])$";

    private Limits() {
    }
}

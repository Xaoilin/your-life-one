package com.yourlifeone.finance.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Checks {@link WebUrl}: a scheme of http or https, a host, no whitespace or backslash, and no user name
 * or password before the host.
 */
public class WebUrlValidator implements ConstraintValidator<WebUrl, String> {
    private static final Pattern FORMAT = Pattern.compile("^https?://[^\\s/?#]+[^\\s]*$");
    private static final String SCHEME_END = "://";
    private static final String AUTHORITY_END = "/?#";
    private static final char CREDENTIALS = '@';
    private static final char BACKSLASH = '\\';
    private static final char PORT = ':';

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (value.length() > Limits.MAX_URL || !FORMAT.matcher(value).matches() || value.indexOf(BACKSLASH) >= 0) {
            return false;
        }
        var authority = authorityOf(value);
        return authority.indexOf(CREDENTIALS) < 0 && !hostOf(authority).isEmpty();
    }

    private static String authorityOf(String url) {
        var start = url.indexOf(SCHEME_END) + SCHEME_END.length();
        var end = start;
        while (end < url.length() && AUTHORITY_END.indexOf(url.charAt(end)) < 0) end++;
        return url.substring(start, end);
    }

    private static String hostOf(String authority) {
        var port = authority.lastIndexOf(PORT);
        return port < 0 ? authority : authority.substring(0, port);
    }
}

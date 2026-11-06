package com.yourlifeone.access.mcp.application;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small input adapters after SDK schema validation; missing/null fields retain their API meaning. */
public final class McpArguments {
    private McpArguments() {
    }

    public static String string(Map<String, Object> arguments, String key) {
        if (!(arguments.get(key) instanceof String value)) throw new IllegalArgumentException("Missing " + key + ".");
        return value.trim();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Map<String, Object> arguments, String key) {
        if (!(arguments.get(key) instanceof Map<?, ?> value)) throw new IllegalArgumentException("Missing " + key + ".");
        return new LinkedHashMap<>((Map<String, Object>) value);
    }

    public static Map<String, Object> fields(Object... pairs) {
        var values = new LinkedHashMap<String, Object>();
        for (int index = 0; index < pairs.length; index += 2) values.put((String) pairs[index], pairs[index + 1]);
        return values;
    }

    public static Map<String, Object> query(Map<String, Object> arguments, String... keys) {
        var values = new LinkedHashMap<String, Object>();
        for (String key : keys) {
            Object value = arguments.get(key);
            values.put(key, value instanceof String text ? text.trim() : value);
        }
        return values;
    }

    public static void confirmed(Map<String, Object> arguments) {
        if (!Boolean.TRUE.equals(arguments.get("confirmed"))) {
            throw new IllegalArgumentException("Explicit confirmation is required for this exact record.");
        }
    }
}

package com.yourlifeone.finance.shared.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/** Per-field integer input: Jackson's global float-to-integer coercion must not truncate profile assumptions. */
public final class WholeNumberDeserializer extends StdScalarDeserializer<Long> {
    public WholeNumberDeserializer() {
        super(Long.class);
    }

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        if (!parser.isExpectedNumberIntToken()) {
            return context.reportInputMismatch(Long.class, "Use a whole-number JSON value.");
        }
        return parser.getLongValue();
    }
}

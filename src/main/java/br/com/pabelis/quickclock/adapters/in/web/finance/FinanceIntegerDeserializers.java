package br.com.pabelis.quickclock.adapters.in.web.finance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public final class FinanceIntegerDeserializers {
    private FinanceIntegerDeserializers() {}

    public static final class Cents extends StdDeserializer<Long> {
        public Cents() { super(Long.class); }
        @Override public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
                return context.reportInputMismatch(Long.class,"Cents must be an integer JSON number.");
            return parser.getLongValue();
        }
    }

    public static final class Count extends StdDeserializer<Integer> {
        public Count() { super(Integer.class); }
        @Override public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
                return context.reportInputMismatch(Integer.class,"Count or day must be an integer JSON number.");
            return parser.getIntValue();
        }
    }
}

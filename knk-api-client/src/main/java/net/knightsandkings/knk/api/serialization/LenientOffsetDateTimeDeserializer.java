package net.knightsandkings.knk.api.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Accepts both full offset datetimes and local datetimes from API responses.
 * Local datetimes are treated as UTC to preserve existing mapper behavior.
 */
public class LenientOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {
    private static final DateTimeFormatter SPACE_SEPARATED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null) {
            return null;
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ignored) {
            // Fall through to local datetime parsing variants.
        }

        try {
            return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
        } catch (Exception ignored) {
            // Fall through to alternate local datetime format.
        }

        try {
            return LocalDateTime.parse(value, SPACE_SEPARATED).atOffset(ZoneOffset.UTC);
        } catch (Exception ex) {
            throw context.weirdStringException(value, OffsetDateTime.class,
                "Expected ISO_OFFSET_DATE_TIME or local datetime (yyyy-MM-dd'T'HH:mm:ss)");
        }
    }
}

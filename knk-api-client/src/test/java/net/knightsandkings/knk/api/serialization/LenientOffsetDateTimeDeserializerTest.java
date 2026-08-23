package net.knightsandkings.knk.api.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LenientOffsetDateTimeDeserializerTest {

    private static final class DateContainer {
        public OffsetDateTime createdAt;
    }

    private static ObjectMapper mapper() {
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(OffsetDateTime.class, new LenientOffsetDateTimeDeserializer());
        return new ObjectMapper().registerModule(module);
    }

    @Test
    void parsesOffsetDateTime() throws Exception {
        String json = "{\"createdAt\":\"2026-08-23T13:19:15Z\"}";

        DateContainer dto = mapper().readValue(json, DateContainer.class);

        assertEquals(OffsetDateTime.parse("2026-08-23T13:19:15Z"), dto.createdAt);
    }

    @Test
    void parsesLocalDateTimeAsUtc() throws Exception {
        String json = "{\"createdAt\":\"2026-08-23T13:19:15\"}";

        DateContainer dto = mapper().readValue(json, DateContainer.class);

        assertEquals(OffsetDateTime.parse("2026-08-23T13:19:15Z"), dto.createdAt);
    }

    @Test
    void rejectsInvalidDate() {
        String json = "{\"createdAt\":\"not-a-date\"}";

        assertThrows(Exception.class, () -> mapper().readValue(json, DateContainer.class));
    }
}

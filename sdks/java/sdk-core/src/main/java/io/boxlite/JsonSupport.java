package io.boxlite;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

final class JsonSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper().rebuild()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .changeDefaultVisibility(visibility ->
            visibility.withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
        .build();

    private JsonSupport() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new BoxliteException("Failed to serialize value to JSON", e);
        }
    }

    static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new BoxliteException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    static <T> List<T> readList(String json, Class<T> elementType) {
        JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new BoxliteException("Failed to parse JSON list: " + e.getMessage(), e);
        }
    }
}

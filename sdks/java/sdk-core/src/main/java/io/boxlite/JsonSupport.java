package io.boxlite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

final class JsonSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private JsonSupport() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BoxliteException("Failed to serialize value to JSON", e);
        }
    }

    static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new BoxliteException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    static <T> List<T> readList(String json, Class<T> elementType) {
        JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new BoxliteException("Failed to parse JSON list: " + e.getMessage(), e);
        }
    }
}

package dev.drme.rugdar.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonUtils {

    private static ObjectMapper mapper;

    public JsonUtils(ObjectMapper objectMapper) {
        JsonUtils.mapper = objectMapper;
    }

    public static String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize " + value.getClass().getSimpleName(), e);
        }
    }

    public static <T> T deserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize " + type.getSimpleName(), e);
        }
    }
}

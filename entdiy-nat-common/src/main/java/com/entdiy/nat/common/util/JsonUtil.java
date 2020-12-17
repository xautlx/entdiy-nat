package com.entdiy.nat.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonUtil {
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json serialize error", e);
        }
    }

    public static <T> T deserialize(String value, Class<T> clazz) {
        try {
            return objectMapper.readValue(value, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json deserialize error", e);
        }
    }

    public static Map<String, Object> deserializeToMap(String value) {
        try {
            return objectMapper.readValue(value, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json deserialize error", e);
        }
    }
}

/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

/**
 * Centralized JSON utility methods using Jackson.
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtil() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static ObjectNode createObject() {
        return MAPPER.createObjectNode();
    }

    public static ArrayNode createArray() {
        return MAPPER.createArrayNode();
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON deserialization failed: " + e.getMessage(), e);
        }
    }

    public static void writeToFile(Object value, File file) throws IOException {
        MAPPER.writeValue(file, value);
    }

    public static String textAt(JsonNode node, String field) {
        return textAt(node, field, "");
    }

    public static String textAt(JsonNode node, String field, String defaultValue) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText(defaultValue);
        }
        return defaultValue;
    }

    public static boolean boolAt(JsonNode node, String field) {
        return node != null && node.has(field) && node.get(field).asBoolean(false);
    }

    public static int intAt(JsonNode node, String field, int defaultValue) {
        if (node != null && node.has(field)) {
            return node.get(field).asInt(defaultValue);
        }
        return defaultValue;
    }
}

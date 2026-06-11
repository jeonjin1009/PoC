package com.poc.json.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class JsonFileParser {

    private final ObjectMapper mapper;

    public JsonFileParser() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** classpath 리소스를 POJO로 역직렬화 */
    public <T> T parseFromResource(String resourcePath, Class<T> type) throws IOException {
        try (InputStream is = getResourceStream(resourcePath)) {
            return mapper.readValue(is, type);
        }
    }

    /** classpath 리소스를 제네릭 컬렉션으로 역직렬화 (e.g. List<User>) */
    public <T> T parseFromResource(String resourcePath, TypeReference<T> typeRef) throws IOException {
        try (InputStream is = getResourceStream(resourcePath)) {
            return mapper.readValue(is, typeRef);
        }
    }

    /** 파일 경로에서 POJO로 역직렬화 */
    public <T> T parseFromFile(File file, Class<T> type) throws IOException {
        return mapper.readValue(file, type);
    }

    /** 파일 경로에서 제네릭 컬렉션으로 역직렬화 (e.g. List<User>) */
    public <T> T parseFromFile(File file, TypeReference<T> typeRef) throws IOException {
        return mapper.readValue(file, typeRef);
    }

    /** JSON 문자열을 POJO로 역직렬화 */
    public <T> T parseFromString(String json, Class<T> type) throws IOException {
        return mapper.readValue(json, type);
    }

    /** JSON 문자열을 제네릭 컬렉션으로 역직렬화 */
    public <T> T parseFromString(String json, TypeReference<T> typeRef) throws IOException {
        return mapper.readValue(json, typeRef);
    }

    /** 트리 모델(JsonNode)로 파싱 — 스키마 불확실 시 유용 */
    public JsonNode parseAsTree(String resourcePath) throws IOException {
        try (InputStream is = getResourceStream(resourcePath)) {
            return mapper.readTree(is);
        }
    }

    /** POJO를 JSON 문자열로 직렬화 */
    public String toJson(Object object) throws IOException {
        return mapper.writeValueAsString(object);
    }

    /** POJO를 JSON 파일로 저장 */
    public void toJsonFile(Object object, File outputFile) throws IOException {
        mapper.writeValue(outputFile, object);
    }

    /** 특정 JSON 필드 값을 직접 추출 (JsonNode 활용) */
    public String extractField(String json, String fieldPath) throws IOException {
        JsonNode root = mapper.readTree(json);
        String[] parts = fieldPath.split("\\.");
        JsonNode node = root;
        for (String part : parts) {
            if (node == null) return null;
            node = node.get(part);
        }
        return node != null ? node.asText() : null;
    }

    /** JSON 배열에서 특정 조건의 항목 개수 조회 */
    public int countArrayElements(String resourcePath) throws IOException {
        try (InputStream is = getResourceStream(resourcePath)) {
            JsonNode root = mapper.readTree(is);
            return root.isArray() ? root.size() : -1;
        }
    }

    public ObjectMapper getMapper() { return mapper; }

    private InputStream getResourceStream(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("리소스를 찾을 수 없습니다: " + resourcePath);
        }
        return is;
    }
}

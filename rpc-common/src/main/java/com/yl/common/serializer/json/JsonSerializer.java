package com.yl.common.serializer.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.yl.common.serializer.Serializer;

import java.io.IOException;
import java.text.SimpleDateFormat;

// 基于 JSON 的序列化实现
public class JsonSerializer implements Serializer {
    // 实现 JSON 序列化的工具类。
    private ObjectMapper objectMapper;

    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.objectMapper.setDateFormat(dateFormat);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        this.objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);
        this.objectMapper.disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
        this.objectMapper.disable(SerializationFeature.CLOSE_CLOSEABLE);
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
    }

    @Override
    public <T> byte[] serialize(T obj) {
        try {
            return this.objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        try {
            return this.objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

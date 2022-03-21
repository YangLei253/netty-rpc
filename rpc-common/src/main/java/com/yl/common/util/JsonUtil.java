package com.yl.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

// JSON 工具类，主要将 RpcServerInfo/RpcNode 对象转换为 JSON，从而方便直接从 zookeeper 中查看。
public class JsonUtil {
    private static ObjectMapper objMapper = new ObjectMapper();

    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        objMapper.setDateFormat(dateFormat);
        objMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);
        objMapper.disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
        objMapper.disable(SerializationFeature.CLOSE_CLOSEABLE);
        objMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
    }

    // 转换 JSON 到对象
    public static <type> type jsonToObject(String json, Class<?> cls) {
        type obj = null;
        JavaType javaType = objMapper.getTypeFactory().constructType(cls);
        try {
            obj = objMapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }

    // 转换 JSON 到对象集合
    public static <type> type jsonToObjectList(String json, Class<?> collectionClass, Class<?>... elementClass) {
        type obj = null;
        JavaType javaType = objMapper.getTypeFactory().constructParametricType(collectionClass, elementClass);
        try {
            obj = objMapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }

    // 转换 JSON 到对象 Map
    public static <type> type jsonToObjectHashMap(String json, Class<?> keyClass, Class<?> valueClass) {
        type obj = null;
        JavaType javaType = objMapper.getTypeFactory().constructParametricType(HashMap.class, keyClass, valueClass);
        try {
            obj = objMapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }

    // 转换对象为 JSON
    public static String objectToJson(Object o) {
        String json = "";
        try {
            json = objMapper.writeValueAsString(o);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return json;
    }
}

package com.yl.common.serializer.protostuff;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.yl.common.serializer.Serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 基于 Protocol Buffer 的序列化实现 (借助于 Protostuff，可以简化对 Protocol Buffer 的使用)
public class ProtostuffSerializer implements Serializer {
    // 存放 Class 与 Schema 间的映射关系 (Schema 可简单理解为序列化对象的结构，它可自动生成。这也是相比于原生 Protocol Buffer 的极大优点)。
    private Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

    // 缓存此空间，重复使用。
    private LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

    private <T> Schema<T> getSchema(Class<T> clazz) {
        return (Schema<T>) schemaCache.computeIfAbsent(clazz, RuntimeSchema::createFrom);
    }

    @Override
    public <T> byte[] serialize(T obj) {
        Class<T> clazz = (Class<T>) obj.getClass();

        try {
            Schema<T> schema = getSchema(clazz);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        Schema<T> schema = getSchema(clazz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }
}

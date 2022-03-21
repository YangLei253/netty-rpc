package com.yl.common.serializer;

// 序列化的接口定义
public interface Serializer {
    public <T> byte[] serialize(T obj);
    public <T> Object deserialize(byte[] bytes, Class<T> clazz);
}

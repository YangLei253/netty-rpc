package com.yl.common.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.yl.common.codec.RpcRequest;
import com.yl.common.codec.RpcResponse;
import com.yl.common.serializer.Serializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// 基于 Kryo 的序列化实现
public class KryoSerializer implements Serializer {
    // 构建 Kryo 实例的工厂类
    private KryoFactory factory = new KryoFactory() {
        @Override
        public Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setReferences(false);
            kryo.register(RpcRequest.class);
            kryo.register(RpcResponse.class);
            Kryo.DefaultInstantiatorStrategy strategy = (Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy();
            strategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
            return kryo;
        }
    };

    // Kryo 池
    private KryoPool pool = new KryoPool.Builder(factory).build();

    @Override
    public <T> byte[] serialize(T obj) {
        Kryo kryo  = pool.borrow();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);

        try {
            kryo.writeObject(output, obj);
            output.close();
            return outputStream.toByteArray();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            pool.release(kryo);
        }
    }

    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        Kryo kryo = pool.borrow();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(inputStream);

        try {
            Object result = kryo.readObject(input, clazz);
            input.close();
            return result;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            pool.release(kryo);
        }
    }
}

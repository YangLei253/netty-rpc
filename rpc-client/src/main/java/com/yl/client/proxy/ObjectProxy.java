package com.yl.client.proxy;

import com.yl.client.connection.ConnectionManager;
import com.yl.client.handler.RpcClientHandler;
import com.yl.common.codec.RpcRequest;
import com.yl.common.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// 借助于 Java 提供的默认反射机制，实现远程代理
public class ObjectProxy<T> implements InvocationHandler, RpcServer {
    private static final Logger logger = LoggerFactory.getLogger(ObjectProxy.class);

    // 代理的 class 和 version
    private Class<T> clazz;
    private String version;

    public ObjectProxy(Class<T> clazz, String version) {
        this.clazz = clazz;
        this.version = version;
    }

    // Java 默认实现方式
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        // 如果调用的方法为 Object 默认实现，直接在此模拟实现即可，无需远程调用。
        if (Object.class == method.getDeclaringClass()) {
            String methodName = method.getName();
            switch (methodName) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy))
                            + ", with InvocationHandler " + this;
                default:
                    throw new UnsupportedOperationException(method.toGenericString());
            }
        }

        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(this.clazz.getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        request.setVersion(this.version);

        String serviceKey = ServiceUtil.makeServiceKey(request.getClassName(), request.getVersion());
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        CompletableFuture<Object> future = handler.sendRequest(request);
        return future.get();
    }

    // 额外提供的异步版本
    @Override
    public CompletableFuture<Object> call(String funcName, Object... args) throws Exception {
        RpcRequest request = createRequest(this.clazz.getName(), funcName, args);
        String serviceKey = ServiceUtil.makeServiceKey(request.getClassName(), request.getVersion());
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        CompletableFuture<Object> future = handler.sendRequest(request);
        return future;
    }

    private RpcRequest createRequest(String className, String funcName, Object... args) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(className);
        request.setMethodName(funcName);
        request.setParameters(args);
        request.setVersion(this.version);

        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        request.setParameterTypes(parameterTypes);

        return request;
    }
}

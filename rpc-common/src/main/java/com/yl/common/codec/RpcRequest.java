package com.yl.common.codec;

import java.io.Serializable;
import java.util.Arrays;

// RPC 请求格式
public class RpcRequest implements Serializable {
    // 用于区分来自同一客户端的不同请求
    private String requestId;
    // 待请求的类名
    private String className;
    // 待请求的方法名
    private String methodName;
    // 待请求的参数类型
    private Class<?>[] parameterTypes;
    // 待请求的参数列表
    private Object[] parameters;
    // RPC 版本 (用作迭代升级)
    private String version;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId='" + requestId + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                ", parameters=" + Arrays.toString(parameters) +
                ", version='" + version + '\'' +
                '}';
    }
}

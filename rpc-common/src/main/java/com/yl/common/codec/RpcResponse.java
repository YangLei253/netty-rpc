package com.yl.common.codec;

import java.io.Serializable;

// RPC 回复格式
public class RpcResponse implements Serializable {
    // 与请求相同的 ID
    private String requestId;
    // 请求结果 (处理结果/异常对象)
    private Object result;
    private Throwable error;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
                ", result=" + result +
                ", error=" + error +
                '}';
    }
}

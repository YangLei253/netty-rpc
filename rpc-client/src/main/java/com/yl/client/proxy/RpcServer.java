package com.yl.client.proxy;

import java.util.concurrent.CompletableFuture;

public interface RpcServer {
    CompletableFuture<Object> call(String funcName, Object... args) throws Exception;
}

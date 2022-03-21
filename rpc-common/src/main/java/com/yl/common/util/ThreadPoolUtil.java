package com.yl.common.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {
    public static ThreadPoolExecutor getInstance(String serviceName, int coreSize, int maxSize) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, maxSize, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "netty-rpc" + serviceName + "-" + r.hashCode());
                    }
                }, new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}

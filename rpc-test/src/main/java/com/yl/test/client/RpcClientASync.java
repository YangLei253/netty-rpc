package com.yl.test.client;

import com.yl.client.RpcClient;
import com.yl.client.proxy.RpcServer;
import com.yl.test.service.HelloService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class RpcClientASync {
    public static void main(String[] args) throws Exception {
        // new ClassPathXmlApplicationContext("client-spring.xml");

        RpcClient client = new RpcClient("192.168.1.113:2181");

        int threadNum = 1;
        int requestNum = 50;
        Thread[] threads = new Thread[threadNum];
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    RpcServer helloService = client.createAsyncService(HelloService.class, "1.0");
                    CompletableFuture<Object>[] futures = new CompletableFuture[requestNum];
                    for (int j = 0; j < requestNum; j++) {
                        try {
                            futures[j] = helloService.call("hello", "I'm coming " + j);
                            futures[j].whenCompleteAsync(new BiConsumer<Object, Throwable>() {
                                @Override
                                public void accept(Object o, Throwable throwable) {
                                    if (o != null) {
                                        System.out.println("get result: " + (String) o);
                                    } else {
                                        System.out.println("get exception: " + throwable.getMessage());
                                    }
                                }
                            }, executorService);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < threadNum; i++) {
            threads[i].join();
        }

        executorService.awaitTermination(10, TimeUnit.SECONDS);

        client.destroy();
    }
}

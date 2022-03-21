package com.yl.test.client;

import com.yl.client.RpcClient;
import com.yl.test.service.HelloService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcClientSync {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("client-spring.xml");

        RpcClient client = applicationContext.getBean(RpcClient.class);

        // RpcClient client = new RpcClient("192.168.1.101:2181");

        int threadNum = 1;
        int requestNum = 50;
        Thread[] threads = new Thread[threadNum];

        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    HelloService helloService = client.createService(HelloService.class, "1.0");
                    for (int j = 0; j < requestNum; j++) {
                        String result = helloService.hello("I'm coming " + j);
                        System.out.println(result);
                    }
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < threadNum; i++) {
            threads[i].join();
        }

        client.destroy();
    }
}

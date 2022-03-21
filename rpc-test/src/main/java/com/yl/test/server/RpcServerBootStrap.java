package com.yl.test.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcServerBootStrap {
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("server-spring.xml");
    }
}

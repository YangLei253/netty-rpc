package com.yl.test.service;

import com.yl.common.annotation.NettyRpcService;

@NettyRpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    public HelloServiceImpl() {

    }

    @Override
    public String hello(String name) {
        return "Hello " + name;
    }

    @Override
    public String hello(Person person) {
        return "Hello " + person.getFirstName() + " " + person.getLastName();
    }

    @Override
    public String hello(String name, Integer age) {
        return name + " is " + age;
    }
}

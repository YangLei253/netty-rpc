package com.yl.common.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// RPC 服务的注解标识 (仅使用 Spring 的注解功能)
// 此标识应用于类/接口/枚举、注解信息存在于运行时，可在 JVM 运行时获取、其可被 Spring 扫描
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface NettyRpcService {
    Class<?> value();

    String version() default "1.0";
}

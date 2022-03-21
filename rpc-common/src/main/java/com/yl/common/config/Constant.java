package com.yl.common.config;

// zookeeper 配置相关的常量信息
public class Constant {
    public static int ZK_SESSION_TIMEOUT = 5000;
    public static int ZK_CONNECTION_TIMEOUT = 5000;
    // zookeeper 的注册目录
    public static String ZK_REGISTRY_PATH = "/registry";
    // zookeeper 的服务节点目录
    public static String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/services";
    public static String ZK_NAMESPACE = "netty-rpc";
}

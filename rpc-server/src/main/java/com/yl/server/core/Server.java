package com.yl.server.core;

// 服务器的接口定义
public interface Server {
    // 启动
    public void start() throws Exception;

    // 向其中增加服务信息
    public void addService(String className, String version, Object bean);

    // 关闭
    public void stop() throws Exception;
}

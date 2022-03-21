package com.yl.server.core;

import com.yl.common.serializer.Serializer;
import com.yl.common.util.ServiceUtil;
import com.yl.common.util.ThreadPoolUtil;
import com.yl.server.register.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

// Netty 版本的 Server 实现
public class NettyServer implements Server {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    // 运行 Netty 服务的线程
    private Thread thread;
    // 服务器地址
    private String serverAddress;
    // 服务注册
    private ServiceRegistry serviceRegistry;
    // 采用的序列化器
    private Serializer serializer;
    // 接口与服务实现间的映射
    private Map<String, Object> serviceMap;

    public NettyServer(String serverAddress, String registryAddress, String serializer) throws Exception {
        this.serverAddress = serverAddress;
        this.serviceRegistry = new ServiceRegistry(registryAddress);
        this.serviceMap = new HashMap<>();
        this.serializer = (Serializer) Class.forName(serializer).newInstance();
    }

    @Override
    public void addService(String className, String version, Object bean) {
        logger.info("Adding service, interface: {}, version: {}, bean：{}", className, version, bean);
        String serviceKey = ServiceUtil.makeServiceKey(className, version);
        this.serviceMap.put(serviceKey, bean);
    }

    @Override
    public void start() throws Exception {
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup boss = new NioEventLoopGroup();
                EventLoopGroup worker = new NioEventLoopGroup();
                ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.getInstance(NettyServer.class.getSimpleName(), 16, 32);

                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(boss, worker).channel(NioServerSocketChannel.class)
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childHandler(new RpcServerInitializer(serviceMap, threadPoolExecutor, serializer))
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    String[] array = serverAddress.split(":");
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);

                    ChannelFuture future = b.bind(host, port).sync();

                    if (serviceRegistry != null) {
                        serviceRegistry.registerService(host, port, serviceMap);
                    }

                    logger.info("Server started on port {}", port);

                    future.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    if (e instanceof InterruptedException) {
                        logger.info("Rpc server is stoped");
                    } else {
                        logger.error("Rpc server remoting server error", e);
                    }
                } finally {
                    serviceRegistry.unregisterService();
                    boss.shutdownGracefully();
                    worker.shutdownGracefully();
                    threadPoolExecutor.shutdown();
                }
            }
        });

        this.thread.start();
    }

    @Override
    public void stop() throws Exception {
        if (this.thread != null && this.thread.isAlive()) {
            this.thread.interrupt();
        }
    }
}

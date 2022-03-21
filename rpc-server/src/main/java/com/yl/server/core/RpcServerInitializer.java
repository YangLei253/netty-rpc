package com.yl.server.core;

import com.yl.common.codec.*;
import com.yl.common.serializer.Serializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// Channel 初始化器
public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {
    // 存放服务接口与服务实现类的映射关系，应用于 RpcServerHandler
    private final Map<String, Object> serviceMap;
    // 存放线程池实现，同样应用于 RpcServerHandler
    private final ThreadPoolExecutor threadPoolExecutor;
    // 存放序列化实现
    private final Serializer serializer;

    public RpcServerInitializer(Map<String, Object> serviceMap, ThreadPoolExecutor threadPoolExecutor, Serializer serializer) {
        this.serviceMap = serviceMap;
        this.threadPoolExecutor = threadPoolExecutor;
        this.serializer = serializer;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline cp = socketChannel.pipeline();

        // 添加心跳检测机制
        cp.addLast(new IdleStateHandler(0, 0, BEAT.BEAT_TIMEOUT, TimeUnit.SECONDS));
        // 此 Handler 专门用于处理拆包问题。鉴于 RpcDecoder 已经手工实现拆包，因此可以忽略它。
        // cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcRequest.class, this.serializer));
        cp.addLast(new RpcEncoder(RpcResponse.class, this.serializer));
        cp.addLast(new RpcServerHandler(this.serviceMap, this.threadPoolExecutor));

    }
}

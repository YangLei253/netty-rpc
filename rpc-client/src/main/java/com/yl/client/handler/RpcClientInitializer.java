package com.yl.client.handler;

import com.yl.common.codec.*;
import com.yl.common.serializer.Serializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    // 存放序列化实现
    private final Serializer serializer;

    public RpcClientInitializer(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline cp = socketChannel.pipeline();

        cp.addLast(new IdleStateHandler(0, 0, BEAT.BEAT_INTERVAL, TimeUnit.SECONDS));
        // 此 Handler 专门用于处理拆包问题。鉴于 RpcDecoder 已经手工实现拆包，因此可以忽略它。
        // cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcResponse.class, this.serializer));
        cp.addLast(new RpcEncoder(RpcRequest.class, this.serializer));
        cp.addLast(new RpcClientHandler());
    }
}

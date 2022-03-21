package com.yl.client.handler;

import com.yl.client.connection.ConnectionManager;
import com.yl.common.codec.BEAT;
import com.yl.common.codec.RpcRequest;
import com.yl.common.codec.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    // 等待执行结果的 RpcFuture 集合
    private ConcurrentHashMap<String, CompletableFuture<Object>> pendingRPC = new ConcurrentHashMap<>();
    // 此 handler 对应的 Channel 与远程地址
    private volatile Channel channel;
    private SocketAddress remoteAddress;
    // 远程地址提供的服务信息 (仅应用于 ChannelInactive 时，从而移除无用连接)
    private String server;

    public void setServer(String server) {
        this.server = server;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        String requestId = rpcResponse.getRequestId();
        CompletableFuture<Object> future = this.pendingRPC.get(requestId);
        if (future == null) {
            logger.warn("Can not get pending response for request id: " + requestId);
        } else {
            this.pendingRPC.remove(requestId);
            future.complete(rpcResponse.getResult());
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
        this.remoteAddress = ctx.channel().remoteAddress();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConnectionManager.getInstance().removeAndCloseHandler(this.server);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Client caught exception: " + cause.getMessage());
        ctx.close();
    }

    public void close() {
        // 写入 EMPTY_BUFFER 保证，以往数据均已被写出。
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }


    // 发送用户请求
    public CompletableFuture<Object> sendRequest(RpcRequest request) throws InterruptedException {
        CompletableFuture<Object> future = new CompletableFuture();
        this.pendingRPC.put(request.getRequestId(), future);

        this.channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (!channelFuture.isSuccess()) {
                    logger.error("Send request {} error", request.getRequestId());
                }
            }
        });

        return future;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 定期发送心跳，从而维持长连接
        if (evt instanceof IdleStateEvent) {
            sendRequest(BEAT.BEAT_PING);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}

package com.yl.server.core;

import com.yl.common.codec.BEAT;
import com.yl.common.codec.RpcRequest;
import com.yl.common.codec.RpcResponse;
import com.yl.common.util.ServiceUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

// Netty 核心的 Handler 实现
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);
    // 存放服务接口与服务实现类的映射关系
    private final Map<String, Object> serviceMap;
    // 将具体服务的调用工作，放置于线程池内，采用异步处理。
    private final ThreadPoolExecutor handlePool;

    public RpcServerHandler(Map<String, Object> serviceMap, ThreadPoolExecutor handlePool) {
        this.serviceMap = serviceMap;
        this.handlePool = handlePool;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Server caught exception: " + cause.getMessage());
        ctx.close();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext channelHandlerContext, final RpcRequest rpcRequest) throws Exception {
        // 心跳包无需处理，直接过滤即可。
        if (rpcRequest.getRequestId().equals(BEAT.BEAT_ID)) {
            logger.info("Server get heartbeat ping");
            return;
        }

        handlePool.execute(new Runnable() {
            @Override
            public void run() {
                logger.info("Receive request: " + rpcRequest);

                RpcResponse response = new RpcResponse();
                response.setRequestId(rpcRequest.getRequestId());

                try {
                    Object result = handle(rpcRequest);
                    response.setResult(result);
                } catch (Throwable throwable) {
                    response.setError(throwable);
                    logger.error("RPC Server handle request error：" + throwable);
                }

                channelHandlerContext.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.info("Send response for request " + rpcRequest);
                    }
                });
            }
        });
    }

    // 实际的调用服务实现 (采用 Cglib 实现)
    private Object handle(RpcRequest request) throws Throwable {
        String className = request.getClassName();
        String version  = request.getVersion();
        String serviceKey = ServiceUtil.makeServiceKey(className, version);
        Object bean = this.serviceMap.get(serviceKey);

        if (bean == null) {
            logger.error("Can not find service implement with class name: {} and version: {}", className, version);
            throw new RuntimeException("The service is not supported");
        }

        Class<?> serviceClass = bean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        // 借助于 Cglib，反射执行具体方法，并返回结果。
        FastClass serviceFastClass = FastClass.create(serviceClass);
        int methodIndex = serviceFastClass.getIndex(methodName, parameterTypes);
        return serviceFastClass.invoke(methodIndex, bean, parameters);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 如果超时触发，则关闭此次连接。
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            logger.warn("Channel is idle, so close it");
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}

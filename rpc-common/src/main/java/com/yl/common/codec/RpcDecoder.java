package com.yl.common.codec;

import com.yl.common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// 将 Byte[] 解码为 RpcRequest/RpcResponse，应用于 Netty Handler。
// 如果收到的数据，不满足一条应用层消息，则后续无需处理，直接 return；否则才将对象放置于 list，然后向后传递。
public class RpcDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);

    // 待序列化的类型、序列化实现
    private Class<?> genericClass;
    private Serializer serializer;

    public RpcDecoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() < 4) {
            return;
        }

        byteBuf.markReaderIndex();

        int length = byteBuf.readInt();

        if (byteBuf.readableBytes() < length) {
            byteBuf.resetReaderIndex();
            return;
        }

        byte[] data = new byte[length];
        byteBuf.readBytes(data);
        Object object = null;

        try {
            object = serializer.deserialize(data, this.genericClass);
            list.add(object);
        } catch (Exception e) {
            logger.error("Decode error: " + e.toString());
        }
    }
}

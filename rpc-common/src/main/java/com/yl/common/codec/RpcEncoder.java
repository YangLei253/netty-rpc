package com.yl.common.codec;

import com.yl.common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// 将 RpcRequest/RpcResponse 编码为 Byte[]，应用于 Netty Handler。
// 具体的编码格式 (假定对象序列化的结果为 data)：data.length + data (**可以看做传输层上的一个简单的应用层协议**)
public class RpcEncoder extends MessageToByteEncoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);

    // 待序列化的类型、序列化实现
    private Class<?> genericClass;
    private Serializer serializer;

    public RpcEncoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        if (genericClass.isInstance(o)) {
            try {
                byte[] data = serializer.serialize(o);
                byteBuf.writeInt(data.length);
                byteBuf.writeBytes(data);
            } catch (Exception e) {
                logger.error("Encode error: " + e.toString());
            }
        }
    }
}

package com.yl.common.codec;

// 服务端提供空闲连接检测，客户端为避免被清掉，因此需要定期发送 BEAT，以维持连接。
public class BEAT {
    // BEAT 间隔时间 (应用于客户端)、BEAT 超时时间 (应用于服务器)
    // 为保证客户端能够进行长连接，需要使得前者小于后者，并且需要尽可能小，以满足服务端的维持长连接需要。
    public static final int BEAT_INTERVAL = 30;
    public static final int BEAT_TIMEOUT = 3 * BEAT_INTERVAL;
    public static final String BEAT_ID = "BEAT_ID";

    // 客户端使用的心跳报文。
    public static final RpcRequest BEAT_PING;

    static {
        BEAT_PING = new RpcRequest();
        BEAT_PING.setRequestId(BEAT_ID);
    }
}

netty-rpc
An RPC framework based on Netty, ZooKeeper and Spring, that is implemented based on https://my.oschina.net/huangyong/blog/361751.

Features:
- Simple code and framework
- Support different load balance strategy
- Support asynchronous/synchronous call (based on CompletableFuture)
- Support different serializer/deserializer
- Dead TCP connection detecting with heartbeat
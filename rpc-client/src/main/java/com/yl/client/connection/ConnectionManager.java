package com.yl.client.connection;

import com.yl.client.discovery.ServiceDiscovery;
import com.yl.client.handler.RpcClientHandler;
import com.yl.client.handler.RpcClientInitializer;
import com.yl.client.route.RpcLoadBalance;
import com.yl.client.route.impl.RpcLoadBalanceRoundRobin;
import com.yl.common.serializer.kryo.KryoSerializer;
import com.yl.common.util.ThreadPoolUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// 客户端的核心实现
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    // 构建默认的 handler
    private static RpcClientHandler DEFAULT_HANDLER = new RpcClientHandler();
    private static RpcClientHandler ERROR_HANDLER = new RpcClientHandler();

    // 单例
    private static ConnectionManager singleton;
    // serviceDiscovery 实现
    private ServiceDiscovery serviceDiscovery;

    // 线程池，用于 Client 与服务节点的连接 (指代 Netty 方式，实际运行 BootStrap 的那个线程，直接会阻塞)
    private ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.getInstance(ConnectionManager.class.getSimpleName(), 8, 16);
    // Netty 方式启动所使用的 EventLoopGroup
    private EventLoopGroup worker = new NioEventLoopGroup(8);

    // RpcNode 与实际连接于此的 Client 的映射关系
    // RpcNode 一定有值，RpcClientHandler 具有三种情况：defaultHandler 表示初始化节点、null 表示连接失败、其余值表示连接成功。
    // private Map<RpcNode, RpcClientHandler> connectedServers = new ConcurrentHashMap<>();

    // server 与实际连接于此的 Client 的映射集合
    // RpcClientHandler 具有三种情况：defaultHandler 表示初始化节点、ERROR_HANDLER 表示连接失败、其余值表示连接成功。
    private Map<String, RpcClientHandler> connectedServers = new ConcurrentHashMap<>();

    // 前者用于同步互斥，后者用于阻塞等待可用的 RpcClientHandler
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    // 等待的基本时间
    private long waitTimeOut = 5000;
    // 选择 Handler 的策略
    private RpcLoadBalance balance = new RpcLoadBalanceRoundRobin();
    // 此实例是否正在运行
    private volatile boolean isRunning = true;

    private ConnectionManager() {}

    public static synchronized ConnectionManager getInstance() {
        if (singleton == null) {
            singleton = new ConnectionManager();
        }

        return singleton;
    }

    public void setServiceDiscovery(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery =  serviceDiscovery;
    }

    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        String server = this.balance.route(serviceDiscovery.getServerList(serviceKey));

        RpcClientHandler handler = this.connectedServers.get(server);

        // 此时，可能尚未建立连接，也可能已有线程开始创建连接，根据不同情况，执行不同的处理过程。
        while (isRunning && (handler == null || handler == DEFAULT_HANDLER || handler == ERROR_HANDLER)) {
            if (handler == ERROR_HANDLER) {
                throw new Exception("Can not get available connection");
            } else if (handler == null) {
                this.lock.lock();
                try {
                    if (this.connectedServers.get(server) == null) {
                        this.connectedServers.put(server, DEFAULT_HANDLER);
                        connectServer(server);
                    }
                } finally {
                    this.lock.unlock();
                }
            }

            waitingForHandler();
            handler = this.connectedServers.get(server);
        }

        if (!isRunning) {
            throw new Exception("Can not get available connection");
        }

        return handler;
    }

    private boolean waitingForHandler() throws InterruptedException {
        this.lock.lock();

        try {
            return this.connected.await(this.waitTimeOut, TimeUnit.MILLISECONDS);
        } finally {
            this.lock.unlock();
        }
    }

    private void signalAvailableHandler() {
        this.lock.lock();
        try {
            this.connected.signalAll();
        } finally {
            this.lock.unlock();
        }
    }


    private void connectServer(String server) {
        String[] addresss = server.split(":");
        // 获取具体地址，尝试建立长连接
        InetSocketAddress remoteAddress = new InetSocketAddress(addresss[0], Integer.parseInt(addresss[1]));

        // 以异步方式建立连接
        this.threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(worker).channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer(new KryoSerializer()));

                ChannelFuture future = b.connect(remoteAddress);

                // 真正连接成功后，填充 connectedServers 字段。
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            logger.info("Successfully connect to remote server, remote peer = " + remoteAddress);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            connectedServers.put(server, handler);
                            handler.setServer(server);
                            signalAvailableHandler();
                        } else {
                            connectedServers.put(server, ERROR_HANDLER);
                            logger.error("Can not connect to remote server, remote peer = " + remoteAddress);
                        }
                    }
                });
            }
        });
    }

    public void close() {
        if (isRunning) {
            isRunning = false;

            signalAvailableHandler();

            this.threadPoolExecutor.shutdown();
            this.worker.shutdownGracefully();

            String[] servers = new String[this.connectedServers.size()];
            this.connectedServers.keySet().toArray(servers);

            for (String server : servers) {
                removeAndCloseHandler(server);
            }

           this.connectedServers.clear();
        }
    }

    public void removeAndCloseHandler(String server) {
        RpcClientHandler handler = this.connectedServers.get(server);

        if (handler != null && handler != ERROR_HANDLER) {
            handler.close();
        }

        this.connectedServers.remove(server);
    }

    // 按照此种方式，随时更新 server 信息的方式比较麻烦，直接从 zookeeper 尝试获取。
    // 依据从 zookeeper 获取的最新 rpcNodes，更新 connectedServers 字段。
    // public void updateConnectedNode(List<RpcNode> rpcNodes) {
    //     // 表明 zookeeper 的注册目录下，已经不存在可用服务节点，故而需要清理 connectedServers。
    //     if (rpcNodes == null || rpcNodes.size() == 0) {
    //         logger.info("No available service");
    //
    //         for (RpcNode rpcNode : this.connectedServers.keySet()) {
    //             removeAndCloseHandler(rpcNode);
    //         }
    //         return;
    //     }
    //
    //     HashSet<RpcNode> newRpcNodes = new HashSet<>(rpcNodes);
    //
    //     for (RpcNode newRpcNode : newRpcNodes) {
    //         if (!this.connectedServers.containsKey(newRpcNode)) {
    //             connectServerNode(newRpcNode);
    //         }
    //     }
    //
    //     for (RpcNode rpcNode : this.connectedServers.keySet()) {
    //         if (!newRpcNodes.contains(rpcNode)) {
    //             removeAndCloseHandler(rpcNode);
    //         }
    //     }
    // }

    // 异步与 rpcNode 建立连接
    // private void connectServerNode(RpcNode rpcNode) {
    //     if (rpcNode.getInfos() == null || rpcNode.getInfos().isEmpty()) {
    //         logger.info("No service on node, host: {}, port: {}", rpcNode.getHost(), rpcNode.getPort());
    //         return;
    //     }
    //
    //     this.connectedServers.putIfAbsent(rpcNode, DEFAULT_HANDLER);
    //
    //     // 获取此 Node 的远程地址，然后建立长连接
    //     InetSocketAddress remoteAddress = new InetSocketAddress(rpcNode.getHost(), rpcNode.getPort());
    //
    //     // 以异步方式建立连接
    //     this.threadPoolExecutor.submit(new Runnable() {
    //         @Override
    //         public void run() {
    //             Bootstrap b = new Bootstrap();
    //             b.group(worker).channel(NioSocketChannel.class)
    //                     .handler(new RpcClientInitializer(new KryoSerializer()));
    //
    //             ChannelFuture future = b.connect(remoteAddress);
    //
    //             // 真正连接成功后，填充 connectedServers 字段。
    //             future.addListener(new ChannelFutureListener() {
    //                 @Override
    //                 public void operationComplete(ChannelFuture channelFuture) throws Exception {
    //                     if (channelFuture.isSuccess()) {
    //                         logger.info("Successfully connect to remote server, remote peer = " + remoteAddress);
    //                         RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
    //                         connectedServers.put(rpcNode, handler);
    //                         handler.setNode(rpcNode);
    //
    //                         signalAvailableHandler();
    //                     } else {
    //                         connectedServers.put(rpcNode, null);
    //                         logger.error("Can not connect to remote server, remote peer = " + remoteAddress);
    //                     }
    //                 }
    //             });
    //         }
    //     });
    // }


    // private void signalAvailableHandler() {
    //     this.lock.lock();
    //     try {
    //         this.connected.signalAll();
    //     } finally {
    //         this.lock.unlock();
    //     }
    // }

    // public void removeAndCloseHandler(RpcNode rpcNode) {
    //     RpcClientHandler handler = this.connectedServers.get(rpcNode);
    //
    //     if (handler != null && handler != DEFAULT_HANDLER) {
    //         handler.close();
    //     }
    //
    //     this.connectedServers.remove(rpcNode);
    // }

    // 按照指定要求，更新 rpcNodes/connectedServers 字段。
    // public void updateConnectedNode(RpcNode node, PathChildrenCacheEvent.Type type) {
    //     if (node == null) {
    //         return;
    //     }
    //
    //     if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !this.connectedServers.keySet().contains(node)) {
    //         connectServerNode(node);
    //     } else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
    //         removeAndCloseHandler(node);
    //         connectServerNode(node);
    //     } else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
    //         removeAndCloseHandler(node);
    //     } else {
    //         throw new IllegalArgumentException("Unknow type:" + type);
    //     }
    // }

    // 依据接口与版本，选择合适的 RpcClientHandler
    // public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
    //     int size = this.connectedServers.values().size();
    //
    //     // 此处循环退出，则表明存在 handler 可用。
    //     while (this.isRunning && size <= 0) {
    //         waitingForHandler();
    //         size = this.connectedServers.values().size();
    //     }
    //
    //     // 如果没有找到对应的 RpcNode，则其会抛出异常。
    //     RpcNode node = this.balance.route(serviceKey, this.connectedServers);
    //
    //
    //     RpcClientHandler handler = this.connectedServers.get(node);
    //
    //     // handler 为 DEFAULT_HANDLER，则表明其正在执行连接操作，需要等待
    //     while (handler == DEFAULT_HANDLER) {
    //         waitingForHandler();
    //         handler = this.connectedServers.get(node);
    //     }
    //
    //     // handler 非空，则表明成功连接，否则表示无法连接。
    //     if (handler != null) {
    //         return handler;
    //     } else {
    //         throw new Exception("Can not get available connection");
    //     }
    // }

    // private boolean waitingForHandler() throws InterruptedException {
    //     this.lock.lock();
    //
    //     try {
    //         return this.connected.await(this.waitTimeOut, TimeUnit.MILLISECONDS);
    //     } finally {
    //         this.lock.unlock();
    //     }
    // }

    // public void close() {
    //     if (isRunning) {
    //         isRunning = false;
    //
    //         signalAvailableHandler();
    //         for (RpcNode node : this.connectedServers.keySet()) {
    //             removeAndCloseHandler(node);
    //         }
    //
    //         this.threadPoolExecutor.shutdown();
    //         this.worker.shutdownGracefully();
    //     }
    // }
}

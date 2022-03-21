package com.yl.common.zookeeper;

import com.yl.common.config.Constant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;

// zookeeper 客户端操作
public class CuratorClient {
    private CuratorFramework client;

    // connectString 指定 zk 服务器的 IP+Port；namespace 类似于 Linux 的当前目录，后续所有路径均会前缀此路径；
    // sessionTimeout 指代会话超时时间；conectionTimeout 指代连接超时时间；
    // ExponentialBackoffRetry 指代连接超时后选择的策略，此处的意思指代初次连接超时后，sleep 1000ms，然后重试，连接成功则结束，否则继续，最多重试 5 次
    public CuratorClient(String connectString, String namespace, int sessionTimeout, int connectionTimeout) {
        this.client = CuratorFrameworkFactory.builder()
                .connectString(connectString).namespace(namespace)
                .sessionTimeoutMs(sessionTimeout).connectionTimeoutMs(connectionTimeout)
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();

        // 阻塞，直至连接成功。
        this.client.start();
    }

    public CuratorClient(String connectString, int timeout) {
        this(connectString, Constant.ZK_NAMESPACE, timeout, timeout);
    }

    public CuratorClient(String connectString) {
        this(connectString, Constant.ZK_NAMESPACE, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorFramework getClient() {
        return this.client;
    }

    // 如果客户端状态发生变化，可以执行相关操作 (举例：如果客户端因阻塞而导致 session 超时，从而 zookeeper 关闭此连接。Curator 会启动重连策略，但是此时原有信息便会消息，需要借助于此，完成相关节点的信息恢复)。
    public void addConnectionStateListener(ConnectionStateListener connectionStateListener) {
        this.client.getConnectionStateListenable().addListener(connectionStateListener);
    }

    // 向 zookeeper 创建 Path 指代的节点，并向其中存储 data 数据。
    public String createPathData(String path, byte[] data) throws Exception {
        return this.client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL).forPath(path, data);
    }

    // 更新节点的数据
    public void updatePathData(String path, byte[] data) throws Exception {
        this.client.setData().forPath(path, data);
    }

    // 删除指定节点
    public void deletePath(String path) throws Exception {
        this.client.delete().forPath(path);
    }

    // 获取节点的数据
    public byte[] getData(String path) throws Exception {
        return this.client.getData().forPath(path);
    }

    // 获取节点的子节点的名称
    public List<String> getChildren(String path) throws Exception {
        return this.client.getChildren().forPath(path);
    }

    // 监听指定节点 (与原生 zookeeper 的监听机制相同，均是一次性的)
    public void watchNode(String path, CuratorWatcher watcher) throws Exception {
        this.client.getData().usingWatcher(watcher).forPath(path);
    }

    // 监听指定节点 (既监听节点的创建和更新，也监听其下子节点的创建、更新、删除)，一次注册，永久监听
    public void watchTreeNode(String path, TreeCacheListener listener) {
        TreeCache cache = new TreeCache(this.client, path);
        cache.getListenable().addListener(listener);
    }

    // 监听指定节点 (仅监听其下子节点的创建、更新、删除)，一次注册，永久监听
    public void watchPathChildrenNode(String path, PathChildrenCacheListener listener) throws Exception {
        PathChildrenCache cache = new PathChildrenCache(this.client, path, true);
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        cache.getListenable().addListener(listener);
    }

    // 关闭连接
    public void close() {
        this.client.close();
    }
}

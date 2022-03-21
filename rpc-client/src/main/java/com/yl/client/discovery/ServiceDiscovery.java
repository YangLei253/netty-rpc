package com.yl.client.discovery;

import com.yl.client.connection.ConnectionManager;
import com.yl.common.config.Constant;
import com.yl.common.zookeeper.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


// 客户端的 zookeeper，负责发现服务
public class ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);

    // 借助于 Curator，连接远程的 zookeeper
    private CuratorClient client;

    public ServiceDiscovery(String registryAddress)  {
        this.client = new CuratorClient(registryAddress);
        ConnectionManager.getInstance().setServiceDiscovery(this);

        // discoveryService();
    }

    public List<String> getServerList(String serviceKey) {
        List<String> servers = null;
        try {
            servers = this.client.getChildren(Constant.ZK_DATA_PATH + "/" + serviceKey);

            this.client.watchPathChildrenNode(Constant.ZK_DATA_PATH + "/" + serviceKey, new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                    PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                    ChildData data = pathChildrenCacheEvent.getData();

                    if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                        logger.info("server is removed");
                        String server = data.getPath().substring(data.getPath().lastIndexOf("/") + 1);
                        ConnectionManager.getInstance().removeAndCloseHandler(server);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Get servers exception: " + e.getMessage());
        }

        return servers;
    }

    // private void discoveryService() {
    //     try {
    //         logger.info("Get initial service info");
    //
    //         getServiceAndUpdateServer();
    //
    //         // 初始化更新完成后，需要添加监听器，以根据后续的 watch 结果，从而执行相关的更新操作。
    //         this.client.watchPathChildrenNode(Constant.ZK_REGISTRY_PATH, new PathChildrenCacheListener() {
    //             @Override
    //             public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
    //                 PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
    //                 ChildData data = pathChildrenCacheEvent.getData();
    //
    //                 switch (type) {
    //                     case CONNECTION_RECONNECTED:
    //                         logger.info("Reconnected to zookeeper, try to get latest service list");
    //                         getServiceAndUpdateServer();
    //                         break;
    //                     case CHILD_ADDED:
    //                         logger.info("node is added");
    //                         getServiceAndUpdateServer(data, type);
    //                         break;
    //                     case CHILD_UPDATED:
    //                         logger.info("node is updated");
    //                         getServiceAndUpdateServer(data, type);
    //                         break;
    //                     case CHILD_REMOVED:
    //                         logger.info("node is removed");
    //                         getServiceAndUpdateServer(data, type);
    //                 }
    //             }
    //         });
    //     } catch (Exception e) {
    //         logger.error("Watch node exception: " + e.getMessage());
    //     }
    // }

    // 从 zookeeper 的注册目录中，获取服务节点的信息，并将其更新至 ConnectionManager。
    // private void getServiceAndUpdateServer() {
    //     try {
    //         // 获取 RegistryPath 下的子节点
    //         List<String> nodeList = this.client.getChildren(Constant.ZK_REGISTRY_PATH);
    //         List<RpcNode> rpcNodes = new ArrayList<>();
    //
    //         // 解析子节点的数据，统一放置于 rpcNodes
    //         for (String node : nodeList) {
    //             byte[] data = this.client.getData(Constant.ZK_REGISTRY_PATH + "/" + node);
    //             RpcNode rpcNode = RpcNode.fromJson(new String(data));
    //             rpcNodes.add(rpcNode);
    //         }
    //
    //         updateConnectedServer(rpcNodes);
    //     } catch (Exception e) {
    //         logger.error("Get node exception: " + e.getMessage());
    //     }
    // }

    // 更新服务信息给 ConnectionManager
    // private void updateConnectedServer(List<RpcNode> rpcNodes) {
    //     ConnectionManager.getInstance().updateConnectedNode(rpcNodes);
    // }

    // 从 zookeeper 的指定目录中，获取服务节点信息，并将其更新至 ConnectionManager
    // private void getServiceAndUpdateServer(ChildData childData, PathChildrenCacheEvent.Type type) {
    //     try {
    //         String data = new String(this.client.getData(childData.getPath()));
    //         RpcNode node = RpcNode.fromJson(data);
    //         updateConnectedServer(node, type);
    //     } catch (Exception e) {
    //         logger.error("Get node exception: " + e.getMessage());
    //     }
    //
    // }

    // 更新服务信息给 ConnectionManager
    // private void updateConnectedServer(RpcNode node, PathChildrenCacheEvent.Type type) {
    //     ConnectionManager.getInstance().updateConnectedNode(node, type);
    // }

    // 关闭此客户端
    public void close() {
        this.client.close();
    }
}

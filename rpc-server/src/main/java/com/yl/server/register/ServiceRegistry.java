package com.yl.server.register;

import com.yl.common.config.Constant;
import com.yl.common.zookeeper.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

// 完成 Server 到 zookeeper 的注册
// 最初实现：直接将当前 Server 所提供的服务放置到 RpcNode 当中，然后将其发往 zookeeper。此种方式虽然简化服务方，但是在客户端难于实现负载均衡。
// 采用与 Dubbo 类似做法，以 service 为目录，其内存储相应的 server 信息，按照此种方式进行组织，从而简化客户端的负载均衡实现。
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    // zookeeper 客户端
    private CuratorClient client;
    // 存放服务注册到 zookeeper 的路径
    // private String path;

    // 存放当前服务器的信息
    private String server;
    // 保存服务列表信息
    private Map<String, Object> serviceMap;


    public ServiceRegistry(String registerAddress) {
        this.client = new CuratorClient(registerAddress);
    }

    // 注册服务 (核心点即是填充 RpcNode 信息，并将其传到 zookeeper)
    public void registerService(final String host, final int port, final Map<String, Object> serviceMap) {
        this.serviceMap = serviceMap;

        this.server = host + ":" + port;

        for (String key : serviceMap.keySet()) {
            String path = Constant.ZK_DATA_PATH + "/" + key + "/" + this.server;
            try {
                this.client.createPathData(path, new byte[0]);
                logger.info("Register new service node, host: {}, port: {}", host, port);
            } catch (Exception e) {
                logger.error("Register service fail, exception: {}", e.getMessage());
            }
        }

        //
        // List<RpcServiceInfo> infos = new ArrayList<>();
        // for (String key : serviceMap.keySet()) {
        //     String[] serviceInfo = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);
        //     if (serviceInfo.length == 2) {
        //         RpcServiceInfo info = new RpcServiceInfo();
        //         info.setClassName(serviceInfo[0]);
        //         info.setVersion(serviceInfo[1]);
        //
        //         logger.info("Register service: " + serviceInfo[0]);
        //         infos.add(info);
        //     } else {
        //         logger.warn("Can't get the name and version of service: " + serviceInfo[0]);
        //     }
        // }

        // try {
        //     RpcNode node = new RpcNode();
        //     node.setHost(host);
        //     node.setPort(port);
        //     node.setInfos(infos);
        //
        //     // 节点的预定路径、节点的数据
        //     byte[] data = node.toString().getBytes();
        //     this.path = Constant.ZK_DATA_PATH + "-" + node.hashCode();
        //
        //     // 因为此处创建的是临时有序数据，因此其实际路径并非上面的预定路径，因此需要重新获取
        //     this.path = this.client.createPathData(path, data);
        //
        //     logger.info("Register new service node, host: {}, port: {}", host, port);
        // } catch (Exception e) {
        //     logger.error("Register service node fail, exception: {}", e.getMessage());
        // }

        // 给此客户端增加监听器，如果发生重连，则重新注册。
        this.client.addConnectionStateListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                if (connectionState == ConnectionState.RECONNECTED) {
                    logger.info("Register service node after reconnected, host: {}, port: {}", host, port);
                    registerService(host, port, serviceMap);
                }
            }
        });
    }

    // 注销服务
    public void unregisterService() {
        logger.info("Unregister service node");

        for (String key : this.serviceMap.keySet()) {
            String path = Constant.ZK_DATA_PATH + "/" + key + "/" + server;
            try {
                this.client.deletePath(path);
            } catch (Exception e) {
                logger.error("Delete service path error: " + e.getMessage());
            }
        }

        // try {
        //     this.client.deletePath(path);
        // } catch (Exception e) {
        //     logger.error("Delete service path error: " + e.getMessage());
        // }

        this.client.close();
    }
}

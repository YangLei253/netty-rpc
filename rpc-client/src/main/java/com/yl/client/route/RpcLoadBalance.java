package com.yl.client.route;

import java.util.List;

// 负载均衡接口定义
public abstract class RpcLoadBalance {
    // 将 RpcNode 依据 serviceKey 进行分组，Map<String, List<RpcNode>> String 指代某 serviceKey，List<Node> 指代提供此功能的 RpcNode 集合。
    // protected Map<String, List<RpcNode>> getServiceMap(Map<RpcNode, RpcClientHandler> connectedServers) {
    //     if (connectedServers == null || connectedServers.isEmpty()) {
    //         return null;
    //     }
    //
    //     Map<String, List<RpcNode>> serviceMap = new HashMap<>();
    //     for (RpcNode rpcNode : connectedServers.keySet()) {
    //         List<RpcServiceInfo> infos = rpcNode.getInfos();
    //         for (RpcServiceInfo info : infos) {
    //             String serviceKey = ServiceUtil.makeServiceKey(info.getClassName(), info.getVersion());
    //
    //             List<RpcNode> list = serviceMap.get(serviceKey);
    //
    //             if (list == null) {
    //                 list = new ArrayList<>();
    //             }
    //
    //             list.add(rpcNode);
    //             serviceMap.putIfAbsent(serviceKey, list);
    //         }
    //     }
    //
    //     return serviceMap;
    // }

    // public abstract RpcNode route(String serviceKey, Map<RpcNode, RpcClientHandler> connectedServers) throws Exception;

    // 上面为旧实现版本，下面为新的实现版本。
    // 因为采用新的方式存储服务信息到 zookeeper，因此负载均衡实现很简单
    public abstract String route(List<String> servers) throws Exception;
}

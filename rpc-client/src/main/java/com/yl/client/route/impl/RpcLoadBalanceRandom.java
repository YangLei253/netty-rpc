package com.yl.client.route.impl;

import com.yl.client.route.RpcLoadBalance;

import java.util.List;
import java.util.Random;

// 随机负载均衡
public class RpcLoadBalanceRandom extends RpcLoadBalance {
    private Random random = new Random();

    private String doRount(List<String> servers) {
        int index = this.random.nextInt(servers.size());
        return servers.get(index);
    }

    @Override
    public String route(List<String> servers) throws Exception {
        return doRount(servers);
    }


    // private RpcNode doRount(List<RpcNode> nodes) {
    //     int index = this.random.nextInt(nodes.size());
    //     return nodes.get(index);
    // }
    //
    // @Override
    // public RpcNode route(String serviceKey, Map<RpcNode, RpcClientHandler> connectedServers) throws Exception {
    //     Map<String, List<RpcNode>> serviceMap = getServiceMap(connectedServers);
    //     List<RpcNode> rpcNodes = serviceMap.get(serviceKey);
    //
    //     if (rpcNodes == null && rpcNodes.isEmpty()) {
    //         throw new Exception("Can not find connection for service: " + serviceKey);
    //     } else {
    //         return doRount(rpcNodes);
    //     }
    // }
}

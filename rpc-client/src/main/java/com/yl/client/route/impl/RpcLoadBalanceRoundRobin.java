package com.yl.client.route.impl;

import com.yl.client.route.RpcLoadBalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// 轮询负载均衡 (极其简单)
public class RpcLoadBalanceRoundRobin extends RpcLoadBalance {
    private AtomicInteger roundRobin = new AtomicInteger();

    private String doRount(List<String> servers) {
        int num = this.roundRobin.getAndIncrement();
        // 一路递增，最终将会溢出为负值，借助于 abs()，保证其总是位于 [0,size) 内。
        int index = Math.abs(num % servers.size());
        return servers.get(index);
    }

    @Override
    public String route(List<String> servers) throws Exception {
        return doRount(servers);
    }

    // private RpcNode doRount(List<RpcNode> nodes) {
    //     int num = this.roundRobin.getAndIncrement();
    //     // 一路递增，最终将会溢出为负值，借助于 abs()，保证其总是位于 [0,size) 内。
    //     int index = Math.abs(num % nodes.size());
    //     return nodes.get(index);
    // }
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

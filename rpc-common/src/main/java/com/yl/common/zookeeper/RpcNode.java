package com.yl.common.zookeeper;

import com.yl.common.util.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

// 已失效
// 机器所提供的服务信息，将组织为此。其将作为 zookeeper 节点的数据内容 (主要应用于 ServiceRegistry)。
public class RpcNode implements Serializable {
    // 服务对应的 host
    private String host;
    // 服务对应的 port
    private int port;
    // 服务信息集合
    private List<RpcServiceInfo> infos;

    // 借助于 JSON 恢复得到 RpcNode
    public static RpcNode fromJson(String json) {
        return JsonUtil.jsonToObject(json, RpcNode.class);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<RpcServiceInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<RpcServiceInfo> infos) {
        this.infos = infos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcNode rpcNode = (RpcNode) o;
        return port == rpcNode.port &&
                Objects.equals(host, rpcNode.host) &&
                isListEquals(this.infos, rpcNode.infos);
    }

    private boolean isListEquals(List<RpcServiceInfo> thisList, List<RpcServiceInfo> thatList) {
        if (thisList == null && thatList == null) {
            return true;
        }
        if ((thisList == null && thatList != null)
                || (thisList != null && thatList == null)
                || (thisList.size() != thatList.size())) {
            return false;
        }
        return thisList.containsAll(thatList) && thatList.containsAll(thisList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, infos);
    }

    @Override
    public String toString() {
        return JsonUtil.objectToJson(this);
    }
}

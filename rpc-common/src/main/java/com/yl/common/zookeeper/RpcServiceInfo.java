package com.yl.common.zookeeper;

import com.yl.common.util.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

// 已失效
// 用于描述机器所提供的每个服务的信息 (主要应用于 RpcNode)
public class RpcServiceInfo implements Serializable {
    // 服务类
    private String className;
    // 服务版本
    private String version;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcServiceInfo that = (RpcServiceInfo) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(version, that.version);
    }

    @Override
    public String toString() {
        return JsonUtil.objectToJson(this);
    }
}

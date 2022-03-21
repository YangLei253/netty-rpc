package com.yl.common.util;

// 此项目支持 RPC Service 的多版本，因此某接口可能同时存在多个服务实现，因此需要对它们进行唯一化标识。
// 联合 RpcServerHandler 查看，此类的作用便一目了然。
public class ServiceUtil {
    public static final String SERVICE_CONCAT_TOKEN = "#";

    // @NettyRpcService/@NettyRpcAutowired 均已提供 version 默认值，因此 className 和 version 均非空。
    public static String makeServiceKey(String className, String version) {
        return className + SERVICE_CONCAT_TOKEN + version;
    }
}

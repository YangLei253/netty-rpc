package com.yl.server;

import com.yl.common.annotation.NettyRpcService;
import com.yl.common.serializer.kryo.KryoSerializer;
import com.yl.server.core.NettyServer;
import com.yl.server.core.Server;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

// Rpc Server，启动 Server 的前夕工作 (获取含 @NettyRpcService 注解的实现，从而填充 handleMap)
// 借助于 ApplicationContextAware 接口，可以访问到 Spring 所创建的 Bean，从而完成相关工作。
// 借助于 InitializingBean 接口，可在该类被初始化时，启动 Server。
// 借助于 DisposableBean 接口，可在该类被销毁前，关闭 Server。
public class RpcServer implements ApplicationContextAware, InitializingBean, DisposableBean {
    private Server server;

    public RpcServer(String serverAddress, String registryAddress) throws Exception {
        this(serverAddress, registryAddress, KryoSerializer.class.getName());
    }

    public RpcServer(String serverAddress, String registryAddress, String serializer) throws Exception {
        this.server = new NettyServer(serverAddress, registryAddress, serializer);
    }

    @Override
    public void destroy() throws Exception {
        this.server.stop();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.server.start();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(NettyRpcService.class);

        if (beans == null) {
            return;
        }

        for (Object bean : beans.values()) {
            NettyRpcService nettyRpcService = bean.getClass().getAnnotation(NettyRpcService.class);
            String className = nettyRpcService.value().getName();
            String version = nettyRpcService.version();

            this.server.addService(className, version, bean);
        }
    }
}

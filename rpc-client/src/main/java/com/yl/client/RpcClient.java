package com.yl.client;

import com.yl.client.connection.ConnectionManager;
import com.yl.client.discovery.ServiceDiscovery;
import com.yl.client.proxy.ObjectProxy;
import com.yl.client.proxy.RpcServer;
import com.yl.common.annotation.NettyRpcAutowired;
import com.yl.common.util.ThreadPoolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.ThreadPoolExecutor;

// Rpc Client，启动 Client 的前夕工作 (获取含 @RpcAutowired 注解的字段，并为其赋值远程实现)
// 借助于 ApplicationContextAware 接口，可以访问到 Spring 所创建的 Bean，从而完成相关工作。
// 借助于 DisposableBean 接口，可在该类被销毁前，关闭 Server。
public class RpcClient implements ApplicationContextAware, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    // 服务发现
    private ServiceDiscovery serviceDiscovery;
    // 线程池
    private static ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.getInstance("", 16, 16);

    public RpcClient(String registryAddress) {
        this.serviceDiscovery = new ServiceDiscovery(registryAddress);
    }

    // 创建指定 className 的远程实现 (同步版本)
    public static <T> T createService(Class<T> clazz, String version) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, new ObjectProxy<T>(clazz, version));
    }

    // 创建指定 className 的远程实现 (异步版本)
    public static <T> RpcServer createAsyncService(Class<T> clazz, String version) {
        return new ObjectProxy<T>(clazz, version);
    }

    // 提交任务至线程池执行
    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    @Override
    public void destroy() throws Exception {
        this.serviceDiscovery.close();
        threadPoolExecutor.shutdown();
        ConnectionManager.getInstance().close();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beansName = applicationContext.getBeanDefinitionNames();

        for (String beanName : beansName) {
            Object bean = applicationContext.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();

            for (Field field : fields) {
                NettyRpcAutowired nettyRpcAutowired = field.getAnnotation(NettyRpcAutowired.class);
                if (nettyRpcAutowired != null) {
                    String version = nettyRpcAutowired.version();
                    field.setAccessible(true);
                    try {
                        field.set(bean, createService(field.getType(), version));
                    } catch (IllegalAccessException e) {
                        logger.error("field set failed");
                    }
                }
            }
        }
    }
}

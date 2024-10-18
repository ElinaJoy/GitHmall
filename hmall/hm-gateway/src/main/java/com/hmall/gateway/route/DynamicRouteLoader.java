package com.hmall.gateway.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
@Component
public class DynamicRouteLoader {
    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter writer;
//    这里我卡了两天
//    这个dataId加了.json
//    nacos上面的配置也需要加上.json，这个文件名不是自动补全的
//    发现这个错误的时候我人都麻了，没想到这么简单，极具有迷惑性的一点是，我一开始文件名没有加上.json，尝试更新配置时控制台也是有输出的！！！
//    但是显示变更的不是这个文件
//    所以我想着再对一下文件名，才发现了这个错误，太离谱了
    private final String dataId = "gateway-service.json";
    private final String group = "DEFAULT_GROUP";
//    定义一个set（保证不重复）来记录路由表的id
    private final Set<String> routeIds = new HashSet<>();

    @PostConstruct
public void initRouterLoader() throws NacosException {
        System.out.println("最开始");
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
//    1.项目启动，拉取配置并且添加监听器
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }
//    2.一旦监听到配置发生了变化，就去拉取新的配置并且需要更新路由表
                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        log.info("正在实现receiveConfigInfo方法,获取到的配置信息如下：{}", configInfo);
                        updateRoutes(configInfo);

                    }
                });
        System.out.println("configInfo为：" + configInfo);
        System.out.println("其次");
//    3.第一次读取到配置也需要做路由表更新
        updateRoutes(configInfo);
        System.out.println("最后");

    }
//    可以看到这里需要对拉取到的配置文件做解析，因此nacos里面的配置用熟悉的json会更好解析
    public void updateRoutes(String configInfo) {
        log.info("监听到路由配置信息的更新:{}",configInfo);
//        1.解析拉取到的路由配置，转换为列表
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        routeIds.forEach(routeId->{
            writer.delete(Mono.just(routeId)).subscribe();
        });
        routeIds.clear();
        if(CollUtil.isEmpty(routeDefinitions)){
            return;
        }
//        2.更新路由表
        routeDefinitions.forEach(rd->{
//            mono可以看成是存放响应式编程的一种容器
//            这里的save更新只能增，无法删
            writer.save(Mono.just(rd)).subscribe();

//            无需对比不同，先删除所有的路由信息，再写上新的

            routeIds.add(rd.getId());

        });

    }

}

package com.hmall.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//这个可以用来首先nacos的配置热更新
@Component
@ConfigurationProperties(prefix = "hm.cart")
@Data
public class CartProperties {
    private  Integer maxItems;
}

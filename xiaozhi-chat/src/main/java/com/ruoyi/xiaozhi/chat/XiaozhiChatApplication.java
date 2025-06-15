package com.ruoyi.xiaozhi.chat;

import com.ruoyi.xiaozhi.feign.DeviceClient;
import com.ruoyi.xiaozhi.feign.core.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

/**
 * 小智聊天服务启动程序
 */
@Import(cn.hutool.extra.spring.SpringUtil.class)
@SpringBootApplication
@EnableFeignClients(clients = DeviceClient.class, defaultConfiguration = FeignConfig.class)
public class XiaozhiChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaozhiChatApplication.class, args);
    }

}

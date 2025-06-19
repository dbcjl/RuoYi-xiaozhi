package com.ruoyi.xiaozhi.chat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天服务配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat.server")
public class ChatServerProperties {

    /** 聊天服务端口 */
    private Integer port = 8080;

    /** 聊天服务上下文路径 */
    private String contextPath = "/xiaozhi/v1";

}

package com.ruoyi.xiaozhi.chat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 聊天客户端配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat.client")
public class ChatClientProperties {

    /** 访问令牌 */
    private String apiKey;

    /** 服务地址 */
    private String url;

    /** 服务路径 */
    private String path;

    /** 模型名称 */
    private String model;

}

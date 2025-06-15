package com.ruoyi.xiaozhi.chat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 火山引擎 tts 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "model.tts.volc-tts")
public class VolcTTSProperties {

    /** 火山引擎appid */
    private String appid;

    /** 火山引擎accessToken */
    private String accessToken;
}

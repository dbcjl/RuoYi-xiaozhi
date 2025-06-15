package com.ruoyi.xiaozhi.chat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SenseVoice配置
 * @author ruoyi-xiaozhi
 */
@Data
@Component
@ConfigurationProperties(prefix = "model.asr.sense-voice")
public class SenseVoiceProperties {

    /** 模型文件夹路径 */
    private String modelDir;

    /** 模型线程数 */
    private int numThreads = 2;

}

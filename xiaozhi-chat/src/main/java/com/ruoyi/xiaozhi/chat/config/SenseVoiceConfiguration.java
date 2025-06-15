package com.ruoyi.xiaozhi.chat.config;

import com.k2fsa.sherpa.onnx.OfflineModelConfig;
import com.k2fsa.sherpa.onnx.OfflineRecognizer;
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig;
import com.ruoyi.xiaozhi.chat.properties.SenseVoiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * 多语言离线语音识别配置类
 * 配置 SenseVoice 模型及其相关路径
 */
@Slf4j
@Configuration
public class SenseVoiceConfiguration {

    public static final String DEFAULT_MODEL_FILENAME = "model.int8.onnx";
    public static final String DEFAULT_TOKEN_FILENAME = "tokens.txt";

    /**
     * 离线语音识别器 Bean 配置
     */
    @Bean(destroyMethod = "release")
    public OfflineRecognizer speechRecognizer(SenseVoiceProperties properties) {
        String modelDirectory = properties.getModelDir();
        log.info("Loading speech recognizer from directory: {}", modelDirectory);

        String fullModelPath = Paths.get(modelDirectory, DEFAULT_MODEL_FILENAME).toString();
        log.info("Using model file: {}", fullModelPath);

        String fullTokenPath = Paths.get(modelDirectory, DEFAULT_TOKEN_FILENAME).toString();
        log.info("Using token file: {}", fullTokenPath);

        int threadCount = properties.getNumThreads();
        log.info("Recognizer thread count: {}", threadCount);

        OfflineSenseVoiceModelConfig senseVoiceModelConfig = OfflineSenseVoiceModelConfig.builder()
                .setModel(fullModelPath)
                .build();

        OfflineModelConfig offlineModelConfig = OfflineModelConfig.builder()
                .setSenseVoice(senseVoiceModelConfig)
                .setTokens(fullTokenPath)
                .setNumThreads(threadCount)
                .setDebug(false)
                .build();

        OfflineRecognizerConfig recognizerConfig = OfflineRecognizerConfig.builder()
                .setOfflineModelConfig(offlineModelConfig)
                .setDecodingMethod("greedy_search")
                .build();

        return new OfflineRecognizer(recognizerConfig);
    }
}

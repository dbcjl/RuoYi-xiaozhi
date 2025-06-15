package com.ruoyi.xiaozhi.chat.config;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 语音活动检测(VAD)模型配置类
 */
@Slf4j
@Configuration
public class VoiceActivityDetectionConfig {

    public static final String VAD_MODEL_SESSION_BEAN = "voiceActivityDetectionModelSession";

    public static final String VAD_MODEL_RESOURCE_PATH = "model/silero_vad.onnx";

    /**
     * 创建语音活动检测模型会话
     */
    @Bean(name = VAD_MODEL_SESSION_BEAN, destroyMethod = "close")
    public OrtSession createVadModelSession() throws OrtException {
        byte[] modelData = ResourceUtil.readBytes(VAD_MODEL_RESOURCE_PATH);
        log.info("Initializing VAD model session, model path: {}, model size: {} bytes",
                VAD_MODEL_RESOURCE_PATH, modelData.length);

        OrtEnvironment runtimeEnv = OrtEnvironment.getEnvironment();
        return runtimeEnv.createSession(modelData, configureSessionOptions());
    }

    /**
     * 配置ONNX会话选项
     */
    private OrtSession.SessionOptions configureSessionOptions() throws OrtException {
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        // 设置InterOp线程数为1（用于并行处理不同计算图操作）
        options.setInterOpNumThreads(1);
        // 设置IntraOp线程数为1（用于单个操作内部的并行处理）
        options.setIntraOpNumThreads(1);
        // 添加CPU设备，false表示禁用CPU执行优化
        options.addCPU(true);
        return options;
    }
}

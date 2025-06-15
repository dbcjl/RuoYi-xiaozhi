package com.ruoyi.xiaozhi.chat.core.vad;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.k2fsa.sherpa.onnx.VadModelConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Silero语音检测器
 */
public class SileroVoiceActivityDetector {

    /** 模型配置 */
    private final VadModelConfig modelConfig;

    /** 推理会话 */
    private final OrtSession session;

    /** 音频采样率 */
    private final int sampleRate;

    /** 静音最小采样数 */
    @Getter
    private int minSilenceSampleCount;

    /** 语音最小采样数 */
    @Getter
    private int minSpeechSampleCount;

    /** 检测阈值 */
    @Setter
    private float detectThreshold;

    private boolean isTriggered;
    private int totalSampleCount;
    private int speechStartSample;
    private int silenceStartSample;

    /** VAD模型内部状态 */
    private float[][][] modelState;

    public SileroVoiceActivityDetector(VadModelConfig config, OrtSession ortSession) {
        this.modelConfig = config;
        this.session = ortSession;
        this.sampleRate = config.getSampleRate();
        this.detectThreshold = config.getSileroVadModelConfig().getThreshold();

        if (config.getSileroVadModelConfig().getWindowSize() != 512) {
            throw new IllegalArgumentException("Silero v5要求16kHz采样率下窗口大小为512");
        }

        setMinSilenceDuration(config.getSileroVadModelConfig().getMinSilenceDuration());
        setMinSpeechDuration(config.getSileroVadModelConfig().getMinSpeechDuration());
        resetState();
    }

    /** 重置内部状态 */
    public void resetState() {
        this.modelState = new float[2][1][128];
        this.isTriggered = false;
        this.totalSampleCount = 0;
        this.speechStartSample = 0;
        this.silenceStartSample = 0;
    }

    /** 获取每次前移的窗口大小 */
    public int getWindowShift() {
        return this.modelConfig.getSileroVadModelConfig().getWindowSize();
    }

    /** 获取输入样本的窗口大小 */
    public int getInputWindowSize() {
        int overlap = 64;
        return this.modelConfig.getSileroVadModelConfig().getWindowSize() + overlap;
    }

    /** 设置静音最小时长（秒） */
    public void setMinSilenceDuration(float durationSeconds) {
        this.minSilenceSampleCount = (int) (durationSeconds * sampleRate);
    }

    /** 设置语音最小时长（秒） */
    public void setMinSpeechDuration(float durationSeconds) {
        this.minSpeechSampleCount = (int) (durationSeconds * sampleRate);
    }

    /** 判断当前采样是否包含语音 */
    public boolean isSpeech(float[] audioSamples) {
        if (audioSamples.length != getInputWindowSize()) {
            throw new IllegalArgumentException(CharSequenceUtil.format("样本长度不等于窗口大小: {} != {}", audioSamples.length, getInputWindowSize()));
        }

        float speechProb = runModel(audioSamples);
        totalSampleCount += modelConfig.getSileroVadModelConfig().getWindowSize();

        if (speechProb > detectThreshold && silenceStartSample != 0) {
            silenceStartSample = 0;
        }

        if (speechProb > detectThreshold && speechStartSample == 0) {
            speechStartSample = totalSampleCount;
            return false;
        }

        if (speechProb > detectThreshold && speechStartSample != 0 && !isTriggered) {
            if (totalSampleCount - speechStartSample < minSpeechSampleCount) {
                return false;
            }
            isTriggered = true;
            return true;
        }

        if (speechProb < detectThreshold && !isTriggered) {
            speechStartSample = 0;
            silenceStartSample = 0;
            return false;
        }

        if (speechProb > detectThreshold - 0.15 && isTriggered) {
            return true;
        }

        if (speechProb > detectThreshold && !isTriggered) {
            isTriggered = true;
            return true;
        }

        if (speechProb < detectThreshold && isTriggered) {
            if (silenceStartSample == 0) {
                silenceStartSample = totalSampleCount;
            }
            if (totalSampleCount - silenceStartSample < minSilenceSampleCount) {
                return true;
            }
            speechStartSample = 0;
            silenceStartSample = 0;
            isTriggered = false;
            return false;
        }

        return false;
    }

    /** 运行VAD模型，输出语音概率 */
    public float runModel(float[] samples) {
        float[][] inputArray = new float[][]{samples};

        OrtEnvironment env = OrtEnvironment.getEnvironment();

        OnnxTensor inputTensor = null;
        OnnxTensor stateTensor = null;
        OnnxTensor srTensor = null;
        OrtSession.Result outputs = null;

        try {
            inputTensor = OnnxTensor.createTensor(env, inputArray);
            stateTensor = OnnxTensor.createTensor(env, modelState);
            srTensor = OnnxTensor.createTensor(env, new long[]{sampleRate});

            Map<String, OnnxTensor> inputMap = new HashMap<>();
            inputMap.put("input", inputTensor);
            inputMap.put("sr", srTensor);
            inputMap.put("state", stateTensor);

            outputs = session.run(inputMap);

            float[][] result = (float[][]) outputs.get(0).getValue();
            modelState = (float[][][]) outputs.get(1).getValue();

            return result[0][0];
        } catch (OrtException e) {
            throw new IllegalStateException("VAD模型运行异常", e);
        } finally {
            NioUtil.close(inputTensor);
            NioUtil.close(stateTensor);
            NioUtil.close(srTensor);
            NioUtil.close(outputs);
        }
    }
}

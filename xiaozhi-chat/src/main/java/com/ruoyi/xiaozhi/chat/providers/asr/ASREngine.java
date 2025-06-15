package com.ruoyi.xiaozhi.chat.providers.asr;

/**
 * ASR语音转文本统一接口
 */
public interface ASREngine {

    /**
     * 语音转文本
     *
     * @param audioSamples  音频采样数据
     * @param sampleRate    音频采样率
     * @return 文本
     */
    String recognize(float[] audioSamples, int sampleRate);

}

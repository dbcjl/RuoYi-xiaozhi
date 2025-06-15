package com.ruoyi.xiaozhi.chat.providers.asr.sensevoice;

import com.k2fsa.sherpa.onnx.OfflineRecognizer;
import com.k2fsa.sherpa.onnx.OfflineStream;
import com.ruoyi.xiaozhi.chat.providers.asr.ASREngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 离线语音识别引擎（基于SenseVoice技术栈）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ASRSenseVoiceEngine implements ASREngine {

    /** 离线语音解码器实例 */
    private final OfflineRecognizer asrDecoder;

    /**
     * 执行语音识别
     * @param pcmFrame PCM音频帧（32-bit浮点小端格式）
     * @param sampleRateInHz 采样率（支持16000/48000）
     * @return 识别文本
     */
    @Override
    public String recognize(float[] pcmFrame, int sampleRateInHz) {
        OfflineStream decodingSession = null;
        try {
            decodingSession = asrDecoder.createStream();
            decodingSession.acceptWaveform(pcmFrame, sampleRateInHz);
            asrDecoder.decode(decodingSession);
            return asrDecoder.getResult(decodingSession).getText();
        } finally {
            if (decodingSession != null) {
                decodingSession.release();
            }
        }
    }
}

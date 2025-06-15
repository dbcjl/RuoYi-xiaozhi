package com.ruoyi.xiaozhi.chat.core.vad;

import ai.onnxruntime.OrtSession;
import com.k2fsa.sherpa.onnx.SpeechSegment;
import com.k2fsa.sherpa.onnx.VadModelConfig;
import com.ruoyi.xiaozhi.chat.core.RingByteBuffer;
import com.ruoyi.xiaozhi.chat.utils.AudioUtils;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 语音活动检测器
 */
public class VoiceActivityDetector {

    /** 模型配置 */
    private final VadModelConfig config;

    /** Silero语音活动检测模型 */
    private final SileroVoiceActivityDetector model;

    /** 音频环形缓冲区 */
    private final RingByteBuffer buffer;

    /** 最后的缓冲区 */
    private final RingByteBuffer last;

    /** 最大说话样本数 */
    private final int maxUtteranceLength;

    /** 存储说话片段队列 */
    private final Queue<SpeechSegment> segments = new ArrayDeque<>();

    /** 开始偏移量 */
    private int startOffset = -1;

    public VoiceActivityDetector(VadModelConfig config, OrtSession ortSession) {
        this.config = config;
        this.model = new SileroVoiceActivityDetector(config, ortSession);
        // 创建一个20秒的音频缓冲区，因为是字节存储，所以还需要再乘以2
        this.buffer = new RingByteBuffer(20 * config.getSampleRate() * 2);
        // 最后的缓冲区，等于2帧，因为是字节存储，所以还需要再乘以2
        this.last = new RingByteBuffer(config.getSileroVadModelConfig().getWindowSize() * 2 * 2);
        this.maxUtteranceLength = (int) (config.getSileroVadModelConfig().getMaxSpeechDuration() * config.getSampleRate());
    }

    public void acceptWaveform(float[] samples) {
        if (buffer.getCurrentSize() / 2 > this.maxUtteranceLength) {
            model.setMinSilenceDuration(0.1f);
            model.setDetectThreshold(0.9f);
        }else {
            model.setMinSpeechDuration(config.getSileroVadModelConfig().getMinSilenceDuration());
            model.setDetectThreshold(config.getSileroVadModelConfig().getThreshold());
        }

        int windowSize = model.getInputWindowSize();
        int windowShift = model.getWindowShift();

        // 转换为字节数据
        byte[] samplesInBytes = AudioUtils.floatToByte(samples);

        // note length is usually window_size and there is no need to use
        // an extra buffer here
        last.writeBytes(samplesInBytes);

        int lastSize = last.getCurrentSize() / 2;
        if (lastSize < windowSize) {
            return;
        }

        byte[] tempBuffer = new byte[windowSize * 2];

        int k = (lastSize - windowSize) / windowShift + 1;
        boolean isSpeech = false;
        for (int i = 0; i < k; i++) {
            last.peekBytes(tempBuffer);
            boolean thisWindowIsSpeech = model.isSpeech(AudioUtils.byteToFloat(tempBuffer));
            isSpeech = isSpeech || thisWindowIsSpeech;
            last.skipBytes(windowShift * 2);
            buffer.writeBytes(tempBuffer, 0, windowShift * 2);
        }

        if (isSpeech) {
            if (startOffset == -1) {
                // beginning of speech
                int bufferTail = buffer.getCurrentSize() / 2;
                startOffset = Math.max(bufferTail - 2 * windowSize - model.getMinSpeechSampleCount(), 0);
            }
        } else {
            // non-speech
            int bufferSize = buffer.getCurrentSize() / 2;
            if (startOffset != -1 && bufferSize > 0) {
                int end = bufferSize - model.getMinSpeechSampleCount();

                int startInBytes = startOffset * 2;
                int endInBytes = end * 2;
                byte[] speechSamples = new byte[endInBytes - startInBytes];
                // 跳过前面的数据
                buffer.skipBytes(startInBytes);
                buffer.peekBytes(speechSamples);

                SpeechSegment segment = new SpeechSegment(startOffset, AudioUtils.byteToFloat(speechSamples));
                segments.offer(segment);
            }

            if (startOffset == -1) {
                // 防止一直静音，把缓冲区撑爆
                int bufferTail = buffer.getCurrentSize() / 2;
                int n = Math.max(bufferTail - 2 * windowSize - model.getMinSpeechSampleCount(), 0);
                if (n > 0) {
                    buffer.skipBytes(n * 2);
                }
            }

            startOffset = -1;
        }
    }

    /** 语音活动片段是否为空 */
    public boolean isEmpty() {
        return segments.isEmpty();
    }

    /** 弹出第一个活动片段 */
    public SpeechSegment poll() {
        return segments.poll();
    }

    /** 清空活动片段 */
    public void clear() {
        segments.clear();
    }

    /** 获取第一个语音活动片段 */
    public SpeechSegment peek() {
        return segments.peek();
    }

    /** 重置状态 */
    public void reset() {
        segments.clear();

        model.resetState();
        buffer.clear();
        last.clear();

        startOffset = -1;
    }

    /** 是否检测到语音 */
    public boolean isSpeechDetected() {
        return startOffset != -1;
    }

}

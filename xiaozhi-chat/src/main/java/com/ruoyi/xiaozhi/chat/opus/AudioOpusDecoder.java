package com.ruoyi.xiaozhi.chat.opus;

import com.ruoyi.xiaozhi.chat.utils.AudioUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.opus.Opus;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Opus音频流解码处理器
 * @implNote 实现RFC6716标准解码流程
 */
@Slf4j
public class AudioOpusDecoder implements Closeable {

    /** 采样率（单位：Hz） */
    @Getter
    private final int sampleRateInHz;

    /** 声道布局 */
    @Getter
    private final int channelLayout;

    /** Opus解码器上下文句柄 */
    private final long codecContextHandle;

    public AudioOpusDecoder(int sampleRateInHz, int channelLayout) {
        this.sampleRateInHz = sampleRateInHz;
        this.channelLayout = channelLayout;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errorCode = stack.mallocInt(1);
            this.codecContextHandle = Opus.opus_decoder_create(
                    sampleRateInHz,
                    channelLayout,
                    errorCode
            );
            if (errorCode.get(0) != Opus.OPUS_OK) {
                throw new CodecException("OPUS_CODEC_INIT_FAILURE", errorCode.get(0));
            }
        }
    }

    /**
     * 解码Opus封装载荷
     * @param encodedPayload 编码后的Opus数据包
     * @param frameSizeInSamples 每帧采样数（基于48kHz时钟）
     * @return PCM裸数据（s16le格式）
     */
    public synchronized byte[] decodeFrame(byte[] encodedPayload, int frameSizeInSamples) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 输入缓冲区：网络字节序的Opus载荷
            ByteBuffer inputBuffer = stack.malloc(encodedPayload.length)
                    .put(encodedPayload)
                    .flip();

            // 输出缓冲区：交错格式PCM样本
            ShortBuffer pcmSamplesBuffer = stack.mallocShort(
                    frameSizeInSamples * this.channelLayout
            );

            int decodedSampleCount = Opus.opus_decode(
                    codecContextHandle,
                    inputBuffer,
                    pcmSamplesBuffer,
                    frameSizeInSamples,
                    0 // 无FEC纠错
            );

            short[] pcmSamples = new short[decodedSampleCount];
            pcmSamplesBuffer.get(pcmSamples, 0, decodedSampleCount);
            return AudioUtils.shortToByte(pcmSamples);
        }
    }

    @Override
    public void close() {
        log.debug("Releasing OPUS codec resources [handle:{}]", codecContextHandle);
        Opus.opus_decoder_destroy(codecContextHandle);
    }

    /** 自定义编解码异常 */
    private static class CodecException extends RuntimeException {
        public CodecException(String errorCode, int opusError) {
            super(String.format("%s:0x%04X", errorCode, opusError));
        }
    }
}

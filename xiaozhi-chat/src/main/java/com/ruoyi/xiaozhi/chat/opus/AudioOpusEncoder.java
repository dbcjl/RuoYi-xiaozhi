package com.ruoyi.xiaozhi.chat.opus;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.opus.Opus;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * AudioOpusEncoder
 * @author ruoyi-xiaozhi
 */
@Slf4j
public class AudioOpusEncoder implements Closeable {

    /** 音频采样率 */
    @Getter
    private final int samplingRate;

    /** 音频通道数 */
    @Getter
    private final int audioChannels;

    /** Opus编码器原生句柄 */
    private final long encoderNativeHandle;

    /**
     * 创建Opus编码器
     * @param samplingRate    采样率 (Hz)
     * @param audioChannels   音频通道数 (1-单声道, 2-立体声)
     */
    public AudioOpusEncoder(int samplingRate, int audioChannels) {
        this.samplingRate = samplingRate;
        this.audioChannels = audioChannels;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errorCode = stack.mallocInt(1);
            encoderNativeHandle = Opus.opus_encoder_create(
                    samplingRate,
                    audioChannels,
                    Opus.OPUS_APPLICATION_AUDIO,
                    errorCode
            );
            if (errorCode.get(0) != Opus.OPUS_OK) {
                throw new IllegalStateException("Opus编码器初始化失败，错误码: " + errorCode.get(0));
            }
        }
    }

    /**
     * 编码PCM音频数据
     * @param rawPcmData     PCM原始字节数据
     * @param frameSampleCount  每帧样本数量
     * @return  编码后的Opus数据包
     */
    public synchronized byte[] encode(byte[] rawPcmData, int frameSampleCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 准备PCM输入缓冲区（使用直接内存）
            ByteBuffer pcmInputBuffer = stack.malloc(rawPcmData.length)
                    .put(rawPcmData)
                    .flip();
            ShortBuffer pcmSamples = pcmInputBuffer
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();

            ByteBuffer encodedPacketBuffer = stack.malloc(512);

            // 执行编码操作
            int encodedSize = Opus.opus_encode(
                    encoderNativeHandle,
                    pcmSamples,
                    frameSampleCount,
                    encodedPacketBuffer
            );

            if (encodedSize < 0) {
                throw new IllegalStateException("Opus编码失败，错误码: " + encodedSize);
            }

            // 提取编码结果
            byte[] encodedAudio = new byte[encodedSize];
            encodedPacketBuffer.get(encodedAudio);
            return encodedAudio;
        }
    }

    @Override
    public void close() {
        log.debug("释放Opus编码器资源，句柄: {}", encoderNativeHandle);
        Opus.opus_encoder_destroy(encoderNativeHandle);
    }
}

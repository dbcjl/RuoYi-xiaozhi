package com.ruoyi.xiaozhi.chat.core;

import lombok.Data;

import java.nio.ByteBuffer;

/**
 * 音频帧数据对象
 * @author ruoyi-xiaozhi
 */
@Data
public class RawAudioPayload {

    /** 音频帧数据 */
    private byte[] pcmBuffer;

    public RawAudioPayload(ByteBuffer buffer) {
        this(buffer.array());
    }

    public RawAudioPayload(byte[] pcmBuffer) {
        this.pcmBuffer = pcmBuffer;
    }

    public boolean isEmpty() {
        return pcmBuffer == null || pcmBuffer.length == 0;
    }

}

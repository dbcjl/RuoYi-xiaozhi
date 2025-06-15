package com.ruoyi.xiaozhi.chat.providers.tts;

import lombok.Data;

/**
 * 音频处理事件对象，用于TTS处理过程中的状态通知和数据传递
 *
 * @author ruoyi-xiaozhi
 */
@Data
public class AudioEvent {

    /** 事件类型标识 */
    private final EventType eventType;

    /** 音频帧数据 */
    private byte[] data;

    /** 关联文本内容 */
    private String text;

    public AudioEvent(EventType eventType) {
        this.eventType = eventType;
    }

    public AudioEvent(EventType eventType, byte[] data) {
        this.eventType = eventType;
        this.data = data;
    }

    public AudioEvent(EventType eventType, String text) {
        this.eventType = eventType;
        this.text = text;
    }

    /** 音频处理事件类型枚举 */
    public enum EventType {
        /** 音频播放开始事件 */
        PLAYBACK_STARTED,
        /** 句子合成开始事件 */
        SENTENCE_START,
        /** 音频数据帧事件 */
        AUDIO_FRAME,
        /** 句子合成结束事件 */
        SENTENCE_END,
        /** 音频播放结束事件 */
        PLAYBACK_STOPPED
    }
}

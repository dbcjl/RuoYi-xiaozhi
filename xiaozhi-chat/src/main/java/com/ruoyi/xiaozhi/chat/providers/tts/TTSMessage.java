package com.ruoyi.xiaozhi.chat.providers.tts;

import lombok.Data;

/**
 * 文本转语音(TTS)处理消息
 * @author ruoyi-xiaozhi
 */
@Data
public class TTSMessage {

    /** 消息类型 */
    private final Category category;

    /** 文本内容 */
    private String content;

    public TTSMessage(Category category) {
        this.category = category;
    }

    public TTSMessage(Category category, String content) {
        this.category = category;
        this.content = content;
    }

    /** 消息类型枚举 */
    public enum Category {
        /** 文本段开始 */
        SEGMENT_START,
        /** 文本内容 */
        TEXT_CONTENT,
        /** 文本段结束 */
        SEGMENT_END
    }
}

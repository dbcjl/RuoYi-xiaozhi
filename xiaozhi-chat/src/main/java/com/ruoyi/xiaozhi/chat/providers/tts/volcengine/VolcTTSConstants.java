package com.ruoyi.xiaozhi.chat.providers.tts.volcengine;

/**
 * 火山引擎TTS常量
 * @author ruoyi-xiaozhi
 */
public class VolcTTSConstants {
    private VolcTTSConstants() {
    }

    public static final String SERVER_URI = "wss://openspeech.bytedance.com/api/v3/tts/bidirection";

    public static final int PROTOCOL_VERSION = 0b0001;
    public static final int DEFAULT_HEADER_SIZE = 0b0001;
    /** Message Type */
    public static final int FULL_CLIENT_REQUEST = 0b0001;
    public static final int AUDIO_ONLY_RESPONSE = 0b1011;
    public static final int FULL_SERVER_RESPONSE = 0b1001;
    public static final int ERROR_INFORMATION = 0b1111;
    // Message Type Specific Flags
    public static final int MSG_TYPE_FLAG_WITH_EVENT = 0b100;
    // Message Serialization
    public static final int NO_SERIALIZATION = 0b0000;
    public static final int JSON = 0b0001;
    // Message Compression
    public static final int COMPRESSION_NO = 0b0000;
    // event
    // 默认事件,对于使用事件的方案，可以通过非0值来校验事件的合法性
    public static final int EVENT_NONE = 0;
    public static final int EVENT_START_CONNECTION = 1;
    // 上行Connection事件
    public static final int EVENT_FINISH_CONNECTION = 2;
    // 下行Connection事件
    public static final int EVENT_CONNECTION_STARTED = 50; // 成功建连
    public static final int EVENT_CONNECTION_FAILED = 51; // 建连失败（可能是无法通过权限认证）
    public static final int EVENT_CONNECTION_FINISHED = 52; // 连接结束
    // 上行Session事件
    public static final int EVENT_START_SESSION = 100;
    public static final int EVENT_FINISH_SESSION = 102;
    // 下行Session事件
    public static final int EVENT_SESSION_STARTED = 150;
    public static final int EVENT_SESSION_FINISHED = 152;
    public static final int EVENT_SESSION_FAILED = 153;
    // 上行通用事件
    public static final int EVENT_TASK_REQUEST = 200;
    // 下行TTS事件
    public static final int EVENT_TTS_SENTENCE_START = 350;
    public static final int EVENT_TTS_SENTENCE_END = 351;
    public static final int EVENT_TTS_RESPONSE = 352;

}

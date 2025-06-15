package com.ruoyi.xiaozhi.chat.providers.tts.volcengine;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.extra.spring.SpringUtil;
import com.ruoyi.xiaozhi.chat.connect.ChatServerHandler;
import com.ruoyi.xiaozhi.chat.providers.tts.AudioEvent;
import com.ruoyi.xiaozhi.chat.providers.tts.BaseTTSProvider;
import com.ruoyi.xiaozhi.chat.providers.tts.TTSMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 火山引擎双向流式TTS
 * @author ruoyi-xiaozhi
 */
@Slf4j
public class VolcTTSProvider extends BaseTTSProvider {

    /** 音色 */
    public static final String SPEAKER = "zh_female_wanwanxiaohe_moon_bigtts";

    /** TTS客户端 */
    private VolcTTSClient client = null;

    /** TTS客户端连接池 */
    private final GenericObjectPool<VolcTTSClient> clientPool = SpringUtil.getBean(new TypeReference<>() {});

    public VolcTTSProvider(ChatServerHandler handler) {
        super(handler);
    }

    @Override
    protected void execute(String text, int sampleRate, int channels, int frameSizeByte, Function<byte[], Boolean> callback) {
        throw new UnsupportedOperationException("不支持单向流TTS操作");
    }

    /**
     * tts任务处理
     */
    @Override
    protected void ttsHandle() throws Exception {
        try {
            this.doHandle();
        }finally {
            // 防止执行报错，没有关闭连接
            this.finishConnection();
        }
    }

    private void doHandle() throws Exception {
        while (!handler.isClosed()) {
            TTSMessage message = TTSMessageQueue.poll(QUEUE_WAIT_TIMEOUT, TimeUnit.MICROSECONDS);

            if (message == null) continue;

            if (handler.isClientAbort()) {
                // 中断时，需要关闭连接
                this.finishConnection();
                continue;
            }

            log.info("TTS消息, message:{}", message);

            switch (message.getCategory()) {
                case SEGMENT_START -> {
                    log.info("Start borrow TTS connection...");
                    // 建立连接
                    client = clientPool.borrowObject();
                    log.info("Borrow TTS connection completed");
                    // 开启会话（阻塞式）
                    client.startSession(SPEAKER, 16000, 1920, this, ttsClient -> {
                        clientPool.returnObject(ttsClient);
                        log.info("TTS connection returned");
                    });
                    // 发送播放开始消息
                    this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.PLAYBACK_STARTED));
                }
                case TEXT_CONTENT -> {
                    if (client != null) {
                        // 发送文本
                        String text = message.getContent();
                        client.sendChunk(text);
                    }
                }
                case SEGMENT_END -> {
                    if (client != null) {
                        client.finishSession();
                        log.info("tts session finish");
                    }
                }
            }
        }
    }

    private void finishConnection() {
        if (client != null) {
            log.info("tts Connection finish");
            client.finishConnection();
            client = null;
        }
    }

}

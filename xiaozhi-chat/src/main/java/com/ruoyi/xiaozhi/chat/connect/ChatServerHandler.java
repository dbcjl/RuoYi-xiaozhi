package com.ruoyi.xiaozhi.chat.connect;

import ai.onnxruntime.OrtSession;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.k2fsa.sherpa.onnx.SileroVadModelConfig;
import com.k2fsa.sherpa.onnx.SpeechSegment;
import com.k2fsa.sherpa.onnx.VadModelConfig;
import com.ruoyi.xiaozhi.chat.core.RingByteBuffer;
import com.ruoyi.xiaozhi.chat.core.RawAudioPayload;
import com.ruoyi.xiaozhi.chat.core.vad.VoiceActivityDetector;
import com.ruoyi.xiaozhi.chat.opus.AudioOpusDecoder;
import com.ruoyi.xiaozhi.chat.providers.asr.ASREngine;
import com.ruoyi.xiaozhi.chat.providers.tts.BaseTTSProvider;
import com.ruoyi.xiaozhi.chat.providers.tts.TTSMessage;
import com.ruoyi.xiaozhi.chat.utils.AudioUtils;
import com.ruoyi.xiaozhi.chat.utils.TextProcessingUtils;
import com.ruoyi.xiaozhi.feign.constant.ChatConstants;
import com.ruoyi.xiaozhi.feign.enums.TTSProviderEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * 聊天服务处理器
 */
@Slf4j
public class ChatServerHandler implements Closeable {

    /** 空字节缓冲区 */
    public static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);

    /** 会话ID */
    @Getter
    private final String sessionId;

    /** websocket连接 */
    public final WebSocket conn;

    /** 客户端监听模式（"auto", "manual" 或 "realtime"） */
    private String clientListenMode = "auto";

    /** 客户端是否开始监听 */
    private boolean clientListenStart = false;

    /** 客户端声音是否已经停止 */
    private boolean clientVoiceStop = false;

    /** 客户端中断标识 */
    @Getter
    private volatile boolean clientAbort = false;

    /** 是否结束聊天-在本次聊天之后 */
    public volatile boolean closeAfterChat = false;

    /** 是否已经关闭 */
    @Getter
    private volatile boolean closed = false;

    /** 是否接收音频数据 */
    private boolean asrServerReceive = true;

    /** 设备端音频数据缓冲 */
    private final RingByteBuffer clientAudioBuffer = new RingByteBuffer(4096);

    /** asr音频数据缓冲 */
    private final Queue<RawAudioPayload> asrRawAudioPayloads = new ArrayDeque<>();

    /** 语音识别 */
    private final ASREngine asrEngine;

    /** 语音合成 */
    private final BaseTTSProvider ttsProvider;

    /** 聊天客户端 */
    private final ChatClient chatClient;

    /** 聊天记忆 */
    private final ChatMemory chatMemory;

    /** opus解码器 */
    private final AudioOpusDecoder audioOpusDecoder;

    /** 声音活动检测器 */
    private final VoiceActivityDetector vad;

    /** 聊天工具 */
    private final List<ToolCallback> chatTools;

    /** 工具上下文 */
    private final Map<String, Object> toolContext = new HashMap<>();

    /**
     * 构造函数
     * @param builder 构造器
     */
    private ChatServerHandler(Builder builder) {
        this.sessionId = IdUtil.fastUUID();
        this.conn = builder.conn;
        // 语音转文本
        this.asrEngine = builder.asrEngine;
        // 创建TTS实例
        this.ttsProvider = BaseTTSProvider.getInstance(this, builder.ttsProvider);
        this.chatClient = builder.chatClient;
        this.chatMemory = builder.chatMemory;
        this.chatTools = builder.chatTools;
        // 初始化工具上下文
        this.toolContext.put("conn", this);
        // 系统提示词
        this.chatMemory.add(this.sessionId, new SystemMessage(builder.prompt));
        // 初始化opus编解码器
        this.audioOpusDecoder = new AudioOpusDecoder(16000, 1);
        // 加载VAD
        this.vad = buildVad(builder.vadModelSession);
    }

    /**
     * 创建声音活动检测器
     * @param vadModelSession 模型会话（共享）
     */
    private VoiceActivityDetector buildVad(OrtSession vadModelSession) {
        SileroVadModelConfig sileroVad = SileroVadModelConfig.builder()
                .setThreshold(0.5f)
                .setMinSilenceDuration(0.25f)
                .setMinSpeechDuration(0.3f)
                .setWindowSize(512)
                .build();

        VadModelConfig config = VadModelConfig.builder()
                .setSileroVadModelConfig(sileroVad)
                .setSampleRate(16000)
                .build();

        return new VoiceActivityDetector(config, vadModelSession);
    }

    /**
     * 处理文本消息
     * @param message   消息内容
     */
    public void handleMessage(String message) {
        // 转换成json对象
        JSONObject messageJson = JSONObject.parseObject(message);
        String type = messageJson.getString("type");
        switch (type) {
            case ChatConstants.MESSAGE_TYPE_HELLO -> this.sendHelloMessage();
            case ChatConstants.MESSAGE_TYPE_ABORT -> this.handleAbortMessage(messageJson);
            case ChatConstants.MESSAGE_TYPE_LISTEN -> this.handleListenMessage(messageJson);
            case null, default -> log.error("未知消息类型, message:{}", message);
        }
    }

    /**
     * 处理中断消息
     * @param messageJson 消息
     */
    private void handleAbortMessage(JSONObject messageJson) {
        log.info("Abort message received, reason: {}", messageJson.getString("reason"));
        // 设置成打断状态，会自动打断llm、tts任务
        this.clientAbort = true;
        // 清空队列
        this.ttsProvider.clear();
        // 打断客户端说话状态
        this.sendTTSMessage("stop");
        log.info("Abort message received-end");
    }

    /**
     * 处理音频消息
     * @param audioBuffer   音频缓冲区
     */
    public void handleMessage(ByteBuffer audioBuffer) {
        if (!clientListenStart) {
            log.info("客户端未开始监听消息，拒绝接收音频数据");
            return;
        }
        if (!asrServerReceive) {
            log.debug("前期数据处理中，暂停接收音频数据");
            return;
        }
        RawAudioPayload rawAudioPayload = new RawAudioPayload(audioBuffer);
        float[] audioSamples;
        if ("manual".equals(clientListenMode)) {
            // 手动模式
            asrRawAudioPayloads.add(rawAudioPayload);
            if (!this.clientVoiceStop) {
                return;
            }
            audioSamples = this.decode(asrRawAudioPayloads);
        }else {
            // 自动模式
            SpeechSegment speechSegment = this.vadCheck(rawAudioPayload);
            if (speechSegment == null) {
                return;
            }
            audioSamples = speechSegment.getSamples();
        }
        this.asrServerReceive = false;
        log.info("asr start");
        String text = asrEngine.recognize(audioSamples, 16000);
        log.info("asr end, result: {}", text);
        String result = TextProcessingUtils.removeAllPunctuationMarks(text);
        if (CharSequenceUtil.isNotEmpty(result)) {
            startToChat(text);
        }else {
            this.asrServerReceive = true;
        }
        this.resetVadStates();
    }

    /**
     * 开始聊天
     * @param text  用户的信息
     */
    private void startToChat(String text) {
        // 发送STT消息
        this.sendSTTMessage(text);
        // 提交异步聊天任务
        Thread.ofVirtual().name("chat-" + CharSequenceUtil.subBefore(sessionId, "-", false)).start(() -> chat(text));
    }

    /**
     * 聊天
     * @param text  用户消息
     */
    private void chat(String text) {
        // 重置聊天打断状态
        this.clientAbort = false;
        Stream<String> llmStream = null;
        // 大模型是否有回复
        AtomicBoolean isResponse = new AtomicBoolean(false);
        try {
            llmStream = chatClient.prompt(new Prompt(text))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .toolContext(toolContext)
                    .toolCallbacks(chatTools)
                    .stream()
                    .content()
                    .doOnCancel(() -> log.info("chat cancel"))
                    .doOnTerminate(() -> log.info("chat terminate"))
                    .takeWhile(item -> !this.clientAbort)
                    .toStream();
            // 流式处理
            llmStream.forEach(chunk -> {
                if (CharSequenceUtil.isEmpty(chunk) || "\"done\"".equalsIgnoreCase(chunk)) return;
                if (!isResponse.get()) {
                    // 第一句，发送开始消息
                    isResponse.set(true);
                    this.ttsProvider.submitTTSMessage(new TTSMessage(TTSMessage.Category.SEGMENT_START));
                    log.info("llm first response: {}", chunk);
                }
                // 发送文本消息
                this.ttsProvider.submitTTSMessage(new TTSMessage(TTSMessage.Category.TEXT_CONTENT, chunk));
            });
            // 大模型有回复的情况下就发送结束消息
            if (isResponse.get()) {
                this.ttsProvider.submitTTSMessage(new TTSMessage(TTSMessage.Category.SEGMENT_END));
            }
        }finally {
            NioUtil.close(llmStream);
            this.clearChatMemory();
        }
    }

    /**
     * 重置VAD状态
     */
    private void resetVadStates() {
        log.info("VAD states reset.");
        this.asrRawAudioPayloads.clear();
        this.clientAudioBuffer.clear();
        this.clientVoiceStop = false;
        this.vad.reset();
    }

    /**
     * 声音活动检测
     * @param rawAudioPayload    当前音频帧
     * @return  检测结果
     */
    public SpeechSegment vadCheck(RawAudioPayload rawAudioPayload) {
        // 数据解码加入缓冲区
        byte[] pcmFrame = audioOpusDecoder.decodeFrame(rawAudioPayload.getPcmBuffer(), 960);
        this.clientAudioBuffer.writeBytes(pcmFrame);
        // 处理缓冲区中的完整帧（每次处理1024个字节）
        byte[] buffer = new byte[1024];
        while (this.clientAudioBuffer.getCurrentSize() >= 1024) {
            // 读取前 1024 字节
            this.clientAudioBuffer.readBytes(buffer);
            // 流式传入语音数据
            this.vad.acceptWaveform(AudioUtils.byteToFloat(buffer));
            // 判断是否检测到语音片段
            if (!this.vad.isEmpty()) {
                SpeechSegment segment = this.vad.poll();
                float startTime = segment.getStart() / (float) 16000;
                float duration = segment.getSamples().length / (float) 16000;
                log.info("语音片段: startTime: {}, duration: {}", startTime, duration);
                return segment;
            }
        }
        return null;
    }

    /**
     * 解码opus数据
     * @param opusRawAudioPayloads   opus数据帧列表
     * @return  PCM数据
     */
    private float[] decode(Queue<RawAudioPayload> opusRawAudioPayloads) {
        // 解码拼接所有音频数据
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (RawAudioPayload rawAudioPayload : opusRawAudioPayloads) {
            byte[] pcmFrame = audioOpusDecoder.decodeFrame(rawAudioPayload.getPcmBuffer(), 960);
            IoUtil.write(bos, false, pcmFrame);
        }
        return AudioUtils.byteToFloat(bos.toByteArray());
    }

    /**
     * 处理监听消息
     * @param messageJson   消息内容
     */
    private void handleListenMessage(JSONObject messageJson) {
        String mode = messageJson.getString("mode");
        if (CharSequenceUtil.isNotBlank(mode)) {
            this.clientListenMode = mode;
        }
        String state = messageJson.getString("state");
        if ("start".equals(state)) {
            this.clientListenStart = true;
            this.clientVoiceStop = false;
        }else if ("stop".equals(state)){
            this.clientVoiceStop = true;
            if (!this.asrRawAudioPayloads.isEmpty()) {
                this.handleMessage(EMPTY_BYTE_BUFFER);
            }
        }else if ("detect".equals(state)) {
            this.asrServerReceive = false;
            this.asrRawAudioPayloads.clear();
            // 获取唤醒词
            String text = messageJson.getString("text");
            String result = TextProcessingUtils.removeAllPunctuationMarks(text);
            log.info("客户端识别到唤醒词, text: {}", text);
            this.sendSTTMessage(result);
            this.sendTTSMessage("stop");
        }
    }

    /**
     * 发送STT状态消息
     * @param text  消息文本
     */
    private void sendSTTMessage(String text) {
        String STTText = TextProcessingUtils.removeEmojisAndTrimSeparators(text);
        String STTMessage = new JSONObject().fluentPut("type", "stt").fluentPut("text", STTText)
                .fluentPut(ChatConstants.SESSION_ID, this.sessionId).toJSONString();
        this.conn.send(STTMessage);
        // 发送TTS开始
        this.sendTTSMessage("start");
    }

    /**
     * 发送 TTS 状态消息
     * @param state 状态
     */
    public void sendTTSMessage(String state) {
        this.sendTTSMessage(state, null);
    }

    /**
     * 发送 TTS 状态消息
     * @param state 状态
     * @param text  消息文本
     */
    public void sendTTSMessage(String state, String text) {
        JSONObject TTSMessage = new JSONObject().fluentPut("type", "tts").fluentPut("state", state)
                .fluentPut(ChatConstants.SESSION_ID, sessionId);
        if (CharSequenceUtil.isNotBlank(text)) {
            TTSMessage.put("text", text);
        }
        // TTS播放结束
        if (state.equals("stop")) {
            // 清除服务端讲话状态
            this.clearSpeakStatus();
        }
        // 发送消息到客户端
        this.conn.send(TTSMessage.toJSONString());
    }

    /** 清除服务端讲话状态 */
    private void clearSpeakStatus() {
        log.info("清除服务端讲话状态");
        this.asrServerReceive = true;
    }

    /**
     * 发送hello消息
     */
    private void sendHelloMessage() {
        JSONObject helloMessage = new JSONObject();
        helloMessage.put("type", "hello");
        helloMessage.put("transport", "websocket");
        helloMessage.put("version", "1");
        helloMessage.put(ChatConstants.SESSION_ID, sessionId);
        conn.send(helloMessage.toJSONString());
    }

    /**
     * 发送emoji表情
     */
    public void sendEmotion(String emotion, String text) {
        String LLMMessage = new JSONObject().fluentPut("type", "llm").fluentPut("text", text)
                .fluentPut("emotion", emotion).fluentPut(ChatConstants.SESSION_ID, sessionId).toJSONString();
        conn.send(LLMMessage);
    }

    /**
     * 释放资源
     */
    @Override
    public void close() {
        this.closed = true;
        this.clearChatMemory();
        IoUtil.close(audioOpusDecoder);
        IoUtil.close(ttsProvider);
    }

    /**
     * 清除聊天记忆
     */
    private void clearChatMemory() {
        if (!this.closed) return;
        this.chatMemory.clear(this.sessionId);
    }

    /**
     * 创建构建器
     * @param conn  websocket连接
     * @return  聊天服务处理器构建器
     */
    public static Builder builder(WebSocket conn) {
        return new Builder(conn);
    }

    public static class Builder {

        private final WebSocket conn;

        private OrtSession vadModelSession;

        private ASREngine asrEngine;

        private ChatClient chatClient;

        private ChatMemory chatMemory;

        private List<ToolCallback> chatTools;

        private String prompt;

        private TTSProviderEnum ttsProvider;

        public Builder(WebSocket conn) {
            this.conn = conn;
        }

        public Builder vadModelSession(OrtSession vadModelSession) {
            this.vadModelSession = vadModelSession;
            return this;
        }

        public Builder asrProvider(ASREngine asrEngine) {
            this.asrEngine = asrEngine;
            return this;
        }

        public Builder chatClient(ChatClient chatClient) {
            this.chatClient = chatClient;
            return this;
        }

        public Builder chatMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }

        public Builder chatTools(List<ToolCallback> chatTools) {
            this.chatTools = chatTools;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder ttsProvider(TTSProviderEnum ttsProvider) {
            this.ttsProvider = ttsProvider;
            return this;
        }

        public ChatServerHandler build() {
            return new ChatServerHandler(this);
        }

    }

}

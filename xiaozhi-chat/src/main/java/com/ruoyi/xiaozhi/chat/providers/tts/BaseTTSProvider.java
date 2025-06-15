package com.ruoyi.xiaozhi.chat.providers.tts;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.xiaozhi.chat.connect.ChatServerHandler;
import com.ruoyi.xiaozhi.chat.core.EmotionAnalyzer;
import com.ruoyi.xiaozhi.chat.opus.AudioOpusEncoder;
import com.ruoyi.xiaozhi.chat.providers.tts.edgetts.EdgeTTSProvider;
import com.ruoyi.xiaozhi.chat.providers.tts.volcengine.VolcTTSProvider;
import com.ruoyi.xiaozhi.chat.utils.AudioUtils;
import com.ruoyi.xiaozhi.chat.utils.TextProcessingUtils;
import com.ruoyi.xiaozhi.feign.enums.TTSProviderEnum;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import java.io.Closeable;
import java.io.File;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TTS抽象基类
 * @author ruoyi-xiaozhi
 */
@Slf4j
public abstract class BaseTTSProvider implements Closeable {

    public static final int QUEUE_WAIT_TIMEOUT = 500;

    /** TTS消息队列 */
    protected BlockingQueue<TTSMessage> TTSMessageQueue = new LinkedBlockingQueue<>(20);

    /** 音频消息队列 */
    protected BlockingQueue<AudioEvent> audioEventQueue = new LinkedBlockingQueue<>(128);

    /** 聊天服务处理器 */
    protected final ChatServerHandler handler;

    /** opus编码器 */
    protected final AudioOpusEncoder audioOpusEncoder;

    public BaseTTSProvider(ChatServerHandler handler) {
        this.handler = handler;
        this.audioOpusEncoder = new AudioOpusEncoder(16000, 1);
        this.initTTSConsumer();
        this.initAudioConsumer();
    }

    public boolean isClientAbort() {
        return this.handler.isClientAbort();
    }

    /**
     * 创建TTS消费者
     */
    private void initTTSConsumer() {
        Thread.ofVirtual().name("tts-" + StrUtil.subBefore(handler.getSessionId(), "-", false)).start(() -> {
            try {
                // TTS处理
                this.ttsHandle();
            } catch (Exception e) {
                log.error("tts consumer error", e);
            }
            log.info("tts consumer thread exit, {}", handler.getSessionId());
        });
    }

    /**
     * 创建音频消费者线程
     */
    private void initAudioConsumer() {
        Thread.ofVirtual().name("audio-" + StrUtil.subBefore(handler.getSessionId(), "-", false)).start(() -> {
            try {
                this.handleAudioPlayback();
            }catch (Exception e) {
                log.error("audio consumer error", e);
            }
            log.info("audio consumer thread exit, {}", handler.getSessionId());
        });
    }

    /**
     * 音频播放处理
     */
    private void handleAudioPlayback() throws InterruptedException {
        final int FRAME_DURATION_MS = 60;
        final int INITIAL_BUFFER_COUNT = 3;

        int frameIndex = -1;
        long lastSentTime = -1;

        while (!handler.isClosed()) {
            AudioEvent message = audioEventQueue.poll(QUEUE_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
            if (message == null || handler.isClientAbort()) continue;

            switch (message.getEventType()) {
                case PLAYBACK_STARTED -> frameIndex = 0;

                case SENTENCE_START -> {
                    String text = message.getText();
                    // 句子的情感分析
                    String emotion = EmotionAnalyzer.analyzeEmotion(text);
                    String emoji = EmotionAnalyzer.mappingEmoji(emotion);
                    log.info("sentence_start, emotion: {}, emoji: {}", emotion, emoji);
                    this.handler.sendEmotion(emotion, emoji);
                    // 发送句子开始消息
                    handler.sendTTSMessage("sentence_start", text);
                }

                case AUDIO_FRAME -> {
                    if (frameIndex++ >= INITIAL_BUFFER_COUNT) {
                        long expectedTime = lastSentTime + TimeUnit.MILLISECONDS.toNanos(FRAME_DURATION_MS);
                        long delay = expectedTime - System.nanoTime();
                        if (delay > 0) {
                            LockSupport.parkNanos(delay);
                        }
                    }

                    if (frameIndex == 1) {
                        log.info("First audio frame dispatched");
                    }

                    handler.conn.send(message.getData());
                    lastSentTime = System.nanoTime();
                }

                case SENTENCE_END -> handler.sendTTSMessage("sentence_end", message.getText());

                case PLAYBACK_STOPPED -> {
                    handler.sendTTSMessage("stop");
                    if (handler.closeAfterChat) {
                        handler.conn.close();
                    }
                }
            }
        }
    }

    /**
     * tts任务处理
     */
    protected void ttsHandle() throws Exception {
        // 存储LLM回复的全部文本
        StringBuilder llmResponse = new StringBuilder();
        // 当前处理位置
        int currentIndex = -1;
        // 是否是第一句
        boolean isFirst = true;
        while (!handler.isClosed()) {

            TTSMessage message = TTSMessageQueue.poll(QUEUE_WAIT_TIMEOUT, TimeUnit.MICROSECONDS);
            if (message == null) continue;

            if (handler.isClientAbort()) continue;

            if (TTSMessage.Category.SEGMENT_START.equals(message.getCategory())) {
                llmResponse.setLength(0);
                currentIndex = 0;
                isFirst = true;
            }else if (TTSMessage.Category.TEXT_CONTENT.equals(message.getCategory())) {
                llmResponse.append(message.getContent());
                String currentText = llmResponse.substring(currentIndex);
                // 查找最后一个有效标点
                int lastPunctuationIndex = TextProcessingUtils.findLastChinesePunctuationIndex(currentText);
                if (lastPunctuationIndex == -1) continue;
                // 切割句子
                String segmentTextRaw = currentText.substring(0, lastPunctuationIndex + 1);
                String segmentText = TextProcessingUtils.removeEmojisAndTrimSeparators(segmentTextRaw);
                if (StrUtil.isEmpty(segmentText)) continue;
                // 第一句要发送播放开始消息
                if (isFirst) {
                    isFirst = false;
                    this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.PLAYBACK_STARTED));
                }
                // 处理句子
                this.sendSentence(segmentText);
                // 更新已处理的字符位置
                currentIndex += segmentTextRaw.length();
            }else if (TTSMessage.Category.SEGMENT_END.equals(message.getCategory())) {
                // 处理最后剩余的文本
                String remainingText = llmResponse.substring(currentIndex);
                if (StrUtil.isNotEmpty(remainingText)) {
                    String segmentText = TextProcessingUtils.removeEmojisAndTrimSeparators(remainingText);
                    if (StrUtil.isNotEmpty(segmentText)) {
                        // 处理最后的句子
                        this.sendSentence(segmentText);
                    }
                }
                // 发送播放结束消息
                this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.PLAYBACK_STOPPED));
            }
        }
    }

    /**
     * 发送句子开始消息
     * @param text  句子文本
     */
    public void sentenceStartMessage(String text) {
        log.info("句子开始消息: {}", text);
        this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.SENTENCE_START, text));
    }

    /**
     * 发送句子结束消息
     * @param text  句子文本
     */
    public void sentenceEndMessage(String text) {
        log.info("句子结束消息: {}", text);
        this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.SENTENCE_END, text));
    }

    /**
     * 停止播放事件
     */
    public void stopMessage() {
        this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.PLAYBACK_STOPPED));
    }

    /**
     * 处理音频帧
     * @param buffer    PCM音频数据
     */
    public void handleFrame(byte[] buffer) {
        byte[] opusData = this.audioOpusEncoder.encode(buffer, buffer.length / 2);
        this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.AUDIO_FRAME, opusData));
    }

    /**
     * 发送音频消息
     * @param message   音频播放消息
     */
    @SneakyThrows
    protected void sendAudioMessage(AudioEvent message) {
        this.audioEventQueue.put(message);
    }

    /**
     * 提交TTS消息
     * @param message   TTS消息
     */
    @SneakyThrows
    public void submitTTSMessage(TTSMessage message) {
        this.TTSMessageQueue.put(message);
    }

    /**
     * 发送句子
     * @param segmentText   句子文本
     */
    private void sendSentence(String segmentText) {
        // 发送句子开始消息
        this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.SENTENCE_START, segmentText));
        // 执行TTS
        int sampleRate = this.audioOpusEncoder.getSamplingRate();
        int channels = this.audioOpusEncoder.getAudioChannels();
        int frameSizeByte = 1920;
        this.execute(segmentText, sampleRate, channels, frameSizeByte, buffer -> {
            // 处理音频帧
            this.handleFrame(buffer);
            return !this.handler.isClientAbort();
        });
        // 发送句子结束消息
        this.sendAudioMessage(new AudioEvent(AudioEvent.EventType.SENTENCE_END));
    }

    /**
     * 生成临时音频文件
     */
    protected final File generateAudioFile(String extension) {
        return FileUtil.createTempFile("tts-audio-", extension, new File("temp-tts-audio"),  true);
    }

    /**
     * tts语音合成
     * @param text              要进行语音合成的文本
     * @param sampleRate        音频的采样率
     * @param channels          音频的通道数
     * @param frameSizeByte     回调期望的帧长（字节单位）
     * @param callback          音频合成回调（支持流式)，返回false表示中断/结束合成
     */
    protected abstract void execute(String text, int sampleRate, int channels, int frameSizeByte, Function<byte[], Boolean> callback);

    /**
     * 解码音频文件
     * @param audioFilePath     音频文件路径
     * @param sampleRate        目标采样率
     * @param channels          目标通道数
     * @param frameSizeByte     帧长（累计多少数据进行回调）
     * @param callback          音频合成回调（支持流式)，返回false表示中断/结束合成
     */
    protected void decodeAudio(String audioFilePath, int sampleRate, int channels, int frameSizeByte, Function<byte[], Boolean> callback) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(audioFilePath)){
            grabber.setAudioChannels(channels);
            grabber.setSampleRate(sampleRate);
            grabber.setSampleFormat(avutil.AV_SAMPLE_FMT_S16P);
            grabber.start();
            // 创建音频缓冲区
            byte[] frameBuffer = new byte[frameSizeByte];
            // 循环处理一帧，frameBuffer填充满了，会触发回调
            int remaining = this.handleFrame(grabber, frameBuffer, () -> callback.apply(frameBuffer));
            // 对最后一帧进行补0操作
            if (remaining > 0) {
                Arrays.fill(frameBuffer, remaining, frameBuffer.length, (byte) 0);
                callback.apply(frameBuffer);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("ffmpeg解码音频出错", e);
        }
    }

    /**
     * 处理音频帧
     *
     * @param grabber       ffmpeg音频抓取器
     * @param frameBuffer   帧数据缓冲区
     * @param nextFrame     回调方法
     * @return 返回最后一帧未填满的剩余字节数
     */
    protected int handleFrame(FFmpegFrameGrabber grabber, byte[] frameBuffer, Supplier<Boolean> nextFrame) throws FFmpegFrameGrabber.Exception {
        // 音频帧缓冲区偏移量
        int offset = 0;
        while (true) {
            try (Frame frame = grabber.grabSamples()) {
                // 没有读取到帧，证明结束了
                if (frame == null) break;
                // 空帧，跳过
                if (frame.samples == null || frame.samples.length == 0) continue;
                // 判断音频数据的格式
                Buffer sample = frame.samples[0];
                if (!(sample instanceof ShortBuffer audioBuffer)) {
                    throw new IllegalStateException("Unsupported audio format: not a ShortBuffer");
                }

                while (audioBuffer.remaining() * 2 >= frameBuffer.length - offset) {
                    int length = frameBuffer.length - offset;
                    AudioUtils.copy(audioBuffer, frameBuffer, offset, length);
                    if (!Boolean.TRUE.equals(nextFrame.get())) return -1;
                    offset = 0;
                }

                int remaining = audioBuffer.remaining() * 2;
                if (remaining > 0) {
                    AudioUtils.copy(audioBuffer, frameBuffer, offset, remaining);
                    offset += remaining;
                }
            }
        }
        return offset;
    }

    /** 清空队列 */
    public void clear() {
        this.TTSMessageQueue.clear();
        this.audioEventQueue.clear();
    }

    /**
     * 释放资源
     */
    @Override
    public void close() {
        this.clear();
        IoUtil.close(audioOpusEncoder);
    }

    /**
     * 获取TTS实例
     * @param handler       处理器实例
     * @param ttsProvider   TTS模型服务商
     * @return  TTS实例
     */
    public static BaseTTSProvider getInstance(ChatServerHandler handler, TTSProviderEnum ttsProvider) {
        return switch (ttsProvider) {
            case EDGE_TTS -> new EdgeTTSProvider(handler);
            case VOLC_ENGINE_TTS -> new VolcTTSProvider(handler);
        };
    }
}

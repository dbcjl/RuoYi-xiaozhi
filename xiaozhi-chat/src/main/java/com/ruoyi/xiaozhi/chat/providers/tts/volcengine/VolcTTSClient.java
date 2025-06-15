package com.ruoyi.xiaozhi.chat.providers.tts.volcengine;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.xiaozhi.chat.providers.tts.BaseTTSProvider;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * 火山引擎TTS客户端
 * @author ruoyi-xiaozhi
 */
@Slf4j
public class VolcTTSClient extends WebSocketClient {

    private boolean connecting = false;

    private String sessionId;

    private byte[] buffer;

    private int bufferOffset = 0;

    private CompletableFuture<String> connFuture;

    private BaseTTSProvider provider;

    private Consumer<VolcTTSClient> finishConn;

    private boolean isStop = false;

    private String speaker;

    private int sampleRate;

    private VolcTTSClient(Builder builder) {
        super(URI.create(VolcTTSConstants.SERVER_URI), builder.headers());
    }

    /**
     * 开始TTS会话（阻塞等待）
     * @param speaker       合成语音的音色
     * @param sampleRate    合成音频的采样率
     * @param frameSizeByte 音频帧大小
     * @param provider      TTS提供者
     * @param finishConn    连接完成的回调
     */
    public synchronized void startSession(String speaker, int sampleRate, int frameSizeByte, BaseTTSProvider provider, Consumer<VolcTTSClient> finishConn) {
        if (connecting) {
            throw new IllegalStateException("正在连接中");
        }
        // 重新初始化参数
        this.sessionId = IdUtil.fastSimpleUUID();
        this.buffer = new byte[frameSizeByte];
        this.bufferOffset = 0;
        this.connFuture = new CompletableFuture<>();
        this.provider = provider;
        this.speaker = speaker;
        this.sampleRate = sampleRate;
        this.finishConn = finishConn;
        this.isStop = false;
        // 开始连接
        startConnection();
        // 阻塞等待
        String message = await();
        if (CharSequenceUtil.isNotEmpty(message)) {
            if (this.finishConn != null) finishConn.accept(this);
            this.finishConn = null;
            throw new IllegalStateException("startSession fail, message: " + message);
        }
        this.connecting = true;
    }

    /**
     * 发送文本
     * @param text  文本
     */
    public void sendChunk(String text) {
        if (!connecting) {
            throw new IllegalStateException("未开始TTS会话");
        }
        byte[] header = new Header(
                VolcTTSConstants.PROTOCOL_VERSION,
                VolcTTSConstants.FULL_CLIENT_REQUEST,
                VolcTTSConstants.DEFAULT_HEADER_SIZE,
                VolcTTSConstants.MSG_TYPE_FLAG_WITH_EVENT,
                VolcTTSConstants.JSON,
                VolcTTSConstants.COMPRESSION_NO,
                0).getBytes();

        byte[] optional = new Optional(VolcTTSConstants.EVENT_TASK_REQUEST, sessionId).getBytes();

        byte[] payload = getBytes(text);
        sendEvent(header, optional, payload);
    }

    private byte[] getBytes(String text) {
        JSONObject payloadJObj = new JSONObject();
        JSONObject user = new JSONObject();
        user.put("uid", "123456");
        payloadJObj.put("user", user);

        payloadJObj.put("event", VolcTTSConstants.EVENT_TASK_REQUEST);
        payloadJObj.put("namespace", "BidirectionalTTS");

        JSONObject reqParams = new JSONObject();
        reqParams.put("text", text);
        reqParams.put("speaker", speaker);

        JSONObject audioParams = new JSONObject();
        audioParams.put("format", "pcm");
        audioParams.put("sample_rate", sampleRate);

        reqParams.put("audio_params", audioParams);
        payloadJObj.put("req_params", reqParams);
        return payloadJObj.toString().getBytes();
    }

    /**
     * 结束TTS会话（流式文本输入完毕调用）
     */
    public void finishSession() {
        if (!connecting) {
            return;
        }
        byte[] header = new Header(
                VolcTTSConstants.PROTOCOL_VERSION,
                VolcTTSConstants.FULL_CLIENT_REQUEST,
                VolcTTSConstants.DEFAULT_HEADER_SIZE,
                VolcTTSConstants.MSG_TYPE_FLAG_WITH_EVENT,
                VolcTTSConstants.JSON,
                VolcTTSConstants.COMPRESSION_NO,
                0).getBytes();
        byte[] optional = new Optional(VolcTTSConstants.EVENT_FINISH_SESSION, sessionId).getBytes();
        byte[] payload = "{}".getBytes();
        sendEvent(header, optional, payload);
    }

    /** 等待连接结果 */
    private String await() {
        try {
            return this.connFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /** 重置 */
    private void reset() {
        this.sessionId = null;
        this.buffer = null;
        this.bufferOffset = 0;
        this.provider = null;
        this.speaker = null;
        this.connecting = false;
        this.isStop = true;
    }

    /** websocket连接成功 */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("onOpen, logid: {}", handshakedata.getFieldValue("X-Tt-Logid"));
    }

    @Override
    public void onMessage(String message) {
        log.info("onMessage: {}", message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("onClose, code: {}, reason: {}, remote: {}", code, reason, remote);
        if (this.connFuture != null && !this.connFuture.isDone()) {
            this.connFuture.complete(CharSequenceUtil.format("连接关闭, code: {}, reason: {}, remote: {}", code, reason, remote));
        }
        this.sendStopMessage();
    }

    @Override
    public void onError(Exception ex) {
        log.error("onError: ", ex);
        if (this.connFuture != null && !this.connFuture.isDone()) {
            this.connFuture.complete(CharSequenceUtil.format("连接发生错误, error: {}", ex.getMessage()));
        }
        this.sendStopMessage();
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        TTSResponse response = parserResponse(bytes.array());
        switch (response.optional.event) {
            case VolcTTSConstants.EVENT_CONNECTION_FAILED, VolcTTSConstants.EVENT_SESSION_FAILED -> {
                log.error("volc tts error: {}", response.optional.event);
                this.connFuture.complete("event: " + response.optional.event);
            }
            case VolcTTSConstants.EVENT_CONNECTION_STARTED -> {
                log.info("volc tts connection started");
                startTTSSession();
            }
            case VolcTTSConstants.EVENT_SESSION_STARTED -> {
                log.info("volc tts session started");
                this.connFuture.complete(CharSequenceUtil.EMPTY);
            }
            case VolcTTSConstants.EVENT_TTS_SENTENCE_START -> {
                if (this.provider == null || this.provider.isClientAbort()) return;
                log.info("volc tts sentence start");
                JSONObject payloadJson = JSONObject.parse(response.payloadJson);
                String text = payloadJson.getString("text");
                this.provider.sentenceStartMessage(text);
            }
            case VolcTTSConstants.EVENT_TTS_SENTENCE_END -> {
                if (this.provider == null || this.provider.isClientAbort()) return;
                log.info("volc tts sentence end");
                // 句子结束
                if (bufferOffset > 0) {
                    // 不足补0
                    Arrays.fill(buffer, bufferOffset, buffer.length, (byte) 0);
                    this.provider.handleFrame(buffer);
                    bufferOffset = 0;
                }
                // 发送句子结束的消息
                JSONObject payloadJson = JSONObject.parse(response.payloadJson);
                String text = payloadJson.getString("text");
                this.provider.sentenceEndMessage(text);
            }
            case VolcTTSConstants.EVENT_TTS_RESPONSE -> {
                if (response.payload == null || this.provider == null || this.provider.isClientAbort()) {
                    return;
                }
                if (response.header.messageType == VolcTTSConstants.AUDIO_ONLY_RESPONSE) {
                    byte[] pcm = response.payload;
                    int offset = 0;
                    while (pcm.length - offset > 0) {
                        int len = Math.min(pcm.length - offset, buffer.length - bufferOffset);
                        System.arraycopy(pcm, offset, buffer, bufferOffset, len);
                        bufferOffset += len;
                        if (bufferOffset >= buffer.length) {
                            // 满足一帧
                            this.provider.handleFrame(buffer);
                            bufferOffset = 0;
                        }
                        offset += len;
                    }
                }
            }
            case VolcTTSConstants.EVENT_SESSION_FINISHED -> {
                log.info("volc tts session finished");
                this.sendStopMessage();
                finishConnection();
            }
            case VolcTTSConstants.EVENT_CONNECTION_FINISHED -> {
                log.info("volc tts connection finished");
                this.sendStopMessage();
                this.reset();
                // 连接完成，触发回调
                if (this.finishConn != null) finishConn.accept(this);
                this.finishConn = null;
            }
        }
    }

    private void sendStopMessage() {
        if (!this.isStop && this.provider != null) {
            this.provider.stopMessage();
            this.isStop = true;
        }
    }

    public void finishConnection() {
        byte[] header = new Header(
                VolcTTSConstants.PROTOCOL_VERSION,
                VolcTTSConstants.FULL_CLIENT_REQUEST,
                VolcTTSConstants.DEFAULT_HEADER_SIZE,
                VolcTTSConstants.MSG_TYPE_FLAG_WITH_EVENT,
                VolcTTSConstants.JSON,
                VolcTTSConstants.COMPRESSION_NO,
                0).getBytes();
        byte[] optional = new Optional(VolcTTSConstants.EVENT_FINISH_CONNECTION, null).getBytes();
        byte[] payload = "{}".getBytes();
        sendEvent(header, optional, payload);
    }

    private void startConnection() {
        byte[] header = new Header(
                VolcTTSConstants.PROTOCOL_VERSION,
                VolcTTSConstants.FULL_CLIENT_REQUEST,
                VolcTTSConstants.DEFAULT_HEADER_SIZE,
                VolcTTSConstants.MSG_TYPE_FLAG_WITH_EVENT,
                VolcTTSConstants.JSON,
                VolcTTSConstants.COMPRESSION_NO,
                0).getBytes();
        byte[] optional = new Optional(VolcTTSConstants.EVENT_START_CONNECTION, null).getBytes();
        byte[] payload = "{}".getBytes();
        sendEvent(header, optional, payload);
    }

    private void startTTSSession() {
        byte[] header = new Header(
                VolcTTSConstants.PROTOCOL_VERSION,
                VolcTTSConstants.FULL_CLIENT_REQUEST,
                VolcTTSConstants.DEFAULT_HEADER_SIZE,
                VolcTTSConstants.MSG_TYPE_FLAG_WITH_EVENT,
                VolcTTSConstants.JSON,
                VolcTTSConstants.COMPRESSION_NO,
                0).getBytes();

        byte[] optional = new Optional(VolcTTSConstants.EVENT_START_SESSION, sessionId).getBytes();
        byte[] payload = getBytes();
        sendEvent(header, optional, payload);
    }

    private byte[] getBytes() {
        JSONObject payloadJObj = new JSONObject();
        JSONObject user = new JSONObject();
        user.put("uid", "123456");

        payloadJObj.put("user", user);
        payloadJObj.put("event", VolcTTSConstants.EVENT_START_SESSION);
        payloadJObj.put("namespace", "BidirectionalTTS");

        JSONObject reqParams = new JSONObject();
        reqParams.put("speaker", speaker);

        JSONObject audioParams = new JSONObject();
        audioParams.put("format", "pcm");
        audioParams.put("sample_rate", sampleRate);
        audioParams.put("enable_timestamp", true);

        reqParams.put("audio_params", audioParams);
        payloadJObj.put("req_params", reqParams);
        return payloadJObj.toString().getBytes();
    }

    private void sendEvent(byte[] header, byte[] optional, byte[] payload) {
        final byte[] payloadSizeBytes = ByteUtil.intToBytes(payload.length, ByteOrder.BIG_ENDIAN);
        byte[] requestBytes = new byte[
                header.length
                        + (optional == null ? 0 : optional.length)
                        + payloadSizeBytes.length + payload.length];
        int desPos = 0;
        System.arraycopy(header, 0, requestBytes, desPos, header.length);
        desPos += header.length;
        if (optional != null) {
            System.arraycopy(optional, 0, requestBytes, desPos, optional.length);
            desPos += optional.length;
        }
        System.arraycopy(payloadSizeBytes, 0, requestBytes, desPos, payloadSizeBytes.length);
        desPos += payloadSizeBytes.length;
        System.arraycopy(payload, 0, requestBytes, desPos, payload.length);
        this.send(requestBytes);
    }

    /**
     * 解析响应包
     */
    private TTSResponse parserResponse(byte[] res) {
        if (res == null || res.length == 0) {
            return null;
        }
        final TTSResponse response = new TTSResponse();
        Header header = new Header();
        response.header = header;

        // 当符号位为1时进行 >> 运算后高位补1（预期是补0），导致结果错误，所以增加个数再与其& 运算，目的是确保高位是补0.
        final byte num = 0b00001111;
        // header 32 bit=4 byte
        header.protocolVersion = (res[0] >> 4) & num;
        header.headerSize = res[0] & 0x0f;
        header.messageType = (res[1] >> 4) & num;
        header.messageTypeSpecificFlags = res[1] & 0x0f;
        header.serializationMethod = res[2] >> num;
        header.messageCompression = res[2] & 0x0f;
        header.reserved = res[3];

        int offset = 4;
        response.optional = new Optional();
        // 正常Response
        if (header.messageType == VolcTTSConstants.FULL_SERVER_RESPONSE || header.messageType == VolcTTSConstants.AUDIO_ONLY_RESPONSE) {
            // 如果有event
            offset = readEvent(res, header.messageTypeSpecificFlags, response, offset);
            final int event = response.optional.event;
            // 根据 event 类型解析
            switch (event) {
                case VolcTTSConstants.EVENT_CONNECTION_STARTED:
                    readConnectStarted(res, response, offset);
                    break;
                case VolcTTSConstants.EVENT_CONNECTION_FAILED:
                    readConnectFailed(res, response, offset);
                    break;
                case VolcTTSConstants.EVENT_SESSION_STARTED:
                    readSessionId(res, response, offset);
                    break;
                case VolcTTSConstants.EVENT_TTS_RESPONSE:
                    offset = readSessionId(res, response, offset);
                    readPayload(res, response, offset);
                    break;
                case VolcTTSConstants.EVENT_TTS_SENTENCE_START, VolcTTSConstants.EVENT_TTS_SENTENCE_END:
                    offset = readSessionId(res, response, offset);
                    readPayloadJson(res, response, offset);
                    break;
                case VolcTTSConstants.EVENT_SESSION_FAILED, VolcTTSConstants.EVENT_SESSION_FINISHED:
                    offset = readSessionId(res, response, offset);
                    readMetaJson(res, response, offset);
                    break;
                default:
                    break;
            }
        }
        // 错误
        else if (header.messageType == VolcTTSConstants.ERROR_INFORMATION) {
            offset = readErrorCode(res, response, offset);
            readPayload(res, response, offset);
        }
        return response;
    }

    private int readPayloadJson(byte[] res, TTSResponse response, int start) {
        Pair<Integer, String> r = readText(res, start);
        start = r.getKey();
        response.payloadJson = r.getValue();
        return start;
    }

    private int readSessionId(byte[] res, TTSResponse response, int start) {
        Pair<Integer, String> r = readText(res, start);
        start = r.getKey();
        response.optional.sessionId = r.getValue();
        return start;
    }

    private int readErrorCode(byte[] res, TTSResponse response, int start) {
        byte[] b = new byte[4];
        System.arraycopy(res, start, b, 0, b.length);
        start += b.length;
        response.optional.errorCode = ByteUtil.bytesToInt(b, ByteOrder.BIG_ENDIAN);
        return start;
    }

    private int readPayload(byte[] res, TTSResponse response, int start) {
        byte[] b = new byte[4];
        System.arraycopy(res, start, b, 0, b.length);
        start += b.length;
        int size = ByteUtil.bytesToInt(b, ByteOrder.BIG_ENDIAN);
        response.payloadSize += size;
        b = new byte[size];
        System.arraycopy(res, start, b, 0, b.length);
        response.payload = b;
        start += b.length;
        if (response.optional.event == VolcTTSConstants.FULL_SERVER_RESPONSE) {
            log.info("===> payload:{}", new String(b));
        }
        return start;
    }

    private void readConnectFailed(byte[] res, TTSResponse response, int start) {
        // 8--11: connection id len
        start = readConnectId(res, response, start);

        readMetaJson(res, response, start);
    }

    private int readMetaJson(byte[] res, TTSResponse response, int start) {
        Pair<Integer, String> r = readText(res, start);
        start = r.getKey();
        response.optional.responseMetaJson = r.getValue();
        return start;
    }

    private int readEvent(byte[] res, int masTypeFlag, TTSResponse response, int start) {
        if (masTypeFlag == VolcTTSConstants.MSG_TYPE_FLAG_WITH_EVENT) {
            byte[] temp = new byte[4];
            System.arraycopy(res, start, temp, 0, temp.length);
            response.optional.event = ByteUtil.bytesToInt(temp, ByteOrder.BIG_ENDIAN);
            start += temp.length;
        }
        return start;
    }

    private void readConnectStarted(byte[] res, TTSResponse response, int start) {
        // 8--11: connection id size
        readConnectId(res, response, start);
    }

    private int readConnectId(byte[] res, TTSResponse response, int start) {
        Pair<Integer, String> r = readText(res, start);
        start = r.getKey();
        response.optional.connectionId = r.getValue();
        return start;
    }

    private Pair<Integer, String> readText(byte[] res, int start) {
        byte[] b = new byte[4];
        System.arraycopy(res, start, b, 0, b.length);
        start += b.length;
        int size = ByteUtil.bytesToInt(b, ByteOrder.BIG_ENDIAN);
        b = new byte[size];
        System.arraycopy(res, start, b, 0, b.length);
        String text = new String(b);
        start += b.length;
        return new Pair<>(start, text);
    }

    public static class TTSResponse {

        public Header header;
        public Optional optional;
        public int payloadSize;
        transient public byte[] payload;

        public String payloadJson;

        @Override
        public String toString() {
            return JSONObject.toJSONString(this);
        }

    }

    public static class Header {

        public int protocolVersion = VolcTTSConstants.PROTOCOL_VERSION;
        public int headerSize = VolcTTSConstants.DEFAULT_HEADER_SIZE;
        public int messageType;
        public int messageTypeSpecificFlags = VolcTTSConstants.MSG_TYPE_FLAG_WITH_EVENT;
        public int serializationMethod = VolcTTSConstants.NO_SERIALIZATION;
        public int messageCompression = VolcTTSConstants.COMPRESSION_NO;
        public int reserved = 0;

        public Header() {
        }

        public Header(int protocolVersion, int headerSize, int messageType, int messageTypeSpecificFlags,
                      int serializationMethod, int messageCompression, int reserved) {
            this.protocolVersion = protocolVersion;
            this.headerSize = headerSize;
            this.messageType = messageType;
            this.messageTypeSpecificFlags = messageTypeSpecificFlags;
            this.serializationMethod = serializationMethod;
            this.messageCompression = messageCompression;
            this.reserved = reserved;
        }

        /**
         * 转成 byte 数组
         */
        public byte[] getBytes() {
            return new byte[]{
                    // Protocol version | Header size (4x)
                    (byte) ((protocolVersion << 4) | headerSize),
                    // Message type | Message type specific flags
                    (byte) (messageType << 4 | messageTypeSpecificFlags),
                    // Serialization method | Compression method
                    (byte) ((serializationMethod << 4) | messageCompression),
                    (byte) reserved
            };
        }
    }

    public static class Optional {

        public int event = VolcTTSConstants.EVENT_NONE;
        public String sessionId;

        public int errorCode;
        public String connectionId;

        public String responseMetaJson;

        public Optional(int event, String sessionId) {
            this.event = event;
            this.sessionId = sessionId;
        }

        public Optional() {
        }

        public byte[] getBytes() {
            byte[] bytes = new byte[0];
            if (event != VolcTTSConstants.EVENT_NONE) {
                bytes = ByteUtil.intToBytes(event, ByteOrder.BIG_ENDIAN);
            }
            if (sessionId != null) {
                byte[] sessionIdSize = ByteUtil.intToBytes(sessionId.getBytes().length, ByteOrder.BIG_ENDIAN);
                final byte[] temp = bytes;
                int desPos = 0;
                bytes = new byte[temp.length + sessionIdSize.length + sessionId.getBytes().length];
                System.arraycopy(temp, 0, bytes, desPos, temp.length);
                desPos += temp.length;
                System.arraycopy(sessionIdSize, 0, bytes, desPos, sessionIdSize.length);
                desPos += sessionIdSize.length;
                System.arraycopy(sessionId.getBytes(), 0, bytes, desPos, sessionId.getBytes().length);

            }
            return bytes;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String appid;

        private String accessToken;

        private String resourceId;

        public Builder appid(String appid) {
            this.appid = appid;
            return this;
        }

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        private Map<String, String> headers() {
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Api-App-Key", appid);
            headers.put("X-Api-Access-Key", accessToken);
            headers.put("X-Api-Resource-Id", resourceId);
            headers.put("X-Api-Connect-Id", IdUtil.fastUUID());
            return headers;
        }

        public VolcTTSClient build() {
            if (CharSequenceUtil.isBlank(appid)) {
                throw new IllegalArgumentException("appid must not be blank.");
            }
            if (CharSequenceUtil.isBlank(accessToken)) {
                throw new IllegalArgumentException("accessToken must not be blank.");
            }
            if (CharSequenceUtil.isBlank(resourceId)) {
                this.resourceId = "volc.service_type.10029";
            }
            return new VolcTTSClient(this);
        }
    }

}

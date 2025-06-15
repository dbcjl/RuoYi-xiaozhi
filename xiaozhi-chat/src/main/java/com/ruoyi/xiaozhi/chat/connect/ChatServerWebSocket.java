package com.ruoyi.xiaozhi.chat.connect;

import ai.onnxruntime.OrtSession;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.ruoyi.xiaozhi.chat.properties.ChatServerProperties;
import com.ruoyi.xiaozhi.chat.providers.asr.ASREngine;
import com.ruoyi.xiaozhi.chat.tool.CallTool;
import com.ruoyi.xiaozhi.chat.utils.WebSocketUtils;
import com.ruoyi.xiaozhi.feign.DeviceClient;
import com.ruoyi.xiaozhi.feign.vo.DeviceDetailVo;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 小智聊天服务WebSocket
 */
@Slf4j
@Component
public class ChatServerWebSocket extends WebSocketServer {

    private final ChatServerProperties properties;

    @Setter(onMethod_ = @Autowired)
    private OrtSession vadSession;

    @Setter(onMethod_ = @Autowired)
    private ASREngine asrEngine;

    @Setter(onMethod_ = @Autowired)
    private ChatClient chatClient;

    @Setter(onMethod_ = @Autowired)
    private ChatMemory chatMemory;

    @Setter(onMethod_ = @Autowired)
    private CallTool callTool;

    @Setter(onMethod_ = @Autowired)
    private DeviceClient deviceClient;

    public ChatServerWebSocket(ChatServerProperties properties) {
        super(new InetSocketAddress(properties.getPort()));
        this.properties = properties;
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
        // 验证访问路径是否正确
        URI uri = URI.create(request.getResourceDescriptor());
        String path = CharSequenceUtil.removeAllSuffix(uri.getPath(), "/");
        if (!CharSequenceUtil.equals(path, properties.getContextPath())) {
            log.error("Invalid path: {}", path);
            throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid path");
        }
        // 从请求头中获取设备MAC地址
        String deviceId = WebSocketUtils.deviceId(request);
        if (CharSequenceUtil.isBlank(deviceId)) {
            log.error("Invalid deviceId: {}", deviceId);
            throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid deviceId");
        }
        // 取出deviceId匹配后台的mac地址
        DeviceDetailVo deviceVo = deviceClient.info(deviceId);
        Assert.notNull(deviceVo, () -> {
            log.error("Unable to find the device, deviceId: {}", deviceId);
            return new InvalidDataException(CloseFrame.POLICY_VALIDATION, "查询不到该设备");
        });
        // 暂存设备信息
        conn.setAttachment(deviceVo);
        return builder;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String clientId = WebSocketUtils.clientId(handshake);
        String deviceId = WebSocketUtils.deviceId(handshake);
        log.info("onOpen, clientId: {}, deviceId: {}", clientId, deviceId);
        // 获取设备信息
        DeviceDetailVo deviceVo = conn.getAttachment();
        // 构建处理器
        conn.setAttachment(ChatServerHandler.builder(conn)
                .vadModelSession(vadSession)
                .chatTools(new ArrayList<>(Arrays.asList(ToolCallbacks.from(callTool))))
                .asrProvider(asrEngine)
                .chatClient(chatClient)
                .chatMemory(chatMemory)
                .ttsProvider(deviceVo.getTtsProvider())
                .prompt(deviceVo.getPrompt())
                .build());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("onClose, code: {}, reason: {}, remote: {}", code, reason, remote);
        ChatServerHandler handler = conn.getAttachment();
        IoUtil.close(handler);
        conn.setAttachment(null);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ChatServerHandler handler = conn.getAttachment();
        if (handler != null) {
            handler.handleMessage(message);
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        ChatServerHandler handler = conn.getAttachment();
        if (handler != null) {
            handler.handleMessage(message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("onError", ex);
    }

    /** 服务启动回调 */
    @Override
    public void onStart() {
        String localhostStr = NetUtil.getLocalhostStr();
        localhostStr = CharSequenceUtil.isBlank(localhostStr) ? "127.0.0.1" : localhostStr;
        log.info("Chat server is running at ws://{}:{}{}", localhostStr, properties.getPort(), properties.getContextPath());
    }

}

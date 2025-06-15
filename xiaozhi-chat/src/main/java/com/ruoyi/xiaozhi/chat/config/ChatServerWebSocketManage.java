package com.ruoyi.xiaozhi.chat.config;

import com.ruoyi.xiaozhi.chat.connect.ChatServerWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 聊天服务生命周期管理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatServerWebSocketManage implements InitializingBean, DisposableBean {

    private final ChatServerWebSocket chatServerWebSocket;

    /**
     * 等待其他bean初始化完成时，启动聊天服务
     */
    @Override
    public void afterPropertiesSet() {
        log.info("Chat server starting...");
        chatServerWebSocket.start();
    }

    /**
     * 应用销毁时，停止聊天服务
     */
    @Override
    public void destroy() throws InterruptedException {
        log.info("Chat server stopping...");
        chatServerWebSocket.stop();
        log.info("Chat server stopped completed");
    }

}

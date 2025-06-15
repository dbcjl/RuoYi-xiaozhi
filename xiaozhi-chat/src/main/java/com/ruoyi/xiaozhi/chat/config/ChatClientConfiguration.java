package com.ruoyi.xiaozhi.chat.config;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.xiaozhi.chat.properties.ChatClientProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 聊天客户端配置类
 */
@Configuration
public class ChatClientConfiguration {

    /**
     * 消息窗口记忆体配置
     */
    @Bean
    public ChatMemory messageMemoryWindow() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    /**
     * 构建 AI 聊天客户端
     */
    @Bean
    public ChatClient chatClient(ChatMemory messageMemoryWindow, ChatClientProperties properties) {
        OpenAiApi.Builder builder = OpenAiApi.builder()
                .baseUrl(properties.getUrl())
                .apiKey(properties.getApiKey());
        if (StrUtil.isNotBlank(properties.getPath())) {
            builder.completionsPath(properties.getPath());
        }
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(builder.build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .build())
                .build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(messageMemoryWindow).build())
                .build();
    }
}

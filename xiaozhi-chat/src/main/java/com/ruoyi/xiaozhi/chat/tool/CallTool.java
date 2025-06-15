package com.ruoyi.xiaozhi.chat.tool;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.xiaozhi.chat.connect.ChatServerHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 工具调用 Function Calling
 * @author ruoyi-xiaozhi
 */
@Slf4j
@Component
public class CallTool {

    /**
     * 退出意图处理
     * @param bye 告别语
     */
    @Tool(returnDirect = true, description = "当且仅当用户明确表示要结束聊天时调用，例如说“我没有问题了”、“我们聊到这吧”、“再见”、“结束对话”等。不要在用户表达不满、沉默、犹豫或切换话题时调用此函数，必须确认用户确实想要结束聊天。")
    public String exitChat(@ToolParam(description = "和用户友好结束对话的告别语") String bye, ToolContext toolContext) {
        if (StrUtil.isBlank(bye)) {
            bye = "再见，祝您生活愉快";
        }
        ChatServerHandler conn = (ChatServerHandler) toolContext.getContext().get("conn");
        conn.closeAfterChat = true;
        log.info("exitChat: {}", bye);
        return bye;
    }

    /**
     * 获取当前日期和时间
     * @return  用户时区中的当前日期和时间
     */
    @Tool(description = "获取用户时区中的当前日期和时间")
    public String getCurrentDateTime() {
        String string = LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
        log.info("获取用户时区中的当前日期和时间: {}", string);
        return string;
    }


}

package com.ruoyi.xiaozhi.chat.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.xiaozhi.feign.constant.ChatConstants;
import org.java_websocket.handshake.ClientHandshake;

import java.nio.charset.StandardCharsets;

/**
 * WebSocket工具类
 */
public class WebSocketUtils {
    private WebSocketUtils() {
    }

    /**
     * 获取设备ID
     * @param clientHandshake   客户端握手信息
     * @return  设备ID（MAC地址）
     */
    public static String deviceId(ClientHandshake clientHandshake) {
        // 先从头部获取设备ID
        String deviceId = clientHandshake.getFieldValue(ChatConstants.HEADER_DEVICE_ID);
        if (CharSequenceUtil.isNotBlank(deviceId)) {
            return deviceId;
        }
        UrlQuery urlQuery = UrlQuery.of(clientHandshake.getResourceDescriptor(), StandardCharsets.UTF_8);
        return Convert.toStr(urlQuery.get("device_mac"), null);
    }

    /**
     * 获取客户端ID
     * @param clientHandshake   客户端握手信息
     * @return  客户端ID
     */
    public static String clientId(ClientHandshake clientHandshake) {
        // 先从头部获取客户端ID
        String clientId = clientHandshake.getFieldValue(ChatConstants.HEADER_CLIENT_ID);
        if (CharSequenceUtil.isNotBlank(clientId)) {
            return clientId;
        }
        UrlQuery urlQuery = UrlQuery.of(clientHandshake.getResourceDescriptor(), StandardCharsets.UTF_8);
        return Convert.toStr(urlQuery.get("device_id"), null);
    }

}

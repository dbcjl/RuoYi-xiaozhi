package com.rouyi.xiaozhi.intellect.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.TimeZone;

/**
 * 设备OTA检测版本返回体，包含激活码要求
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceReportVO {

    /** 服务器时间信息 */
    @JsonProperty("server_time")
    private ServerTime serverTime;

    /** 激活码 */
    private Activation activation;

    /** 错误信息 */
    private String error;

    /** 固件版本信息 */
    private Firmware firmware;

    /** WebSocket配置 */
    private Websocket websocket;

    public static DeviceReportVO error(String error) {
        DeviceReportVO deviceReportVO = new DeviceReportVO();
        deviceReportVO.setError(error);
        return deviceReportVO;
    }

    @Data
    public static class Firmware {
        /** 版本号 */
        private String version;
        /** 下载地址 */
        private String url;
    }

    @Data
    public static class ServerTime {
        /** 时间戳 */
        private Long timestamp;

        /** 时区 */
        private String timezone;

        /** 时区偏移量，单位为分钟 */
        @JsonProperty("timezone_offset")
        private Integer timezoneOffset;
    }

    @Data
    public static class Activation {
        /** 激活码 */
        private String code;

        /** 激活码信息: 激活地址 */
        private String message;

        /** 挑战码 */
        private String challenge;
    }

    @Data
    public static class Websocket {
        /** WebSocket服务器地址 */
        private String url;
    }

    public static ServerTime buildServerTime() {
        ServerTime serverTime = new ServerTime();
        TimeZone tz = TimeZone.getDefault();
        serverTime.setTimestamp(Instant.now().toEpochMilli());
        serverTime.setTimezone(tz.getID());
        serverTime.setTimezoneOffset(tz.getOffset(System.currentTimeMillis()) / (60 * 1000));
        return serverTime;
    }

}

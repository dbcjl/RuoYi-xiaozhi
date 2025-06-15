package com.ruoyi.xiaozhi.feign.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.xiaozhi.feign.enums.TTSProviderEnum;
import lombok.Data;

import java.util.Date;

/**
 * 设备信息
 */
@Data
public class DeviceDetailVo {

    /** 设备ID */
    private Long id;

    /** 设备mac地址 */
    private String macAddress;

    /** 客户端ID */
    private String clientId;

    /** 最后连接时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastConnAt;

    /** 智能体名称 */
    private String agentName;

    /** 系统提示词 */
    private String prompt;

    /** 语音合成模型 */
    private TTSProviderEnum ttsProvider;

}

package com.rouyi.xiaozhi.intellect.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rouyi.xiaozhi.common.annotation.Excel;
import com.rouyi.xiaozhi.common.core.domain.BaseEntity;

/**
 * 设备管理对象 tb_device
 *
 * @author ruoyi-xiaozhi
 */
@Data
@TableName("tb_device")
@EqualsAndHashCode(callSuper = true)
public class Device extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** mac地址 */
    @Excel(name = "mac地址")
    private String macAddress;

    /** 客户端ID */
    @Excel(name = "客户端ID")
    private String clientId;

    /** 最后连接时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "最后连接时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date lastConnAt;

    /** 设备状态 */
    @Excel(name = "设备状态")
    private Status status;

    /** 智能体ID */
    @Excel(name = "智能体ID")
    private Long agentId;

    /** 智能体名称 */
    @TableField(exist = false)
    private String agentName;

    /** 设备备注 */
    @Excel(name = "设备备注")
    private String remarks;

    /** 用户称呼 */
    @Excel(name = "用户称呼")
    private String username;

    /** 在线状态 */
    public enum Status {
        /** 在线 */
        ONLINE,
        /** 离线 */
        OFFLINE
    }

}

package com.rouyi.xiaozhi.intellect.model.dto;

import cn.hutool.core.util.RandomUtil;
import lombok.Data;

/**
 * 设备缓存数据
 */
@Data
public class DeviceCacheDTO {

    private String macAddress;

    private String clientId;

    private String checkCode;

    private String board;

    private String version;

    public static DeviceCacheDTO build(DeviceReportDTO deviceReport, String clientId) {
        DeviceCacheDTO deviceCache = new DeviceCacheDTO();
        deviceCache.setMacAddress(deviceReport.getMacAddress());
        deviceCache.setClientId(clientId);
        // 生成随机的6位验证码
        deviceCache.setCheckCode(RandomUtil.randomNumbers(6));
        // 板子信息
        String board = "unknown";
        if (deviceReport.getBoard() != null && deviceReport.getBoard().getType() != null) {
            board = deviceReport.getBoard().getType();
        }else if (deviceReport.getChipModelName() != null) {
            board = deviceReport.getChipModelName();
        }
        deviceCache.setBoard(board);
        // 版本信息
        if (deviceReport.getApplication() != null && deviceReport.getApplication().getVersion() != null) {
            deviceCache.setVersion(deviceReport.getApplication().getVersion());
        }
        return deviceCache;
    }

}

package com.rouyi.xiaozhi.web.controller.api;

import cn.hutool.core.text.CharSequenceUtil;
import com.rouyi.xiaozhi.intellect.model.dto.DeviceReportDTO;
import com.rouyi.xiaozhi.intellect.model.vo.DeviceReportVO;
import com.rouyi.xiaozhi.intellect.service.IDeviceService;
import com.ruoyi.xiaozhi.feign.constant.ChatConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * OTA升级接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ota")
public class ApiOTAController {

    private final IDeviceService deviceService;

    /**
     * OTA版本和设备激活状态检查
     * @param deviceReportDTO   设备上报信息
     * @param deviceId          设备ID
     * @param clientId          客户端ID
     * @return OTA和设备激活状态信息
     */
    @PostMapping
    public DeviceReportVO checkOTAVersion(@RequestBody DeviceReportDTO deviceReportDTO,
                                          @RequestHeader(ChatConstants.HEADER_DEVICE_ID) String deviceId,
                                          @RequestHeader(value = ChatConstants.HEADER_CLIENT_ID, required = false) String clientId) {
        if (CharSequenceUtil.isBlank(deviceId)) {
            return DeviceReportVO.error("Device ID is required");
        }
        clientId = CharSequenceUtil.isBlank(clientId) ? deviceId : clientId;
        String macAddress = deviceReportDTO.getMacAddress();
        // 设备Id和Mac地址应是一致的, 并且必须需要application字段
        if (!deviceId.equals(macAddress) || deviceReportDTO.getApplication() == null) {
            return DeviceReportVO.error("Invalid OTA request");
        }
        return deviceService.checkDeviceActive(deviceReportDTO, clientId);
    }

}

package com.rouyi.xiaozhi.web.controller.api;

import com.rouyi.xiaozhi.intellect.service.IDeviceService;
import com.ruoyi.xiaozhi.feign.core.R;
import com.ruoyi.xiaozhi.feign.vo.DeviceDetailVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 设备接口控制层
 * @author ruoyi-xiaozhi
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device")
public class ApiDeviceController {

    private final IDeviceService deviceService;

    /**
     * 查询设备信息
     * @param macAddress    mac地址
     * @return  设备信息
     */
    @GetMapping("/info")
    public R<DeviceDetailVo> info(@RequestParam String macAddress) {
        return R.ok(deviceService.detail(macAddress));
    }

    /**
     * 设备上线
     * @param deviceId  设备ID
     * @return  操作结果
     */
    @PostMapping("/online/{deviceId}")
    public R<Void> onlineStatus(@PathVariable Long deviceId) {
        deviceService.online(deviceId);
        return R.ok();
    }

    /**
     * 设备下线
     * @param deviceId  设备ID
     * @return  操作结果
     */
    @PostMapping("/offline/{deviceId}")
    public R<Void> offlineStatus(@PathVariable Long deviceId) {
        deviceService.offline(deviceId);
        return R.ok();
    }

}

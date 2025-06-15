package com.ruoyi.xiaozhi.feign;

import com.ruoyi.xiaozhi.feign.vo.DeviceDetailVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 设备服务接口
 */
@FeignClient(name = "device-client", url = "${chat.server.manage-url:http://localhost:8080}", path = "/api/device")
public interface DeviceClient {

    /**
     * 查询设备信息
     * @param macAddress    mac地址
     * @return  设备信息
     */
    @GetMapping("/info")
    DeviceDetailVo info(@RequestParam("macAddress") String macAddress);

    /**
     * 设备上线
     * @param deviceId  设备ID
     */
    @PostMapping("/online/{deviceId}")
    void onlineStatus(@PathVariable("deviceId") Long deviceId);

    /**
     * 设备下线
     * @param deviceId  设备ID
     */
    @PostMapping("/offline/{deviceId}")
    void offlineStatus(@PathVariable("deviceId") Long deviceId);

}

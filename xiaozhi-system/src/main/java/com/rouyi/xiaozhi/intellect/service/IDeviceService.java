package com.rouyi.xiaozhi.intellect.service;

import java.util.List;
import com.rouyi.xiaozhi.intellect.domain.Device;
import com.rouyi.xiaozhi.intellect.model.dto.DeviceReportDTO;
import com.rouyi.xiaozhi.intellect.model.vo.DeviceReportVO;
import com.ruoyi.xiaozhi.feign.vo.DeviceDetailVo;

/**
 * 设备管理Service接口
 * 
 * @author ruoyi-xiaozhi
 */
public interface IDeviceService {
    /**
     * 查询设备管理
     * 
     * @param id 设备管理主键
     * @return 设备管理
     */
    Device selectDeviceById(Long id);

    /**
     * 查询设备管理列表
     * 
     * @param device 设备管理
     * @return 设备管理集合
     */
    List<Device> selectDeviceList(Device device);

    /**
     * 修改设备管理
     * 
     * @param device 设备管理
     * @return 结果
     */
    int updateDevice(Device device);

    /**
     * 批量删除设备管理
     * 
     * @param ids 需要删除的设备管理主键集合
     * @return 结果
     */
    int deleteDeviceByIds(Long[] ids);

    /**
     * 检测设备激活
     * @param deviceReportDTO   设备上报信息
     * @param clientId          客户端ID
     * @return  设备上报响应
     */
    DeviceReportVO checkDeviceActive(DeviceReportDTO deviceReportDTO, String clientId);

    /**
     * 修改设备的状态为在线
     * @param deviceId  设备ID
     */
    void online(Long deviceId);

    /**
     * 修改设备的状态为离线
     * @param deviceId  设备ID
     */
    void offline(Long deviceId);

    /**
     * 设备激活
     * @param code  设备激活验证码
     */
    void activation(String code);

    /**
     * 设备详情
     * @param macAddress    mac地址
     * @return  设备详情信息
     */
    DeviceDetailVo detail(String macAddress);
}

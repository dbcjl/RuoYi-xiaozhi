package com.rouyi.xiaozhi.intellect.mapper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rouyi.xiaozhi.common.core.mapper.CommonMapper;
import com.rouyi.xiaozhi.intellect.domain.Device;

import java.util.List;

/**
 * 设备管理Mapper接口
 *
 * @author ruoyi-xiaozhi
 */
public interface DeviceMapper extends CommonMapper<Device> {

    /**
     * 查询设备管理列表
     */
    default List<Device> selectList(Device device) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(device.getId() != null, Device::getId, device.getId());
        queryWrapper.eq(StrUtil.isNotBlank(device.getMacAddress()), Device::getMacAddress, device.getMacAddress());
        queryWrapper.eq(StrUtil.isNotBlank(device.getClientId()), Device::getClientId, device.getClientId());
        queryWrapper.eq(device.getStatus() != null, Device::getStatus, device.getStatus());
        return this.selectList(queryWrapper);
    }

    /**
     * 根据mac地址查询设备信息
     * @param macAddress    mac地址
     * @return  设备信息
     */
    default Device findByMacAddress(String macAddress) {
        if (StrUtil.isBlank(macAddress)) {
            return null;
        }
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getMacAddress, macAddress);
        return selectOne(queryWrapper);
    }
}

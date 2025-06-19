package com.rouyi.xiaozhi.intellect.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.rouyi.xiaozhi.common.core.redis.RedisCache;
import com.rouyi.xiaozhi.intellect.domain.Agent;
import com.rouyi.xiaozhi.intellect.domain.Device;
import com.rouyi.xiaozhi.intellect.mapper.AgentMapper;
import com.rouyi.xiaozhi.intellect.mapper.DeviceMapper;
import com.rouyi.xiaozhi.intellect.model.dto.DeviceCacheDTO;
import com.rouyi.xiaozhi.intellect.model.dto.DeviceReportDTO;
import com.rouyi.xiaozhi.intellect.model.vo.DeviceReportVO;
import com.rouyi.xiaozhi.intellect.service.IDeviceService;
import com.ruoyi.xiaozhi.feign.vo.DeviceDetailVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 设备管理Service业务层处理
 *
 * @author ruoyi-xiaozhi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements IDeviceService {

    /** 设备激活缓存key */
    public static final String DEVICE_DATA_CACHE_KEY = "ota_activation_data:";

    /** 设备验证码缓存Key */
    public static final String DEVICE_CODE_CACHE_KEY = "ota_activation_code:";

    private final DeviceMapper deviceMapper;

    private final AgentMapper agentMapper;

    private final RedisCache redisCache;

    /**
     * 查询设备管理
     *
     * @param id 设备管理主键
     * @return 设备管理
     */
    @Override
    public Device selectDeviceById(Long id) {
        return deviceMapper.selectById(id);
    }

    /**
     * 查询设备管理列表
     *
     * @param device 设备管理
     * @return 设备管理
     */
    @Override
    public List<Device> selectDeviceList(Device device) {
        List<Device> devices = deviceMapper.selectList(device);
        // 回显智能体名称
        Map<Long, Agent> agentMapping = agentMapper.mapRecordsByIds(devices.stream().map(Device::getAgentId).collect(Collectors.toSet()));
        for (Device item : devices) {
            Agent agent = agentMapping.get(item.getAgentId());
            item.setAgentName(agent != null ? agent.getAgentName() : null);
        }
        return devices;
    }

    /**
     * 修改设备管理
     *
     * @param device 设备管理
     * @return 结果
     */
    @Override
    public int updateDevice(Device device) {
        return deviceMapper.updateById(device);
    }

    /**
     * 批量删除设备管理
     *
     * @param ids 需要删除的设备管理主键
     * @return 结果
     */
    @Override
    public int deleteDeviceByIds(Long[] ids) {
        return deviceMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 检测设备激活
     *
     * @param deviceReportDTO 设备上报信息
     * @param clientId        客户端ID
     * @return 设备上报响应
     */
    @Override
    public DeviceReportVO checkDeviceActive(DeviceReportDTO deviceReportDTO, String clientId) {
        DeviceReportVO response = new DeviceReportVO();
        response.setServerTime(DeviceReportVO.buildServerTime());

        // TODO OTA升级暂时不做
        DeviceReportVO.Firmware firmware = new DeviceReportVO.Firmware();
        firmware.setVersion(deviceReportDTO.getApplication().getVersion());
        firmware.setUrl("");
        response.setFirmware(firmware);

        // 获取mac地址
        String macAddress = deviceReportDTO.getMacAddress();
        // 查询设备是否存在平台
        Device device = deviceMapper.findByMacAddress(macAddress);
        if (device == null) {
            String code = buildActivationCode(deviceReportDTO, clientId);
            // 设置校验码
            DeviceReportVO.Activation activation = new DeviceReportVO.Activation();
            activation.setCode(code);
            activation.setMessage("激活码: " + code);
            response.setActivation(activation);
        }else {
            DeviceReportVO.Websocket websocket = new DeviceReportVO.Websocket();
            websocket.setUrl("ws://" + NetUtil.getLocalhost().getHostAddress() +":8082/xiaozhi/v1");
            response.setWebsocket(websocket);
        }
        return response;
    }

    /**
     * 构建设备激活码
     * @param deviceReportDTO   设备上报信息
     * @param clientId          客户端ID
     * @return  设备验证码
     */
    private String buildActivationCode(DeviceReportDTO deviceReportDTO, String clientId) {
        String dataKey = this.deviceDataCacheKey(deviceReportDTO.getMacAddress());
        // 查询之前是否已经有验证码了，防止同个设备不断生成新的验证码
        DeviceCacheDTO deviceCache = redisCache.getCacheObject(dataKey);
        if (deviceCache == null) {
            deviceCache = DeviceCacheDTO.build(deviceReportDTO, clientId);
            // 5分钟过期
            redisCache.setCacheObject(dataKey, deviceCache, 5, TimeUnit.MINUTES);
            // 设置验证码反查（反查的不需要过期）
            String codeKey = DEVICE_CODE_CACHE_KEY + deviceCache.getCheckCode();
            redisCache.setCacheObject(codeKey, deviceCache.getMacAddress());
        }else {
            // 重置过期过期时间
            redisCache.expire(dataKey, 5, TimeUnit.MINUTES);
        }
        return deviceCache.getCheckCode();
    }

    /** 设备数据缓存Key */
    private String deviceDataCacheKey(String macAddress) {
        return DEVICE_DATA_CACHE_KEY + StrUtil.replace(macAddress, ":", "_");
    }

    /**
     * 修改设备的状态为在线
     *
     * @param deviceId 设备ID
     */
    @Override
    public void online(Long deviceId) {
        Assert.notNull(deviceId, "deviceId must not be null.");
        Device update = new Device();
        update.setId(deviceId);
        update.setLastConnAt(DateUtil.date());
        update.setStatus(Device.Status.ONLINE);
        deviceMapper.updateById(update);
    }

    /**
     * 修改设备的状态为离线
     *
     * @param deviceId 设备ID
     */
    @Override
    public void offline(Long deviceId) {
        Assert.notNull(deviceId, "deviceId must not be null.");
        Device update = new Device();
        update.setId(deviceId);
        update.setStatus(Device.Status.OFFLINE);
        deviceMapper.updateById(update);
    }

    /**
     * 设备激活
     *
     * @param code 设备激活验证码
     */
    @Override
    public void activation(String code) {
        Assert.notEmpty(code, "设备激活验证码不能为空");
        // 通过验证码获取设备MAC地址
        String codeKey = DEVICE_CODE_CACHE_KEY + code;
        String macAddress = redisCache.getCacheObject(codeKey);
        if (StrUtil.isBlank(macAddress)) {
            throw new IllegalArgumentException("无效的验证码, code: " + code);
        }
        String dataKey = deviceDataCacheKey(macAddress);
        DeviceCacheDTO deviceCache = redisCache.getCacheObject(dataKey);
        if (deviceCache == null) {
            redisCache.deleteObject(codeKey);
            throw new IllegalStateException("验证码已过期, code: " + code);
        }
        // 判断验证码是否一致
        if (!deviceCache.getCheckCode().equals(code)) {
            throw new IllegalArgumentException("验证码错误, code: " + code);
        }
        // 先删除缓存
        redisCache.deleteObject(dataKey);
        redisCache.deleteObject(codeKey);
        // 导入设备信息
        this.importDevice(deviceCache);
    }

    /**
     * 设备详情
     *
     * @param macAddress mac地址
     * @return 设备详情信息
     */
    @Override
    public DeviceDetailVo detail(String macAddress) {
        Assert.notEmpty(macAddress, "macAddress不能为空");
        Device device = deviceMapper.findByMacAddress(macAddress);
        Assert.notNull(device, "device not found, macAddress: {}", macAddress);
        // 获取设备详情信息
        DeviceDetailVo result = new DeviceDetailVo();
        result.setId(device.getId());
        result.setMacAddress(device.getMacAddress());
        result.setClientId(device.getClientId());
        result.setLastConnAt(device.getLastConnAt());
        // 查询智能体
        if (device.getAgentId() == null) {
            throw new IllegalStateException(StrUtil.format("device not agent, macAddress: {}", macAddress));
        }
        Agent agent = agentMapper.selectById(device.getAgentId());
        if (agent == null) {
            throw new IllegalStateException(StrUtil.format("agent not exists, agentId: {}", device.getAgentId()));
        }
        result.setAgentName(agent.getAgentName());
        // 构建系统提示词
        result.setPrompt(buildPrompt(agent.getPrompt(), device.getUsername()));
        result.setTtsProvider(agent.getTtsProvider());
        return result;
    }

    /**
     * 构建系统提示词
     * @param template  提示词模版
     * @param username  称呼
     * @return  系统提示词
     */
    private String buildPrompt(String template, String username) {
        Map<String, String> values = Map.of("username", StrUtil.isBlank(username) ? "小明同学" : username);
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");
        return helper.replacePlaceholders(template, values::get);
    }

    /**
     * 导入设备
     * @param deviceCache   设备缓存信息
     */
    private void importDevice(DeviceCacheDTO deviceCache) {
        String macAddress = deviceCache.getMacAddress();
        Device device = deviceMapper.findByMacAddress(macAddress);
        if (device != null) {
            throw new IllegalStateException("设备已存在, 请勿重复激活导入");
        }
        // 设备直接保存到数据库
        device = new Device();
        device.setMacAddress(macAddress);
        device.setClientId(deviceCache.getClientId());
        device.setStatus(Device.Status.OFFLINE);
        deviceMapper.insert(device);
    }

}

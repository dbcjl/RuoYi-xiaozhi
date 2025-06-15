package com.rouyi.xiaozhi.web.controller.intellect;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rouyi.xiaozhi.common.annotation.Log;
import com.rouyi.xiaozhi.common.core.controller.BaseController;
import com.rouyi.xiaozhi.common.core.domain.AjaxResult;
import com.rouyi.xiaozhi.common.enums.BusinessType;
import com.rouyi.xiaozhi.intellect.domain.Device;
import com.rouyi.xiaozhi.intellect.service.IDeviceService;
import com.rouyi.xiaozhi.common.utils.poi.ExcelUtil;
import com.rouyi.xiaozhi.common.core.page.TableDataInfo;

/**
 * 设备管理Controller
 *
 * @author ruoyi-xiaozhi
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/device")
public class DeviceController extends BaseController {

    private final IDeviceService deviceService;

    /**
     * 查询设备管理列表
     */
    @PreAuthorize("@ss.hasPermi('intellect:device:list')")
    @GetMapping("/list")
    public TableDataInfo list(Device device) {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    /**
     * 导出设备管理列表
     */
    @PreAuthorize("@ss.hasPermi('intellect:device:export')")
    @Log(title = "设备管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Device device) {
        List<Device> list = deviceService.selectDeviceList(device);
        ExcelUtil<Device> util = new ExcelUtil<>(Device.class);
        util.exportExcel(response, list, "设备管理数据");
    }

    /**
     * 获取设备管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('intellect:device:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(deviceService.selectDeviceById(id));
    }

    /**
     * 修改设备管理
     */
    @PreAuthorize("@ss.hasPermi('intellect:device:edit')")
    @Log(title = "设备管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Device device) {
        return toAjax(deviceService.updateDevice(device));
    }

    /**
     * 删除设备管理
     */
    @PreAuthorize("@ss.hasPermi('intellect:device:remove')")
    @Log(title = "设备管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(deviceService.deleteDeviceByIds(ids));
    }

    /**
     * 激活设备（导入）
     * @param code  验证码
     * @return  激活结果
     */
    @PreAuthorize("@ss.hasPermi('intellect:device:activation')")
    @Log(title = "设备管理", businessType = BusinessType.ACTIVATION)
    @PostMapping("/activation/{code}")
    public AjaxResult activation(@PathVariable String code) {
        deviceService.activation(code);
        return AjaxResult.success();
    }
}

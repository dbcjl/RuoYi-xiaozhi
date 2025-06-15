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
import com.rouyi.xiaozhi.intellect.domain.Agent;
import com.rouyi.xiaozhi.intellect.service.IAgentService;
import com.rouyi.xiaozhi.common.utils.poi.ExcelUtil;
import com.rouyi.xiaozhi.common.core.page.TableDataInfo;

/**
 * 智能体Controller
 *
 * @author ruoyi-xiaozhi
 * @date 2025-06-14
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intellect/agent")
public class AgentController extends BaseController {

    private final IAgentService agentService;

    /**
     * 查询智能体列表
     */
    @PreAuthorize("@ss.hasPermi('intellect:agent:list')")
    @GetMapping("/list")
    public TableDataInfo list(Agent agent) {
        startPage();
        List<Agent> list = agentService.selectAgentList(agent);
        return getDataTable(list);
    }

    /**
     * 导出智能体列表
     */
    @PreAuthorize("@ss.hasPermi('intellect:agent:export')")
    @Log(title = "智能体", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Agent agent) {
        List<Agent> list = agentService.selectAgentList(agent);
        ExcelUtil<Agent> util = new ExcelUtil<>(Agent.class);
        util.exportExcel(response, list, "智能体数据");
    }

    /**
     * 获取智能体详细信息
     */
    @PreAuthorize("@ss.hasPermi('intellect:agent:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(agentService.selectAgentById(id));
    }

    /**
     * 新增智能体
     */
    @PreAuthorize("@ss.hasPermi('intellect:agent:add')")
    @Log(title = "智能体", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Agent agent) {
        return toAjax(agentService.insertAgent(agent));
    }

    /**
     * 修改智能体
     */
    @PreAuthorize("@ss.hasPermi('intellect:agent:edit')")
    @Log(title = "智能体", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Agent agent) {
        return toAjax(agentService.updateAgent(agent));
    }

    /**
     * 删除智能体
     */
    @PreAuthorize("@ss.hasPermi('intellect:agent:remove')")
    @Log(title = "智能体", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(agentService.deleteAgentByIds(ids));
    }

    /**
     * 获取智能体下拉列表
     * @return  智能体下拉列表
     */
    @GetMapping("/listAll")
    public AjaxResult listAll() {
        return success(agentService.listAll());
    }
}

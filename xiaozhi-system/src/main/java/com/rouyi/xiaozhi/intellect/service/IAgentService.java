package com.rouyi.xiaozhi.intellect.service;

import java.util.List;
import com.rouyi.xiaozhi.intellect.domain.Agent;

/**
 * 智能体Service接口
 * 
 * @author ruoyi-xiaozhi
 */
public interface IAgentService {
    /**
     * 查询智能体
     * 
     * @param id 智能体主键
     * @return 智能体
     */
    Agent selectAgentById(Long id);

    /**
     * 查询智能体列表
     * 
     * @param agent 智能体
     * @return 智能体集合
     */
    List<Agent> selectAgentList(Agent agent);

    /**
     * 新增智能体
     * 
     * @param agent 智能体
     * @return 结果
     */
    int insertAgent(Agent agent);

    /**
     * 修改智能体
     * 
     * @param agent 智能体
     * @return 结果
     */
    int updateAgent(Agent agent);

    /**
     * 批量删除智能体
     * 
     * @param ids 需要删除的智能体主键集合
     * @return 结果
     */
    int deleteAgentByIds(Long[] ids);

    /**
     * 获取智能体列表（所有）
     * @return 智能体列表（所有）
     */
    List<Agent> listAll();
}

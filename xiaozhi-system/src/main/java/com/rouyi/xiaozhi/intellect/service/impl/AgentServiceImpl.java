package com.rouyi.xiaozhi.intellect.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rouyi.xiaozhi.intellect.domain.Agent;
import com.rouyi.xiaozhi.intellect.mapper.AgentMapper;
import com.rouyi.xiaozhi.intellect.service.IAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 智能体Service业务层处理
 * 
 * @author ruoyi-xiaozhi
 */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final AgentMapper agentMapper;

    /**
     * 查询智能体
     * 
     * @param id 智能体主键
     * @return 智能体
     */
    @Override
    public Agent selectAgentById(Long id) {
        return agentMapper.selectById(id);
    }

    /**
     * 查询智能体列表
     * 
     * @param agent 智能体
     * @return 智能体
     */
    @Override
    public List<Agent> selectAgentList(Agent agent) {
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(agent.getId() != null, Agent::getId, agent.getId());
        queryWrapper.like(StrUtil.isNotBlank(agent.getAgentName()), Agent::getAgentName, agent.getAgentName());
        queryWrapper.eq(agent.getTtsProvider() != null, Agent::getTtsProvider, agent.getTtsProvider());
        return agentMapper.selectList(queryWrapper);
    }

    /**
     * 新增智能体
     * 
     * @param agent 智能体
     * @return 结果
     */
    @Override
    public int insertAgent(Agent agent) {
        return agentMapper.insert(agent);
    }

    /**
     * 修改智能体
     * 
     * @param agent 智能体
     * @return 结果
     */
    @Override
    public int updateAgent(Agent agent) {
        return agentMapper.updateById(agent);
    }

    /**
     * 批量删除智能体
     * 
     * @param ids 需要删除的智能体主键
     * @return 结果
     */
    @Override
    public int deleteAgentByIds(Long[] ids) {
        return agentMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 获取智能体列表（所有）
     *
     * @return 智能体列表（所有）
     */
    @Override
    public List<Agent> listAll() {
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Agent::getId, Agent::getAgentName);
        return agentMapper.selectList(queryWrapper);
    }

}

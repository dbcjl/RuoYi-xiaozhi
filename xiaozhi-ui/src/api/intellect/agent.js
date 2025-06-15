import request from '@/utils/request'

// 查询智能体列表
export function listAgent(query) {
  return request({
    url: '/intellect/agent/list',
    method: 'get',
    params: query
  })
}

// 查询智能体详细
export function getAgent(id) {
  return request({
    url: '/intellect/agent/' + id,
    method: 'get'
  })
}

// 新增智能体
export function addAgent(data) {
  return request({
    url: '/intellect/agent',
    method: 'post',
    data: data
  })
}

// 修改智能体
export function updateAgent(data) {
  return request({
    url: '/intellect/agent',
    method: 'put',
    data: data
  })
}

// 删除智能体
export function delAgent(id) {
  return request({
    url: '/intellect/agent/' + id,
    method: 'delete'
  })
}

// 获取智能体下拉框列表
export function agentListAll() {
  return request({
    url: '/intellect/agent/listAll',
    method: 'get'
  })
}

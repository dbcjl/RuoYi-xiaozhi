import request from '@/utils/request'

// 查询设备管理列表
export function listDevice(query) {
  return request({
    url: '/intellect/device/list',
    method: 'get',
    params: query
  })
}

// 查询设备管理详细
export function getDevice(id) {
  return request({
    url: '/intellect/device/' + id,
    method: 'get'
  })
}

// 新增设备管理
export function addDevice(data) {
  return request({
    url: '/intellect/device',
    method: 'post',
    data: data
  })
}

// 修改设备管理
export function updateDevice(data) {
  return request({
    url: '/intellect/device',
    method: 'put',
    data: data
  })
}

// 删除设备管理
export function delDevice(id) {
  return request({
    url: '/intellect/device/' + id,
    method: 'delete'
  })
}

// 设备激活
export function activationDevice(code) {
  return request({
    url: '/intellect/device/activation/' + code,
    method: 'post'
  })
}

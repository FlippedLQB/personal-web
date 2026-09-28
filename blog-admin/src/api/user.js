import request from '@/utils/request'

// 用户列表（仅 super_admin）
export function getUserList(params) {
  return request({
    url: '/admin/user/list',
    method: 'get',
    params
  })
}

// 管理员建号，返回随机密码
export function createUser(data) {
  return request({
    url: '/admin/user',
    method: 'post',
    data
  })
}

// 给用户分配角色
export function assignUserRoles(id, roleIds) {
  return request({
    url: `/admin/user/${id}/roles`,
    method: 'put',
    data: { roleIds }
  })
}

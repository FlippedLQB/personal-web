import request from '@/utils/request'

// 角色列表
export function getRoleList() {
  return request({
    url: '/rbac/role/list',
    method: 'get'
  })
}

// 新增角色
export function createRole(data) {
  return request({
    url: '/rbac/role',
    method: 'post',
    data
  })
}

// 编辑角色
export function updateRole(data) {
  return request({
    url: '/rbac/role',
    method: 'put',
    data
  })
}

// 删除角色
export function deleteRole(id) {
  return request({
    url: `/rbac/role/${id}`,
    method: 'delete'
  })
}

// 查询角色已绑定的权限 ID 列表
export function getRolePermissions(id) {
  return request({
    url: `/rbac/role/${id}/permissions`,
    method: 'get'
  })
}

// 给角色分配权限
export function assignRolePermissions(id, permissionIds) {
  return request({
    url: `/rbac/role/${id}/permissions`,
    method: 'put',
    data: { permissionIds }
  })
}

// 权限树
export function getPermissionTree() {
  return request({
    url: '/rbac/permission/tree',
    method: 'get'
  })
}

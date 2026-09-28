import request from '@/utils/request'

// 登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 获取当前用户信息（含角色列表）
export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

// 获取当前用户的菜单与权限码
export function getMyMenus() {
  return request({
    url: '/rbac/my/menus',
    method: 'get'
  })
}

// 修改个人资料
export function updateProfile(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

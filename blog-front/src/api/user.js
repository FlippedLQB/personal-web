import request from '@/utils/request'

// 用户资料相关 API（需登录）

// 获取当前登录用户信息
export function getUserInfoApi() {
  return request.get('/api/user/info')
}

// 修改个人资料
// body: { nickname, avatar, oldPassword, newPassword }
export function updateProfileApi(body) {
  return request.put('/api/user/profile', body)
}

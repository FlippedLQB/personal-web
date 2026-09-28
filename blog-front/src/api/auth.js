import request from '@/utils/request'

// 认证相关 API（注册 / 登录）

// 用户注册
// body: { username, password }
export function registerApi(body) {
  return request.post('/api/auth/register', body)
}

// 用户登录
// body: { username, password }
// 返回: { token, userId, username, nickname, avatar }
export function loginApi(body) {
  return request.post('/api/auth/login', body)
}

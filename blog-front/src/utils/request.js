import axios from 'axios'
import { router } from '@/router'

// 创建Axios实例
const request = axios.create({
  baseURL: '/', // 通过vite代理转发
  timeout: 15000
})

// 请求拦截器：自动在header加 Authorization: Bearer {token}
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('blog_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 用于在Vue组件外调用 message（通过 setMessageGetter 注入）
let messageGetter = null
export function setMessageGetter(fn) {
  messageGetter = typeof fn === 'function' ? fn : null
}

function getMessage() {
  if (messageGetter) {
    try {
      const m = messageGetter()
      if (m) return m
    } catch (e) {
      // ignore
    }
  }
  // 兜底：返回简易对象，避免在 message 还未注入时报错
  return {
    error: (msg) => console.error('[message.error]', msg),
    success: (msg) => console.log('[message.success]', msg),
    warning: (msg) => console.warn('[message.warning]', msg),
    info: (msg) => console.log('[message.info]', msg)
  }
}

// 响应拦截器：统一处理错误码
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 二进制响应（如下载）直接返回
    if (response.config.responseType === 'blob') {
      return response
    }

    // 业务返回非 200 code，错误提示
    if (res.code !== 200) {
      getMessage().error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 返回业务数据 data 字段
    return res.data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      // 401：token 失效，清除并跳转登录
      localStorage.removeItem('blog_token')
      localStorage.removeItem('blog_user')
      getMessage().warning('登录已失效，请重新登录')
      const currentPath = router.currentRoute.value.fullPath
      if (!currentPath.startsWith('/login')) {
        router.push({
          path: '/login',
          query: { redirect: currentPath }
        })
      }
    } else if (status === 403) {
      getMessage().error('没有权限执行此操作')
    } else if (status === 404) {
      getMessage().error('请求的接口不存在')
    } else if (status >= 500) {
      getMessage().error('服务器繁忙，请稍后重试')
    } else if (error.code === 'ECONNABORTED') {
      getMessage().error('请求超时，请检查网络')
    } else {
      getMessage().error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request

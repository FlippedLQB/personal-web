import axios from 'axios'

// 创建 Axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：自动携带 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理业务码与 401
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 文件下载等场景直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code === 200) {
      return res
    }
    // 业务错误
    window.$message?.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      // token 失效，清理并跳登录
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 避免重复跳转
      if (!location.pathname.includes('/login')) {
        location.href = '/login'
      }
    } else if (status === 403) {
      window.$message?.error('无权限访问')
    } else {
      window.$message?.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request

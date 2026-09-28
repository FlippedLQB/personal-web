import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi } from '@/api/auth'
import { getUserInfoApi, updateProfileApi } from '@/api/user'

// 用户状态管理：token、用户信息、登录/退出
export const useUserStore = defineStore('user', () => {
  // token，从本地存储恢复
  const token = ref(localStorage.getItem('blog_token') || '')

  // 用户信息：{id, username, nickname, avatar}
  const userInfo = ref(JSON.parse(localStorage.getItem('blog_user') || 'null'))

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)

  // 当前用户ID
  const userId = computed(() => userInfo.value?.id || null)

  // 昵称（兼容 nickname / username）
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')

  // 头像
  const avatar = computed(() => userInfo.value?.avatar || '')

  // 用户名
  const username = computed(() => userInfo.value?.username || '')

  // 登录：调用API并保存状态
  const login = async ({ username, password }) => {
    const data = await loginApi({ username, password })
    token.value = data.token
    userInfo.value = {
      id: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar
    }
    // 持久化
    localStorage.setItem('blog_token', token.value)
    localStorage.setItem('blog_user', JSON.stringify(userInfo.value))
    return data
  }

  // 退出：清空状态
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('blog_token')
    localStorage.removeItem('blog_user')
  }

  // 刷新用户信息（用于个人中心）
  const fetchUserInfo = async () => {
    const data = await getUserInfoApi()
    userInfo.value = data
    localStorage.setItem('blog_user', JSON.stringify(data))
    return data
  }

  // 更新个人资料
  const updateProfile = async (payload) => {
    const data = await updateProfileApi(payload)
    userInfo.value = {
      ...userInfo.value,
      nickname: data.nickname ?? userInfo.value.nickname,
      avatar: data.avatar ?? userInfo.value.avatar
    }
    localStorage.setItem('blog_user', JSON.stringify(userInfo.value))
    return data
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    userId,
    nickname,
    avatar,
    username,
    login,
    logout,
    fetchUserInfo,
    updateProfile
  }
})

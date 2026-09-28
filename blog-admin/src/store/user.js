import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo, getMyMenus } from '@/api/auth'

// 用户状态：token、用户信息、菜单、权限码
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    menus: [], // 菜单树
    perms: [], // 权限码数组
    // 标记动态路由是否已注册，避免重复注册
    dynamicRoutesAdded: false
  }),

  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || '',
    avatar: (state) => state.userInfo?.avatar || ''
  },

  actions: {
    // 登录
    async login(payload) {
      const res = await loginApi(payload)
      const data = res.data
      this.token = data.token
      localStorage.setItem('token', data.token)
      // 缓存基础用户信息
      this.userInfo = {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        avatar: data.avatar
      }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return data
    },

    // 拉取当前用户信息（含角色）
    async fetchUserInfo() {
      const res = await getUserInfo()
      this.userInfo = { ...this.userInfo, ...res.data }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return res.data
    },

    // 拉取菜单与权限码
    async fetchMenus() {
      const res = await getMyMenus()
      const data = res.data || {}
      this.menus = data.menus || []
      this.perms = data.perms || []
      return data
    },

    // 退出登录
    logout() {
      this.token = ''
      this.userInfo = null
      this.menus = []
      this.perms = []
      this.dynamicRoutesAdded = false
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})

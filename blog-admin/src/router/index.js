import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

// 静态路由：无需权限即可访问
export const staticRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', hidden: true }
  }
]

// 后台布局（静态注册，但其子路由由动态菜单注入）
const layoutRoute = {
  path: '/admin',
  name: 'AdminLayout',
  component: () => import('@/views/Layout.vue'),
  meta: { title: '管理后台' },
  children: [
    {
      path: 'settings',
      name: 'Settings',
      component: () => import('@/views/Settings.vue'),
      meta: { title: '个人设置' }
    }
  ]
}

// 菜单 path → 页面组件映射表
// 后端返回的菜单 path 用于动态注册路由，此处根据 path 找到对应组件
const viewModules = {
  '/admin/article/list': () => import('@/views/article/ArticleList.vue'),
  '/admin/article/edit/:id?': () => import('@/views/article/ArticleEdit.vue'),
  '/admin/image': () => import('@/views/image/ImageManage.vue'),
  '/admin/rbac/user': () => import('@/views/rbac/UserManage.vue'),
  '/admin/rbac/role': () => import('@/views/rbac/RoleManage.vue'),
  '/admin/rbac/permission': () => import('@/views/rbac/PermissionTree.vue')
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...staticRoutes,
    layoutRoute,
    // 兜底：未匹配跳 404
    { path: '/:pathMatch(.*)*', redirect: '/404' }
  ]
})

// 是否已注册动态路由（使用 store 中的标志位以支持登出后重置）

/**
 * 递归遍历菜单树，将每个有 path 的节点注册为 Layout 的子路由
 */
function registerDynamicRoutes(menus) {
  const queue = [...menus]
  while (queue.length) {
    const node = queue.shift()
    if (node.path && viewModules[node.path] && !router.hasRoute(`dyn_${node.path}`)) {
      router.addRoute('AdminLayout', {
        // 子路由 path 相对 /admin
        path: node.path.replace('/admin/', ''),
        name: `dyn_${node.path}`,
        component: viewModules[node.path],
        meta: { title: node.permName || node.path, permCode: node.permCode }
      })
    }
    if (node.children && node.children.length) {
      queue.push(...node.children)
    }
  }
}

/**
 * 隐藏子路由：不在菜单中显示，但需要根据权限码注册
 * 例如文章编辑页：用户拥有 article:create 或 article:update 时才能访问
 */
const hiddenRoutes = [
  {
    path: '/admin/article/edit/:id?',
    component: () => import('@/views/article/ArticleEdit.vue'),
    title: '文章编辑',
    requirePerms: ['article:create', 'article:update'] // 任一即可
  }
]

function registerHiddenRoutes(perms) {
  const allPerms = perms || []
  const hasAll = allPerms.includes('*')
  hiddenRoutes.forEach((r) => {
    const ok = hasAll || (r.requirePerms || []).some((p) => allPerms.includes(p))
    if (ok && !router.hasRoute(`hidden_${r.path}`)) {
      router.addRoute('AdminLayout', {
        path: r.path.replace('/admin/', ''),
        name: `hidden_${r.path}`,
        component: r.component,
        meta: { title: r.title, hidden: true }
      })
    }
  })
}

/**
 * 清理所有动态注册的路由（登出时调用，避免不同用户路由残留）
 */
export function clearDynamicRoutes() {
  const routes = router.getRoutes()
  routes.forEach((r) => {
    const name = r.name
    if (typeof name === 'string' && (name.startsWith('dyn_') || name.startsWith('hidden_'))) {
      router.removeRoute(name)
    }
  })
}

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const isLogin = !!userStore.token

  // 未登录
  if (!isLogin) {
    if (to.path === '/login') return next()
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 已登录，访问登录页则跳后台首页
  if (to.path === '/login') {
    return next({ path: '/admin' })
  }

  // 动态路由尚未注册：拉取菜单并注册
  if (!userStore.dynamicRoutesAdded) {
    try {
      await userStore.fetchMenus()
      registerDynamicRoutes(userStore.menus)
      registerHiddenRoutes(userStore.perms)
      userStore.dynamicRoutesAdded = true
      // 重新导航以匹配新注册的路由
      return next({ ...to, replace: true })
    } catch (e) {
      // 拉菜单失败：退出登录
      userStore.logout()
      return next({ path: '/login' })
    }
  }

  // 已登录访问根路径：跳后台首页
  if (to.path === '/') {
    return next({ path: '/admin' })
  }

  next()
})

export default router

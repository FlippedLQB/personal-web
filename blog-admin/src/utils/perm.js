import { useUserStore } from '@/store/user'

/**
 * 判断当前用户是否拥有指定权限码
 * @param {string} code 权限码，例如 'article:create'
 * @returns {boolean}
 */
export function hasPerm(code) {
  const userStore = useUserStore()
  const perms = userStore.perms || []
  // 拥有全部权限标识 * 直接通过
  if (perms.includes('*')) return true
  return perms.includes(code)
}

/**
 * 注册全局自定义指令 v-perm
 * 用法：v-perm="'article:create'" 或 v-perm="['article:create','article:update']"
 * 元素无权限时从 DOM 移除
 */
export function setupPermDirective(app) {
  app.directive('perm', {
    mounted(el, binding) {
      const value = binding.value
      const codes = Array.isArray(value) ? value : [value]
      const ok = codes.some((c) => hasPerm(c))
      if (!ok) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  })
}

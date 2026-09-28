<template>
  <n-layout has-sider position="absolute" style="height: 100vh">
    <!-- 左侧菜单 -->
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      :collapsed="collapsed"
      show-trigger
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <div class="logo">
        <span v-if="!collapsed">博客管理后台</span>
        <span v-else>博客</span>
      </div>
      <n-menu
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="20"
        :options="menuOptions"
        :value="activeKey"
        @update:value="handleMenuSelect"
      />
    </n-layout-sider>

    <n-layout>
      <!-- 顶栏 -->
      <n-layout-header bordered class="header">
        <div class="header-left">
          <span class="header-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <n-dropdown :options="userDropdownOptions" @select="handleUserDropdown">
            <div class="user-info">
              <n-avatar round size="small" :src="userStore.avatar" />
              <span class="username">{{ userStore.nickname }}</span>
            </div>
          </n-dropdown>
        </div>
      </n-layout-header>

      <!-- 内容区 -->
      <n-layout-content class="content" :native-scrollbar="false">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDialog, useMessage } from 'naive-ui'
import {
  DocumentTextOutline,
  ImagesOutline,
  SettingsOutline,
  PeopleOutline,
  KeyOutline,
  ShieldCheckmarkOutline,
  CreateOutline
} from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { clearDynamicRoutes } from '@/router'

const route = useRoute()
const router = useRouter()
const dialog = useDialog()
const message = useMessage()
const userStore = useUserStore()

const collapsed = ref(false)

// 图标映射表：菜单 icon 字段 → 图标组件
const iconMap = {
  article: CreateOutline,
  'article-list': DocumentTextOutline,
  image: ImagesOutline,
  settings: SettingsOutline,
  user: PeopleOutline,
  role: KeyOutline,
  permission: ShieldCheckmarkOutline
}

function renderIcon(iconName) {
  const Icon = iconMap[iconName] || iconMap[iconName?.split(':')[0]] || DocumentTextOutline
  return () => h(NIcon, null, { default: () => h(Icon) })
}

// 将后端菜单树转为 n-menu 的 options
function buildMenuOptions(menus) {
  const result = []
  menus.forEach((m) => {
    const item = {
      label: m.permName,
      key: m.path || `perm_${m.id}`,
      icon: m.icon ? renderIcon(m.icon) : undefined
    }
    if (m.children && m.children.length) {
      item.children = buildMenuOptions(m.children)
      // 父级若自身无 path，使用第一个子节点 key
      if (!m.path && item.children.length) {
        item.key = item.children[0].key
      }
    }
    result.push(item)
  })
  return result
}

const menuOptions = computed(() => buildMenuOptions(userStore.menus))

// 当前选中菜单 key
const activeKey = ref(route.path)

watch(
  () => route.path,
  (p) => {
    activeKey.value = p
  },
  { immediate: true }
)

// 当前页面标题
const currentTitle = computed(() => route.meta?.title || '管理后台')

// 菜单点击：跳转
function handleMenuSelect(key) {
  if (!key || key.startsWith('perm_')) return
  if (key !== route.path) {
    router.push(key)
  }
}

// 顶栏用户下拉
const userDropdownOptions = [
  { label: '个人设置', key: 'settings' },
  { type: 'divider', key: 'd1' },
  { label: '退出登录', key: 'logout' }
]

function handleUserDropdown(key) {
  if (key === 'settings') {
    router.push('/admin/settings')
  } else if (key === 'logout') {
    dialog.warning({
      title: '确认退出',
      content: '确定要退出登录吗？',
      positiveText: '退出',
      negativeText: '取消',
      onPositiveClick: () => {
        // 清理动态路由，避免下一用户残留
        clearDynamicRoutes()
        userStore.logout()
        message.success('已退出登录')
        router.replace('/login')
      }
    })
  }
}
</script>

<style scoped>
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1f2a44;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid #efeff5;
  background: #fafafe;
}

.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2a44;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f3f3f5;
}

.username {
  font-size: 14px;
  color: #333;
}

.content {
  height: calc(100vh - 56px);
  padding: 0;
  background: #f5f5f5;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<template>
  <div class="app-layout">
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="app-container app-header-inner">
        <router-link to="/" class="logo">个人博客</router-link>
        <nav class="nav-menu">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/album" class="nav-link">相册</router-link>
          <router-link to="/about" class="nav-link">关于我</router-link>
        </nav>
        <div class="nav-right">
          <template v-if="user.isLoggedIn">
            <span class="welcome-text">你好，{{ user.nickname || user.username }}</span>
            <n-button text @click="handleLogout">退出</n-button>
          </template>
          <template v-else>
            <n-button text @click="router.push('/login')">登录</n-button>
            <n-button text @click="router.push('/register')">注册</n-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="app-main">
      <div class="app-container">
        <router-view />
      </div>
    </main>

    <!-- 页脚 -->
    <footer class="app-footer">
      <div class="app-container">
        <p>© 2026 个人博客 · 基于Vue3 + Vite + NaiveUI 构建</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, useDialog, useMessage } from 'naive-ui'
import { useUserStore } from './store/user'
import { setMessageGetter } from './utils/request'

const router = useRouter()
const user = useUserStore()
const dialog = useDialog()
const message = useMessage()

// 将 message 注入到 request 模块，让响应拦截器能调用全局消息
onMounted(() => {
  setMessageGetter(() => message)
})

onBeforeUnmount(() => {
  setMessageGetter(null)
})

// 退出登录
const handleLogout = () => {
  dialog.warning({
    title: '确认退出',
    content: '确定要退出登录吗？',
    positiveText: '退出',
    negativeText: '取消',
    onPositiveClick: () => {
      user.logout()
      message.success('已退出登录')
      router.push('/')
    }
  })
}
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}

.app-header-inner {
  display: flex;
  align-items: center;
  height: 60px;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #4098fc;
  text-decoration: none;
  margin-right: 40px;
}

.nav-menu {
  display: flex;
  gap: 24px;
  flex: 1;
}

.nav-link {
  color: #555;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.2s;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: #4098fc;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.welcome-text {
  color: #666;
  font-size: 14px;
}

.app-main {
  flex: 1;
  padding: 24px 0;
  background: #f5f7fa;
}

.app-footer {
  background: #fff;
  border-top: 1px solid #eee;
  text-align: center;
  padding: 16px 0;
  color: #999;
  font-size: 13px;
}

/* 响应式：移动端适配 */
@media (max-width: 640px) {
  .logo {
    margin-right: 12px;
    font-size: 17px;
  }
  .nav-menu {
    gap: 12px;
  }
  .nav-link {
    font-size: 14px;
  }
  .welcome-text {
    display: none;
  }
}
</style>

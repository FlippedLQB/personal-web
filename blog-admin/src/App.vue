<template>
  <!-- 全局配置：中文 + 消息容器 -->
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN">
    <n-loading-bar-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-message-provider>
            <GlobalMessageBinder />
            <router-view />
          </n-message-provider>
        </n-notification-provider>
      </n-dialog-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>

<script setup>
import { defineComponent, h } from 'vue'
import { zhCN, dateZhCN, useMessage, useDialog } from 'naive-ui'

// 内部组件：将 useMessage/useDialog 暴露到 window，便于 axios 拦截器使用
const GlobalMessageBinder = defineComponent({
  name: 'GlobalMessageBinder',
  setup() {
    const message = useMessage()
    const dialog = useDialog()
    window.$message = message
    window.$dialog = dialog
    return () => h('div', { style: 'display:none' })
  }
})
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>

<template>
  <!-- NaiveUI 全局 Provider，包裹整个应用以使用 useMessage/useDialog -->
  <n-config-provider :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <AppContent />
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { NConfigProvider, NMessageProvider, NDialogProvider, NNotificationProvider } from 'naive-ui'
import AppContent from './AppContent.vue'

// NaiveUI 主题微调
const themeOverrides = {
  common: {
    primaryColor: '#4098fc'
  }
}
</script>

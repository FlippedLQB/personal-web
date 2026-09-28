<template>
  <!-- 404 页面 -->
  <div class="not-found page-card">
    <div class="code">404</div>
    <p class="text">抱歉，你访问的页面不存在</p>
    <n-button type="primary" @click="router.push('/')">返回首页</n-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'

const router = useRouter()
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 24px;
}

.code {
  font-size: 96px;
  font-weight: 700;
  color: #4098fc;
  line-height: 1;
}

.text {
  color: #888;
  font-size: 15px;
}
</style>

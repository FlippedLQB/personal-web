<template>
  <!-- 登录页 -->
  <div class="auth-page">
    <n-card class="auth-card" :bordered="false" size="large">
      <h2 class="auth-title">登录</h2>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="top"
        @submit.prevent="handleSubmit"
      >
        <n-form-item label="用户名" path="username">
          <n-input
            v-model:value="form.username"
            placeholder="请输入用户名"
            :disabled="loading"
            @keyup.enter="handleSubmit"
          />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            placeholder="请输入密码"
            :disabled="loading"
            @keyup.enter="handleSubmit"
          />
        </n-form-item>
        <div class="auth-actions">
          <n-button
            type="primary"
            block
            :loading="loading"
            @click="handleSubmit"
          >
            登录
          </n-button>
        </div>
        <div class="auth-footer">
          还没有账号？
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
      </n-form>
    </n-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NCard,
  NForm,
  NFormItem,
  NInput,
  NButton,
  useMessage
} from 'naive-ui'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const message = useMessage()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: {
    required: true,
    message: '请输入用户名',
    trigger: ['blur', 'input']
  },
  password: {
    required: true,
    message: '请输入密码',
    trigger: ['blur', 'input']
  }
}

// 提交登录
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userStore.login({ ...form })
    message.success('登录成功')
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' ? redirect : '/')
  } catch (e) {
    // 错误已由响应拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.auth-title {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.auth-actions {
  margin-top: 8px;
}

.auth-footer {
  margin-top: 16px;
  text-align: center;
  color: #888;
  font-size: 14px;
}

.link {
  color: #4098fc;
  margin-left: 4px;
}

.link:hover {
  text-decoration: underline;
}
</style>

<template>
  <div class="login-container">
    <n-card class="login-card" :bordered="false">
      <div class="login-header">
        <h1 class="login-title">博客管理后台</h1>
        <p class="login-subtitle">管理员登录</p>
      </div>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" @keyup.enter="handleLogin">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="form.username" placeholder="请输入用户名" clearable />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input v-model:value="form.password" type="password" show-password-on="click" placeholder="请输入密码" />
        </n-form-item>
        <n-button type="primary" block :loading="loading" @click="handleLogin">登 录</n-button>
      </n-form>
    </n-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: { required: true, message: '请输入用户名', trigger: ['blur', 'input'] },
  password: { required: true, message: '请输入密码', trigger: ['blur', 'input'] }
}

// 登录
async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userStore.login({ ...form })
    message.success('登录成功')
    const redirect = route.query.redirect || '/admin'
    // 路由守卫会自动拉取菜单并注册动态路由
    router.replace(redirect)
  } catch (e) {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2a44 0%, #2c3e6b 50%, #4a5fc1 100%);
}

.login-card {
  width: 380px;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-title {
  margin: 0 0 8px;
  font-size: 22px;
  color: #1f2a44;
}

.login-subtitle {
  margin: 0;
  color: #888;
  font-size: 13px;
}
</style>

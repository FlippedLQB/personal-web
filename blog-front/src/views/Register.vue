<template>
  <!-- 注册页 -->
  <div class="auth-page">
    <n-card class="auth-card" :bordered="false" size="large">
      <h2 class="auth-title">注册</h2>
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
          />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            placeholder="请输入密码"
            :disabled="loading"
          />
        </n-form-item>
        <n-form-item label="确认密码" path="confirmPassword">
          <n-input
            v-model:value="form.confirmPassword"
            type="password"
            show-password-on="click"
            placeholder="请再次输入密码"
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
            注册
          </n-button>
        </div>
        <div class="auth-footer">
          已有账号？
          <router-link to="/login" class="link">去登录</router-link>
        </div>
        <div class="auth-tip">注册成功后，昵称默认等于用户名，可在登录后修改。</div>
      </n-form>
    </n-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NForm,
  NFormItem,
  NInput,
  NButton,
  useMessage
} from 'naive-ui'
import { registerApi } from '@/api/auth'

const router = useRouter()
const message = useMessage()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

// 校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: ['blur', 'input'] },
    { min: 3, max: 20, message: '用户名长度需为 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['blur', 'input'] },
    { min: 6, max: 30, message: '密码长度需为 6-30 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: ['blur', 'input'] },
    {
      validator: (rule, value) => {
        return value === form.password
      },
      message: '两次输入的密码不一致',
      trigger: ['blur', 'input']
    }
  ]
}

// 提交注册
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await registerApi({ username: form.username, password: form.password })
    message.success('注册成功，请登录')
    router.push('/login')
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

.auth-tip {
  margin-top: 8px;
  text-align: center;
  color: #aaa;
  font-size: 12px;
}

.link {
  color: #4098fc;
  margin-left: 4px;
}

.link:hover {
  text-decoration: underline;
}
</style>

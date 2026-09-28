<template>
  <div class="page-container">
    <div class="card-box settings-card">
      <h2 class="settings-title">个人设置</h2>

      <n-tabs type="line" animated>
        <!-- 基本资料 -->
        <n-tab-pane name="profile" tab="基本资料">
          <n-form ref="profileFormRef" :model="profileForm" label-placement="left" label-width="80" style="max-width: 480px">
            <n-form-item label="昵称" path="nickname">
              <n-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
            </n-form-item>
            <n-form-item label="头像">
              <div class="avatar-row">
                <n-avatar round size="large" :src="profileForm.avatar" />
                <upload-image v-model="profileForm.avatar" />
              </div>
            </n-form-item>
            <n-form-item label=" ">
              <n-button type="primary" :loading="profileSaving" @click="saveProfile">保存</n-button>
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 修改密码 -->
        <n-tab-pane name="password" tab="修改密码">
          <n-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-placement="left"
            label-width="100"
            style="max-width: 480px"
          >
            <n-form-item label="原密码" path="oldPassword">
              <n-input v-model:value="pwdForm.oldPassword" type="password" show-password-on="click" placeholder="请输入原密码" />
            </n-form-item>
            <n-form-item label="新密码" path="newPassword">
              <n-input v-model:value="pwdForm.newPassword" type="password" show-password-on="click" placeholder="请输入新密码" />
            </n-form-item>
            <n-form-item label="确认新密码" path="confirmPassword">
              <n-input
                v-model:value="pwdForm.confirmPassword"
                type="password"
                show-password-on="click"
                placeholder="请再次输入新密码"
              />
            </n-form-item>
            <n-form-item label=" ">
              <n-button type="primary" :loading="pwdSaving" @click="savePassword">修改密码</n-button>
            </n-form-item>
          </n-form>
        </n-tab-pane>
      </n-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import UploadImage from '@/components/UploadImage.vue'
import { updateProfile } from '@/api/auth'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()

const profileFormRef = ref(null)
const profileSaving = ref(false)
const profileForm = reactive({
  nickname: userStore.userInfo?.nickname || '',
  avatar: userStore.userInfo?.avatar || ''
})

async function saveProfile() {
  if (!profileForm.nickname) {
    message.warning('请输入昵称')
    return
  }
  profileSaving.value = true
  try {
    await updateProfile({
      nickname: profileForm.nickname,
      avatar: profileForm.avatar
    })
    // 同步到 store
    userStore.userInfo = { ...userStore.userInfo, nickname: profileForm.nickname, avatar: profileForm.avatar }
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
    message.success('保存成功')
  } catch (e) {
    // 错误已提示
  } finally {
    profileSaving.value = false
  }
}

const pwdFormRef = ref(null)
const pwdSaving = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: { required: true, message: '请输入原密码', trigger: ['blur', 'input'] },
  newPassword: { required: true, message: '请输入新密码', trigger: ['blur', 'input'] },
  confirmPassword: {
    required: true,
    trigger: ['blur', 'input'],
    validator(rule, value) {
      if (!value) return new Error('请再次输入新密码')
      if (value !== pwdForm.newPassword) return new Error('两次输入的密码不一致')
      return true
    }
  }
}

async function savePassword() {
  try {
    await pwdFormRef.value?.validate()
  } catch (e) {
    return
  }
  pwdSaving.value = true
  try {
    await updateProfile({
      nickname: profileForm.nickname,
      avatar: profileForm.avatar,
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    message.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    // 错误已提示
  } finally {
    pwdSaving.value = false
  }
}
</script>

<style scoped>
.settings-card {
  max-width: 720px;
}

.settings-title {
  margin: 0 0 16px;
  font-size: 18px;
  color: #1f2a44;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>

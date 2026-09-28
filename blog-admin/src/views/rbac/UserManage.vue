<template>
  <div class="page-container">
    <div class="card-box">
      <div class="search-bar">
        <h3 class="page-title">用户管理</h3>
        <div style="flex: 1"></div>
        <n-button type="primary" @click="openCreate">+ 新建用户</n-button>
        <n-button @click="loadList">刷新</n-button>
      </div>

      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :bordered="false"
        remote
        @update:page="handlePageChange"
        @update:page-size="handleSizeChange"
      />
    </div>

    <!-- 新建用户弹窗 -->
    <n-modal v-model:show="createModal.show" preset="card" title="新建用户" style="width: 480px">
      <n-form ref="createFormRef" :model="createForm" :rules="createRules" label-placement="top">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="createForm.username" placeholder="登录用户名" />
        </n-form-item>
        <n-form-item label="昵称" path="nickname">
          <n-input v-model:value="createForm.nickname" placeholder="显示昵称" />
        </n-form-item>
        <n-form-item label="角色" path="roleIds">
          <n-select
            v-model:value="createForm.roleIds"
            multiple
            :options="roleOptions"
            placeholder="选择角色"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="createModal.show = false">取消</n-button>
          <n-button type="primary" :loading="createModal.loading" @click="submitCreate">确认创建</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 创建成功展示随机密码弹窗 -->
    <n-modal v-model:show="resultModal.show" preset="card" title="创建成功" style="width: 460px">
      <n-alert type="success" title="用户创建成功，请妥善保管以下随机密码">
        <div class="pwd-box">
          <span class="pwd-text">{{ resultModal.password }}</span>
          <n-button size="small" @click="copyPwd">复制</n-button>
        </div>
      </n-alert>
      <template #footer>
        <n-space justify="end">
          <n-button type="primary" @click="resultModal.show = false">我知道了</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 分配角色弹窗 -->
    <n-modal v-model:show="assignModal.show" preset="card" title="分配角色" style="width: 480px">
      <n-form label-placement="top">
        <n-form-item label="用户">
          <n-input :value="`${assignModal.username} (ID: ${assignModal.userId})`" disabled />
        </n-form-item>
        <n-form-item label="角色">
          <n-select
            v-model:value="assignModal.roleIds"
            multiple
            :options="roleOptions"
            placeholder="选择角色"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="assignModal.show = false">取消</n-button>
          <n-button type="primary" :loading="assignModal.loading" @click="submitAssign">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { h, onMounted, reactive, ref } from 'vue'
import { NButton, NSpace, NTag, useMessage } from 'naive-ui'
import { getUserList, createUser, assignUserRoles } from '@/api/user'
import { getRoleList } from '@/api/rbac'

const message = useMessage()

const query = reactive({ page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const roleOptions = ref([])

const columns = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '用户名', key: 'username', width: 140 },
  { title: '昵称', key: 'nickname', width: 140 },
  {
    title: '角色',
    key: 'roles',
    render(row) {
      const roles = row.roles || []
      return h(
        NSpace,
        { size: 4 },
        {
          default: () =>
            roles.map((r) =>
              h(NTag, { size: 'small', type: 'info' }, { default: () => r.roleName || r.roleCode })
            )
        }
      )
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', text: true, type: 'primary', onClick: () => openAssign(row) }, { default: () => '分配角色' })
        ]
      })
    }
  }
]

async function loadList() {
  loading.value = true
  try {
    const res = await getUserList(query)
    const data = res.data || {}
    tableData.value = data.list || []
    pagination.itemCount = data.total || 0
    pagination.page = data.page || query.page
    pagination.pageSize = data.size || query.size
  } catch (e) {
    // 错误已提示
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await getRoleList()
    const list = res.data || []
    roleOptions.value = list.map((r) => ({ label: `${r.roleName}(${r.roleCode})`, value: r.id }))
  } catch (e) {
    // ignore
  }
}

function handlePageChange(page) {
  query.page = page
  loadList()
}

function handleSizeChange(size) {
  query.size = size
  query.page = 1
  loadList()
}

// 新建用户
const createFormRef = ref(null)
const createModal = reactive({ show: false, loading: false })
const createForm = reactive({ username: '', nickname: '', roleIds: [] })
const createRules = {
  username: { required: true, message: '请输入用户名', trigger: ['blur', 'input'] },
  nickname: { required: true, message: '请输入昵称', trigger: ['blur', 'input'] },
  roleIds: { type: 'array', required: true, message: '请选择角色', trigger: ['change', 'blur'] }
}

function openCreate() {
  createForm.username = ''
  createForm.nickname = ''
  createForm.roleIds = []
  createModal.show = true
}

async function submitCreate() {
  try {
    await createFormRef.value?.validate()
  } catch (e) {
    return
  }
  createModal.loading = true
  try {
    const res = await createUser({ ...createForm })
    const data = res.data || {}
    createModal.show = false
    resultModal.password = data.password || ''
    resultModal.show = true
    loadList()
  } catch (e) {
    // 错误已提示
  } finally {
    createModal.loading = false
  }
}

// 创建结果
const resultModal = reactive({ show: false, password: '' })

async function copyPwd() {
  try {
    await navigator.clipboard.writeText(resultModal.password)
    message.success('密码已复制')
  } catch (e) {
    message.warning('复制失败，请手动复制')
  }
}

// 分配角色
const assignModal = reactive({ show: false, loading: false, userId: null, username: '', roleIds: [] })

function openAssign(row) {
  assignModal.userId = row.id
  assignModal.username = row.username
  assignModal.roleIds = (row.roles || []).map((r) => r.id)
  assignModal.show = true
}

async function submitAssign() {
  assignModal.loading = true
  try {
    await assignUserRoles(assignModal.userId, assignModal.roleIds)
    message.success('分配成功')
    assignModal.show = false
    loadList()
  } catch (e) {
    // 错误已提示
  } finally {
    assignModal.loading = false
  }
}

onMounted(async () => {
  await loadRoles()
  loadList()
})
</script>

<style scoped>
.page-title {
  margin: 0;
  font-size: 16px;
  color: #1f2a44;
}

.pwd-box {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.pwd-text {
  font-family: Consolas, monospace;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #d4380d;
  background: #fff7e6;
  padding: 6px 12px;
  border-radius: 4px;
  user-select: all;
}
</style>

<template>
  <div class="page-container">
    <div class="card-box">
      <div class="search-bar">
        <h3 class="page-title">角色管理</h3>
        <div style="flex: 1"></div>
        <n-button v-perm="'role:add'" type="primary" @click="openCreate">+ 新建角色</n-button>
        <n-button @click="loadList">刷新</n-button>
      </div>

      <n-data-table :columns="columns" :data="tableData" :loading="loading" :bordered="false" />
    </div>

    <!-- 角色编辑弹窗 -->
    <n-modal v-model:show="formModal.show" preset="card" :title="formModal.isEdit ? '编辑角色' : '新建角色'" style="width: 480px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-form-item label="角色名称" path="roleName">
          <n-input v-model:value="form.roleName" placeholder="如：编辑" />
        </n-form-item>
        <n-form-item label="角色编码" path="roleCode">
          <n-input v-model:value="form.roleCode" placeholder="如：editor" :disabled="formModal.isEdit" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="form.description" type="textarea" :rows="2" placeholder="角色描述" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="formModal.show = false">取消</n-button>
          <n-button type="primary" :loading="formModal.loading" @click="submitForm">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 权限分配弹窗 -->
    <n-modal v-model:show="permModal.show" preset="card" title="分配权限" style="width: 520px">
      <div class="perm-modal-body">
        <div class="perm-role-info">角色：{{ permModal.roleName }} ({{ permModal.roleCode }})</div>
        <n-spin :show="permModal.treeLoading">
          <perm-tree-select
            v-if="permTree.length"
            v-model="permModal.permissionIds"
            :data="permTree"
          />
          <n-empty v-else description="暂无权限数据" />
        </n-spin>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="permModal.show = false">取消</n-button>
          <n-button type="primary" :loading="permModal.saving" @click="submitPermAssign">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { h, onMounted, reactive, ref } from 'vue'
import { NButton, NSpace, NTag, useDialog, useMessage } from 'naive-ui'
import PermTreeSelect from '@/components/PermTreeSelect.vue'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  assignRolePermissions,
  getPermissionTree
} from '@/api/rbac'
import { hasPerm } from '@/utils/perm'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const tableData = ref([])

const columns = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '角色名称', key: 'roleName', width: 140 },
  { title: '角色编码', key: 'roleCode', width: 140 },
  { title: '描述', key: 'description', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 260,
    render(row) {
      const buttons = []
      if (hasPerm('role:update')) {
        buttons.push(h(NButton, { size: 'small', text: true, type: 'primary', onClick: () => openEdit(row) }, { default: () => '编辑' }))
      }
      if (hasPerm('role:assignPerm')) {
        buttons.push(h(NButton, { size: 'small', text: true, type: 'info', onClick: () => openPermAssign(row) }, { default: () => '分配权限' }))
      }
      if (hasPerm('role:delete')) {
        buttons.push(h(NButton, { size: 'small', text: true, type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }))
      }
      return h(NSpace, null, { default: () => buttons })
    }
  }
]

async function loadList() {
  loading.value = true
  try {
    const res = await getRoleList()
    tableData.value = res.data || []
  } catch (e) {
    // 错误已提示
  } finally {
    loading.value = false
  }
}

// 角色新增/编辑
const formRef = ref(null)
const formModal = reactive({ show: false, loading: false, isEdit: false })
const form = reactive({ id: undefined, roleName: '', roleCode: '', description: '' })
const rules = {
  roleName: { required: true, message: '请输入角色名称', trigger: ['blur', 'input'] },
  roleCode: { required: true, message: '请输入角色编码', trigger: ['blur', 'input'] }
}

function openCreate() {
  formModal.isEdit = false
  form.id = undefined
  form.roleName = ''
  form.roleCode = ''
  form.description = ''
  formModal.show = true
}

function openEdit(row) {
  formModal.isEdit = true
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.description = row.description || ''
  formModal.show = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  formModal.loading = true
  try {
    const payload = {
      roleName: form.roleName,
      roleCode: form.roleCode,
      description: form.description
    }
    if (formModal.isEdit) {
      payload.id = form.id
      await updateRole(payload)
    } else {
      await createRole(payload)
    }
    message.success(formModal.isEdit ? '更新成功' : '创建成功')
    formModal.show = false
    loadList()
  } catch (e) {
    // 错误已提示
  } finally {
    formModal.loading = false
  }
}

function handleDelete(row) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除角色「${row.roleName}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteRole(row.id)
        message.success('删除成功')
        loadList()
      } catch (e) {
        // 错误已提示
      }
    }
  })
}

// 权限分配
const permTree = ref([])
const permModal = reactive({
  show: false,
  saving: false,
  treeLoading: false,
  roleId: null,
  roleName: '',
  roleCode: '',
  permissionIds: []
})

async function loadPermTree() {
  if (permTree.value.length) return
  try {
    const res = await getPermissionTree()
    permTree.value = res.data || []
  } catch (e) {
    // ignore
  }
}

async function openPermAssign(row) {
  permModal.roleId = row.id
  permModal.roleName = row.roleName
  permModal.roleCode = row.roleCode
  permModal.permissionIds = []
  permModal.show = true
  permModal.treeLoading = true
  try {
    await loadPermTree()
    const res = await getRolePermissions(row.id)
    permModal.permissionIds = res.data || []
  } catch (e) {
    // 错误已提示
  } finally {
    permModal.treeLoading = false
  }
}

async function submitPermAssign() {
  permModal.saving = true
  try {
    await assignRolePermissions(permModal.roleId, permModal.permissionIds)
    message.success('权限分配成功')
    permModal.show = false
  } catch (e) {
    // 错误已提示
  } finally {
    permModal.saving = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.page-title {
  margin: 0;
  font-size: 16px;
  color: #1f2a44;
}

.perm-modal-body {
  max-height: 50vh;
  overflow: auto;
}

.perm-role-info {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #555;
}
</style>

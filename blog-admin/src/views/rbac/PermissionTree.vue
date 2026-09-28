<template>
  <div class="page-container">
    <div class="card-box">
      <div class="search-bar">
        <h3 class="page-title">权限管理</h3>
        <div style="flex: 1"></div>
        <n-button @click="loadTree">刷新</n-button>
      </div>

      <n-spin :show="loading">
        <n-empty v-if="!treeData.length && !loading" description="暂无权限数据" />
        <n-tree
          v-else
          :data="treeData"
          key-field="id"
          label-field="permName"
          children-field="children"
          block-line
          :default-expanded-keys="expandedKeys"
          :render-label="renderLabel"
          :render-prefix="renderPrefix"
        />
      </n-spin>
    </div>
  </div>
</template>

<script setup>
import { h, onMounted, ref } from 'vue'
import { NIcon, NTag } from 'naive-ui'
import { ShieldCheckmarkOutline, KeyOutline, CubeOutline } from '@vicons/ionicons5'
import { getPermissionTree } from '@/api/rbac'

const loading = ref(false)
const treeData = ref([])
const expandedKeys = ref([])

// 收集所有第一层节点 id，默认展开
function collectTopIds(nodes) {
  const ids = []
  ;(nodes || []).forEach((n) => {
    if (n.id != null) ids.push(n.id)
  })
  return ids
}

// 根据权限类型显示不同图标
function renderPrefix({ option }) {
  const type = option.permType
  let Icon = CubeOutline
  if (type === 'menu' || option.children?.length) Icon = ShieldCheckmarkOutline
  else if (type === 'button' || (option.permCode && option.permCode.includes(':'))) Icon = KeyOutline
  return h(NIcon, null, { default: () => h(Icon) })
}

function renderLabel({ option }) {
  const tags = []
  if (option.permCode) {
    tags.push(h(NTag, { size: 'tiny', type: 'info', style: 'margin-left:8px' }, { default: () => option.permCode }))
  }
  if (option.path) {
    tags.push(h(NTag, { size: 'tiny', type: 'default', style: 'margin-left:4px' }, { default: () => option.path }))
  }
  return h('span', { style: 'display:inline-flex;align-items:center;' }, [
    option.permName,
    ...tags
  ])
}

async function loadTree() {
  loading.value = true
  try {
    const res = await getPermissionTree()
    treeData.value = res.data || []
    expandedKeys.value = collectTopIds(treeData.value)
  } catch (e) {
    // 错误已提示
  } finally {
    loading.value = false
  }
}

onMounted(loadTree)
</script>

<style scoped>
.page-title {
  margin: 0;
  font-size: 16px;
  color: #1f2a44;
}
</style>

<template>
  <div class="page-container">
    <div class="card-box">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <n-input v-model:value="query.keyword" placeholder="搜索标题/关键词" clearable style="width: 220px" @keyup.enter="loadList" />
        <n-select
          v-model:value="query.status"
          :options="statusOptions"
          placeholder="文章状态"
          clearable
          style="width: 160px"
          @update:value="loadList"
        />
        <n-button type="primary" @click="loadList">查询</n-button>
        <n-button @click="resetQuery">重置</n-button>
        <div style="flex: 1"></div>
        <n-button v-perm="'article:create'" type="success" @click="goCreate">+ 新建文章</n-button>
      </div>

      <!-- 文章表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :bordered="false"
        remote
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NSpace, NTag, useDialog, useMessage } from 'naive-ui'
import { getArticleList, deleteArticle } from '@/api/article'
import { hasPerm } from '@/utils/perm'

const router = useRouter()
const dialog = useDialog()
const message = useMessage()

const statusOptions = [
  { label: '全部', value: null },
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 }
]

const query = reactive({
  page: 1,
  size: 10,
  status: null,
  keyword: ''
})

const loading = ref(false)
const tableData = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const columns = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(
        NTag,
        { type: row.status === 1 ? 'success' : 'default', size: 'small' },
        { default: () => (row.status === 1 ? '已发布' : '草稿') }
      )
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  { title: '更新时间', key: 'updateTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render(row) {
      const buttons = []
      if (hasPerm('article:update')) {
        buttons.push(
          h(NButton, { size: 'small', type: 'primary', text: true, onClick: () => goEdit(row.id) }, { default: () => '编辑' })
        )
      }
      if (hasPerm('article:delete')) {
        buttons.push(
          h(
            NButton,
            { size: 'small', type: 'error', text: true, onClick: () => handleDelete(row) },
            { default: () => '删除' }
          )
        )
      }
      return h(NSpace, null, { default: () => buttons })
    }
  }
]

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size
    }
    if (query.status !== null && query.status !== '') {
      params.status = query.status
    }
    if (query.keyword) {
      params.keyword = query.keyword
    }
    const res = await getArticleList(params)
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

function resetQuery() {
  query.keyword = ''
  query.status = null
  query.page = 1
  loadList()
}

function handlePageChange(page) {
  query.page = page
  loadList()
}

function handlePageSizeChange(size) {
  query.size = size
  query.page = 1
  loadList()
}

function goCreate() {
  router.push('/admin/article/edit')
}

function goEdit(id) {
  router.push(`/admin/article/edit/${id}`)
}

function handleDelete(row) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除文章「${row.title}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteArticle(row.id)
        message.success('删除成功')
        loadList()
      } catch (e) {
        // 错误已提示
      }
    }
  })
}

onMounted(loadList)
</script>

import request from '@/utils/request'

// 文章相关 API

// 获取文章列表（公开）
// params: { page, size, keyword }
export function getArticleListApi(params) {
  return request.get('/api/article/list', { params })
}

// 获取文章详情（公开）
// id: 文章ID
export function getArticleDetailApi(id) {
  return request.get(`/api/article/detail/${id}`)
}

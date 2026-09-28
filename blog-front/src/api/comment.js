import request from '@/utils/request'

// 评论相关 API

// 获取文章的评论列表（公开）
// articleId: 文章ID
// params: { page, size }
export function getCommentListApi(articleId, params) {
  return request.get(`/api/comment/list/${articleId}`, { params })
}

// 新增评论（需登录）
// body: { articleId, content, parentId, replyToUserId }
export function addCommentApi(body) {
  return request.post('/api/comment', body)
}

// 删除评论（需登录，仅本人）
// id: 评论ID
export function deleteCommentApi(id) {
  return request.delete(`/api/comment/${id}`)
}

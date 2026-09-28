import request from '@/utils/request'

// 管理端文章列表
export function getArticleList(params) {
  return request({
    url: '/article/admin/list',
    method: 'get',
    params
  })
}

// 管理端文章详情
export function getArticleDetail(id) {
  return request({
    url: `/article/admin/${id}`,
    method: 'get'
  })
}

// 创建文章
export function createArticle(data) {
  return request({
    url: '/article',
    method: 'post',
    data
  })
}

// 更新文章
export function updateArticle(data) {
  return request({
    url: '/article',
    method: 'put',
    data
  })
}

// 删除文章
export function deleteArticle(id) {
  return request({
    url: `/article/${id}`,
    method: 'delete'
  })
}

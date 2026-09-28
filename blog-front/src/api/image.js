import request from '@/utils/request'

// 相册（图片）相关 API

// 获取公开相册列表（公开）
// params: { page, size }
export function getImageListApi(params) {
  return request.get('/api/image/list', { params })
}

import request from '@/utils/request'

// 上传图片（multipart/form-data）
export function uploadImage(formData) {
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 素材列表
export function getUploadList(params) {
  return request({
    url: '/upload/list',
    method: 'get',
    params
  })
}

// 删除图片
export function deleteUpload(id) {
  return request({
    url: `/upload/${id}`,
    method: 'delete'
  })
}

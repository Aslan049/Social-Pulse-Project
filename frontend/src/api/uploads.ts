import { api } from './client'
import type { Platform, UploadResult } from './types'

export const uploadFile = (platform: Platform, file: File, onProgress?: (percent: number) => void) => {
  const form = new FormData()
  form.append('platform', platform)
  form.append('file', file)
  return api
    .post<UploadResult>('/api/uploads', form, {
      onUploadProgress: (event) => {
        if (onProgress && event.total) {
          onProgress(Math.round((event.loaded * 100) / event.total))
        }
      },
    })
    .then((r) => r.data)
}

export const listUploads = () => api.get<UploadResult[]>('/api/uploads').then((r) => r.data)

export const getUpload = (id: number) => api.get<UploadResult>(`/api/uploads/${id}`).then((r) => r.data)

export const deleteUpload = (id: number) => api.delete(`/api/uploads/${id}`).then(() => undefined)

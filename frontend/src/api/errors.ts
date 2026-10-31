import axios from 'axios'

interface ProblemDetail {
  detail?: string
  errors?: Record<string, string>
}

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ProblemDetail>(error)) {
    if (!error.response) {
      return 'Sunucuya ulaşılamadı. Backend çalışıyor mu?'
    }
    return error.response.data?.detail ?? 'Beklenmeyen bir hata oluştu'
  }
  return 'Beklenmeyen bir hata oluştu'
}

export function getFieldErrors(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ProblemDetail>(error)) {
    return error.response?.data?.errors ?? {}
  }
  return {}
}
